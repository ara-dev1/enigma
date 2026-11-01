package com.enigma.engine

import com.enigma.engine.configuration.EnigmaConfiguration
import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.Rotor
import com.enigma.engine.model.SignalPathTrace
import com.enigma.engine.model.SignalStep
import com.enigma.engine.preset.EnigmaPreset

/**
 * A fully wired Enigma machine (M3 or M4).
 *
 * Rotors are stored in the operator's order: [leftRotor] is the slowest wheel next to the
 * reflector, [rightRotor] is the fast wheel next to the keyboard. For the M4 the thin Greek
 * rotor sits to the left of [leftRotor] and never steps during encryption.
 *
 * The machine is stateful: every call to [encrypt] first turns the rotors and then routes the
 * current through plugboard, rotors, reflector and back. Encrypting a message twice with two
 * identically configured machines returns the original text (the cipher is reciprocal).
 *
 * Instances are *not* thread safe; create one machine per conversation/key.
 */
class EnigmaMachine(
    val model: EnigmaModel,
    var reflector: Reflector,
    var greekRotor: Rotor? = null, // Only for the M4
    var leftRotor: Rotor,
    var middleRotor: Rotor,
    var rightRotor: Rotor,
    var plugboard: Plugboard = Plugboard()
) {
    init {
        validateTopology()
    }

    /**
     * Checks the model/reflector/rotor combination.
     *
     * The M4 needs a thin reflector plus a Beta or Gamma wheel in the fourth slot; the M3 has
     * exactly three rotors and a standard reflector. Illegal combinations are rejected right
     * away instead of silently degrading to a three rotor machine.
     */
    fun validateTopology() {
        if (model.requiresThinReflector) {
            val greek = requireNotNull(greekRotor) { "Model ${model.name} requires a Greek rotor (Beta or Gamma)" }
            require(greek.spec.isThin) { "The fourth rotor must be a thin rotor (Beta or Gamma), got '${greek.spec.id}'" }
            require(reflector.spec.isThin) { "Model ${model.name} requires a thin reflector (B Thin or C Thin)" }
        } else {
            require(greekRotor == null) {
                "Model ${model.name} has only three rotors, but a fourth (Greek) rotor was supplied"
            }
            require(!reflector.spec.isThin) { "Model ${model.name} requires a standard reflector (A, B or C)" }
        }
        val wheels = listOfNotNull(leftRotor, middleRotor, rightRotor, greekRotor)
        require(wheels.none { it.spec.isThin && it !== greekRotor }) {
            "Thin rotors (Beta/Gamma) can only be used in the fourth position"
        }
    }

    /**
     * Executes rotor stepping logic according to Enigma mechanical pawl principles.
     *
     * 1. Right rotor always steps on key press.
     * 2. Middle rotor steps if the right rotor is at its notch OR if the middle rotor itself is
     *    at its notch (the famous double-stepping anomaly).
     * 3. Left rotor steps if the middle rotor is at its notch.
     * 4. The fourth (Greek) rotor of the M4 never steps during encryption.
     */
    fun step() {
        val rightAtNotch = rightRotor.isAtNotch()
        val middleAtNotch = middleRotor.isAtNotch()

        // Right rotor ALWAYS steps
        rightRotor.step()

        // Double-stepping logic:
        if (middleAtNotch) {
            middleRotor.step()
            leftRotor.step()
        } else if (rightAtNotch) {
            middleRotor.step()
        }
    }

    /**
     * Encrypts or decrypts a single character and returns the transformed character while
     * stepping the rotors. Characters outside A-Z are returned unchanged.
     */
    fun encrypt(char: Char): Char {
        val upper = char.uppercaseChar()
        if (!Alphabet.isLetter(upper)) return char

        // 1. Step rotors prior to character transformation
        step()

        // 2. Plugboard forward
        var currIndex = Alphabet.toIndex(plugboard.process(upper))

        // 3. Right -> Middle -> Left -> (Greek if M4)
        currIndex = rightRotor.forward(currIndex)
        currIndex = middleRotor.forward(currIndex)
        currIndex = leftRotor.forward(currIndex)
        val greek = if (model.hasFourthRotor) requireGreekRotor() else null
        if (greek != null) {
            currIndex = greek.forward(currIndex)
        }

        // 4. Reflector
        currIndex = reflector.reflect(currIndex)

        // 5. (Greek if M4) -> Left -> Middle -> Right
        if (greek != null) {
            currIndex = greek.backward(currIndex)
        }
        currIndex = leftRotor.backward(currIndex)
        currIndex = middleRotor.backward(currIndex)
        currIndex = rightRotor.backward(currIndex)

        // 6. Plugboard backward
        return plugboard.process(Alphabet.toChar(currIndex))
    }

    /**
     * Encrypts or decrypts a single character and produces a step-by-step trace of the
     * electrical signal path. Non A-Z characters are rejected: use [encrypt] for those.
     */
    fun processWithTrace(char: Char): SignalPathTrace {
        val upper = char.uppercaseChar()
        require(Alphabet.isLetter(upper)) { "Can only trace valid A-Z characters, got '$char'" }

        val greek = if (model.hasFourthRotor) requireGreekRotor() else null
        val steps = mutableListOf<SignalStep>()

        // 1. Step rotors
        step()
        steps.add(
            SignalStep(
                stage = "Rotor Stepping",
                character = upper,
                index = Alphabet.toIndex(upper),
                description = "Rotors stepped. Current positions: " +
                        (if (greek != null) "${greek.position} " else "") +
                        "${leftRotor.position} ${middleRotor.position} ${rightRotor.position}"
            )
        )

        // 2. Keyboard to Plugboard
        val plug1Char = plugboard.process(upper)
        val plug1Index = Alphabet.toIndex(plug1Char)
        steps.add(
            SignalStep(
                stage = "Plugboard (In)",
                character = plug1Char,
                index = plug1Index,
                description = if (plug1Char != upper) "Swapped '$upper' -> '$plug1Char'" else "No swap ('$upper')"
            )
        )

        // 3. Right Rotor Forward
        val rFwdIndex = rightRotor.forward(plug1Index)
        val rFwdChar = Alphabet.toChar(rFwdIndex)
        steps.add(
            SignalStep(
                stage = "Right Rotor (Fwd)",
                character = rFwdChar,
                index = rFwdIndex,
                description = "Rotor ${rightRotor.spec.id} (Pos ${rightRotor.position}, Ring ${rightRotor.ringSetting}) -> '$rFwdChar'"
            )
        )

        // 4. Middle Rotor Forward
        val mFwdIndex = middleRotor.forward(rFwdIndex)
        val mFwdChar = Alphabet.toChar(mFwdIndex)
        steps.add(
            SignalStep(
                stage = "Middle Rotor (Fwd)",
                character = mFwdChar,
                index = mFwdIndex,
                description = "Rotor ${middleRotor.spec.id} (Pos ${middleRotor.position}, Ring ${middleRotor.ringSetting}) -> '$mFwdChar'"
            )
        )

        // 5. Left Rotor Forward
        val lFwdIndex = leftRotor.forward(mFwdIndex)
        val lFwdChar = Alphabet.toChar(lFwdIndex)
        steps.add(
            SignalStep(
                stage = "Left Rotor (Fwd)",
                character = lFwdChar,
                index = lFwdIndex,
                description = "Rotor ${leftRotor.spec.id} (Pos ${leftRotor.position}, Ring ${leftRotor.ringSetting}) -> '$lFwdChar'"
            )
        )

        var lastIndex = lFwdIndex

        // 6. Greek Rotor Forward (M4)
        if (greek != null) {
            lastIndex = greek.forward(lastIndex)
            val gFwdChar = Alphabet.toChar(lastIndex)
            steps.add(
                SignalStep(
                    stage = "Greek Rotor (Fwd)",
                    character = gFwdChar,
                    index = lastIndex,
                    description = "Rotor ${greek.spec.id} (Pos ${greek.position}, Ring ${greek.ringSetting}) -> '$gFwdChar'"
                )
            )
        }

        // 7. Reflector
        val refIndex = reflector.reflect(lastIndex)
        val refChar = Alphabet.toChar(refIndex)
        steps.add(
            SignalStep(
                stage = "Reflector",
                character = refChar,
                index = refIndex,
                description = "Reflector ${reflector.spec.id} reflected to '$refChar'"
            )
        )
        lastIndex = refIndex

        // 8. Greek Rotor Backward (M4)
        if (greek != null) {
            lastIndex = greek.backward(lastIndex)
            val gBwdChar = Alphabet.toChar(lastIndex)
            steps.add(
                SignalStep(
                    stage = "Greek Rotor (Bwd)",
                    character = gBwdChar,
                    index = lastIndex,
                    description = "Rotor ${greek.spec.id} -> '$gBwdChar'"
                )
            )
        }

        // 9. Left Rotor Backward
        val lBwdIndex = leftRotor.backward(lastIndex)
        val lBwdChar = Alphabet.toChar(lBwdIndex)
        steps.add(
            SignalStep(
                stage = "Left Rotor (Bwd)",
                character = lBwdChar,
                index = lBwdIndex,
                description = "Rotor ${leftRotor.spec.id} -> '$lBwdChar'"
            )
        )

        // 10. Middle Rotor Backward
        val mBwdIndex = middleRotor.backward(lBwdIndex)
        val mBwdChar = Alphabet.toChar(mBwdIndex)
        steps.add(
            SignalStep(
                stage = "Middle Rotor (Bwd)",
                character = mBwdChar,
                index = mBwdIndex,
                description = "Rotor ${middleRotor.spec.id} -> '$mBwdChar'"
            )
        )

        // 11. Right Rotor Backward
        val rBwdIndex = rightRotor.backward(mBwdIndex)
        val rBwdChar = Alphabet.toChar(rBwdIndex)
        steps.add(
            SignalStep(
                stage = "Right Rotor (Bwd)",
                character = rBwdChar,
                index = rBwdIndex,
                description = "Rotor ${rightRotor.spec.id} -> '$rBwdChar'"
            )
        )

        // 12. Plugboard Backward
        val finalChar = plugboard.process(rBwdChar)
        val finalIndex = Alphabet.toIndex(finalChar)
        steps.add(
            SignalStep(
                stage = "Plugboard (Out)",
                character = finalChar,
                index = finalIndex,
                description = if (finalChar != rBwdChar) "Swapped '$rBwdChar' -> '$finalChar'" else "No swap ('$finalChar')"
            )
        )

        return SignalPathTrace(
            inputChar = upper,
            outputChar = finalChar,
            steps = steps
        )
    }

    /**
     * Processes a string of text. Non alphabetic characters are passed through unchanged;
     * use [Alphabet.normalize] first if they should be dropped.
     */
    fun processText(text: String): String {
        val result = StringBuilder(text.length)
        for (char in text) {
            if (Alphabet.isLetter(char)) {
                result.append(encrypt(char))
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    /** Current window letters, e.g. `VJWY` for an M4 or `BRS` for an M3. */
    fun display(): String = buildString {
        greekRotor?.let { append(it.position) }
        append(leftRotor.position)
        append(middleRotor.position)
        append(rightRotor.position)
    }

    /** Snapshot of the complete configuration (rotor order, positions, rings, plugboard). */
    fun configuration(): EnigmaConfiguration = EnigmaConfiguration.of(this)

    /**
     * Re-applies a preset to this machine. The preset has to match the machine model,
     * otherwise use [fromPreset] or [fromConfiguration] to build a machine of the right type.
     */
    fun applyPreset(preset: EnigmaPreset) {
        require(preset.model == this.model) {
            "Preset '${preset.id}' configures an ${preset.model.name} but this machine is an " +
                    "${model.name}. Build a new machine with EnigmaMachine.fromPreset()."
        }
        reflector = Reflector(preset.reflectorSpec)
        greekRotor = preset.greekRotorSpec?.let {
            Rotor(it, preset.greekRotorPosition, preset.greekRotorRing)
        }
        leftRotor = Rotor(preset.rotorSpecs[0], preset.positions[0], preset.ringSettings[0])
        middleRotor = Rotor(preset.rotorSpecs[1], preset.positions[1], preset.ringSettings[1])
        rightRotor = Rotor(preset.rotorSpecs[2], preset.positions[2], preset.ringSettings[2])
        plugboard = Plugboard(preset.plugboardPairs)
        validateTopology()
    }

    /** Restores the positions (and Greek rotor position) recorded in a configuration. */
    fun restorePositions(configuration: EnigmaConfiguration) {
        require(configuration.model == model) {
            "Configuration belongs to an ${configuration.model.name}, this machine is an ${model.name}"
        }
        require(configuration.rotors.map { it.specId } ==
                listOf(leftRotor.spec.id, middleRotor.spec.id, rightRotor.spec.id)) {
            "Configuration rotor order does not match this machine"
        }
        leftRotor.position = configuration.rotors[0].normalizedPosition
        middleRotor.position = configuration.rotors[1].normalizedPosition
        rightRotor.position = configuration.rotors[2].normalizedPosition
        greekRotor?.let { greek ->
            configuration.greekRotor?.let { greek.position = it.normalizedPosition }
        }
    }

    private fun requireGreekRotor(): Rotor =
        requireNotNull(greekRotor) { "Model ${model.name} requires a Greek rotor" }

    fun copy(): EnigmaMachine {
        return EnigmaMachine(
            model = this.model,
            reflector = Reflector(this.reflector.spec),
            greekRotor = this.greekRotor?.copy(),
            leftRotor = this.leftRotor.copy(),
            middleRotor = this.middleRotor.copy(),
            rightRotor = this.rightRotor.copy(),
            plugboard = Plugboard(this.plugboard.pairsList)
        )
    }

    override fun toString(): String =
        "${model.name}[${display()}] reflector=${reflector.spec.id} plugboard=${plugboard.pairsList.joinToString(",")}"

    companion object {
        fun createDefaultM3(): EnigmaMachine = EnigmaMachine(
            model = EnigmaModel.M3,
            reflector = Reflector(ReflectorCatalog.REFLECTOR_B),
            leftRotor = Rotor(RotorCatalog.ROTOR_I, 'A', 1),
            middleRotor = Rotor(RotorCatalog.ROTOR_II, 'A', 1),
            rightRotor = Rotor(RotorCatalog.ROTOR_III, 'A', 1),
            plugboard = Plugboard()
        )

        fun createDefaultM4(): EnigmaMachine = EnigmaMachine(
            model = EnigmaModel.M4,
            reflector = Reflector(ReflectorCatalog.REFLECTOR_B_THIN),
            greekRotor = Rotor(RotorCatalog.ROTOR_BETA, 'A', 1),
            leftRotor = Rotor(RotorCatalog.ROTOR_I, 'A', 1),
            middleRotor = Rotor(RotorCatalog.ROTOR_II, 'A', 1),
            rightRotor = Rotor(RotorCatalog.ROTOR_III, 'A', 1),
            plugboard = Plugboard()
        )

        fun fromPreset(preset: EnigmaPreset): EnigmaMachine = EnigmaMachine(
            model = preset.model,
            reflector = Reflector(preset.reflectorSpec),
            greekRotor = preset.greekRotorSpec?.let {
                Rotor(it, preset.greekRotorPosition, preset.greekRotorRing)
            },
            leftRotor = Rotor(preset.rotorSpecs[0], preset.positions[0], preset.ringSettings[0]),
            middleRotor = Rotor(preset.rotorSpecs[1], preset.positions[1], preset.ringSettings[1]),
            rightRotor = Rotor(preset.rotorSpecs[2], preset.positions[2], preset.ringSettings[2]),
            plugboard = Plugboard(preset.plugboardPairs)
        )

        /** Rebuilds a machine from a serialisable [EnigmaConfiguration]. */
        fun fromConfiguration(configuration: EnigmaConfiguration): EnigmaMachine = EnigmaMachine(
            model = configuration.model,
            reflector = Reflector(ReflectorCatalog.getById(configuration.reflectorId)),
            greekRotor = configuration.greekRotor?.let {
                Rotor(RotorCatalog.getById(it.specId), it.normalizedPosition, it.ringSetting)
            },
            leftRotor = Rotor(
                RotorCatalog.getById(configuration.rotors[0].specId),
                configuration.rotors[0].normalizedPosition,
                configuration.rotors[0].ringSetting
            ),
            middleRotor = Rotor(
                RotorCatalog.getById(configuration.rotors[1].specId),
                configuration.rotors[1].normalizedPosition,
                configuration.rotors[1].ringSetting
            ),
            rightRotor = Rotor(
                RotorCatalog.getById(configuration.rotors[2].specId),
                configuration.rotors[2].normalizedPosition,
                configuration.rotors[2].ringSetting
            ),
            plugboard = Plugboard(configuration.plugboard)
        )

        /** Builds a machine from a plugboard notation such as `AV BS CG DL`. */
        fun parsePlugboard(notation: String): Plugboard {
            val tokens = notation.split(" ", ",").filter { it.isNotBlank() }
            return Plugboard(tokens.map { token ->
                require(token.length == 2) { "Malformed plugboard pair '$token'" }
                PlugboardPair(token[0], token[1])
            })
        }
    }
}
