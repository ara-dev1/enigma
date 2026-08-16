package com.enigma.engine

import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.Rotor
import com.enigma.engine.model.SignalPathTrace
import com.enigma.engine.model.SignalStep
import com.enigma.engine.preset.EnigmaPreset

class EnigmaMachine(
    val model: EnigmaModel,
    var reflector: Reflector,
    var greekRotor: Rotor? = null, // Only for M4
    var leftRotor: Rotor,          // Rotor 1 (Fastest/Rightmost in physical or Rotor 3) -> standard convention:
                                   // Let's explicitly store as [Left (slow), Middle (medium), Right (fast)]
    var middleRotor: Rotor,
    var rightRotor: Rotor,
    var plugboard: Plugboard = Plugboard()
) {
    init {
        if (model == EnigmaModel.M4) {
            requireNotNull(greekRotor) { "M4 model requires a Greek rotor (Beta/Gamma)" }
            require(greekRotor!!.spec.isThin) { "Greek rotor must be a thin rotor (Beta or Gamma)" }
            require(reflector.spec.isThin) { "M4 model requires a thin reflector (Reflector B Thin or C Thin)" }
        } else {
            require(!reflector.spec.isThin) { "M3 model requires a standard reflector" }
        }
    }

    /**
     * Executes rotor stepping logic according to Enigma mechanical pawl principles.
     *
     * In standard 3-rotor Enigma (and 4-rotor M4):
     * 1. Right rotor always steps on keypress.
     * 2. Middle rotor steps if right rotor is at its notch OR if middle rotor itself is at its notch (Double-stepping anomaly!).
     * 3. Left rotor steps if middle rotor is at its notch.
     * 4. The 4th rotor (Greek rotor in M4) does NOT step during keypresses (it remains stationary unless manually rotated).
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
     * Encrypts or decrypts a single character and returns the transformed character while stepping the rotors.
     */
    fun encrypt(char: Char): Char {
        val upper = char.uppercaseChar()
        if (upper !in 'A'..'Z') return char

        // 1. Step rotors prior to character transformation
        step()

        // 2. Plugboard forward
        var currentChar = plugboard.process(upper)
        var currIndex = Alphabet.toIndex(currentChar)

        // 3. Right -> Middle -> Left -> (Greek if M4)
        currIndex = rightRotor.forward(currIndex)
        currIndex = middleRotor.forward(currIndex)
        currIndex = leftRotor.forward(currIndex)

        if (model == EnigmaModel.M4 && greekRotor != null) {
            currIndex = greekRotor!!.forward(currIndex)
        }

        // 4. Reflector
        currIndex = reflector.reflect(currIndex)

        // 5. (Greek if M4) -> Left -> Middle -> Right
        if (model == EnigmaModel.M4 && greekRotor != null) {
            currIndex = greekRotor!!.backward(currIndex)
        }

        currIndex = leftRotor.backward(currIndex)
        currIndex = middleRotor.backward(currIndex)
        currIndex = rightRotor.backward(currIndex)

        // 6. Plugboard backward
        val prePlugboardChar = Alphabet.toChar(currIndex)
        return plugboard.process(prePlugboardChar)
    }

    /**
     * Encrypts or decrypts a single character and produces a step-by-step trace of the electrical signal path.
     */
    fun processWithTrace(char: Char): SignalPathTrace {
        val upper = char.uppercaseChar()
        require(upper in 'A'..'Z') { "Can only trace valid A-Z characters" }

        val steps = mutableListOf<SignalStep>()

        // 1. Step rotors
        step()
        steps.add(
            SignalStep(
                stage = "Rotor Stepping",
                character = upper,
                index = Alphabet.toIndex(upper),
                description = "Rotors stepped. Current positions: " +
                        (if (model == EnigmaModel.M4) "${greekRotor?.position} " else "") +
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
        if (model == EnigmaModel.M4 && greekRotor != null) {
            val gr = greekRotor!!
            lastIndex = gr.forward(lastIndex)
            val gFwdChar = Alphabet.toChar(lastIndex)
            steps.add(
                SignalStep(
                    stage = "Greek Rotor (Fwd)",
                    character = gFwdChar,
                    index = lastIndex,
                    description = "Rotor ${gr.spec.id} (Pos ${gr.position}, Ring ${gr.ringSetting}) -> '$gFwdChar'"
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
        if (model == EnigmaModel.M4 && greekRotor != null) {
            val gr = greekRotor!!
            lastIndex = gr.backward(lastIndex)
            val gBwdChar = Alphabet.toChar(lastIndex)
            steps.add(
                SignalStep(
                    stage = "Greek Rotor (Bwd)",
                    character = gBwdChar,
                    index = lastIndex,
                    description = "Rotor ${gr.spec.id} -> '$gBwdChar'"
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
     * Processes a string of text. Non-alphabetic characters are passed through unchanged.
     */
    fun processText(text: String): String {
        val result = StringBuilder()
        for (char in text) {
            if (char.uppercaseChar() in 'A'..'Z') {
                result.append(encrypt(char))
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    fun applyPreset(preset: EnigmaPreset) {
        require(preset.model == this.model || (this.model == EnigmaModel.M4 && preset.model == EnigmaModel.M3)) {
            "Cannot apply preset model ${preset.model} directly to engine model $model without reconfiguring model type"
        }
        reflector = Reflector(preset.reflectorSpec)
        if (preset.greekRotorSpec != null) {
            greekRotor = Rotor(preset.greekRotorSpec, preset.greekRotorPosition, preset.greekRotorRing)
        } else {
            greekRotor = null
        }
        leftRotor = Rotor(preset.rotorSpecs[0], preset.positions[0], preset.ringSettings[0])
        middleRotor = Rotor(preset.rotorSpecs[1], preset.positions[1], preset.ringSettings[1])
        rightRotor = Rotor(preset.rotorSpecs[2], preset.positions[2], preset.ringSettings[2])
        plugboard = Plugboard(preset.plugboardPairs)
    }

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

    companion object {
        fun createDefaultM3(): EnigmaMachine {
            return EnigmaMachine(
                model = EnigmaModel.M3,
                reflector = Reflector(com.enigma.engine.catalog.ReflectorCatalog.REFLECTOR_B),
                leftRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_I, 'A', 1),
                middleRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_II, 'A', 1),
                rightRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_III, 'A', 1),
                plugboard = Plugboard()
            )
        }

        fun createDefaultM4(): EnigmaMachine {
            return EnigmaMachine(
                model = EnigmaModel.M4,
                reflector = Reflector(com.enigma.engine.catalog.ReflectorCatalog.REFLECTOR_B_THIN),
                greekRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_BETA, 'A', 1),
                leftRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_I, 'A', 1),
                middleRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_II, 'A', 1),
                rightRotor = Rotor(com.enigma.engine.catalog.RotorCatalog.ROTOR_III, 'A', 1),
                plugboard = Plugboard()
            )
        }

        fun fromPreset(preset: EnigmaPreset): EnigmaMachine {
            val greek = if (preset.model == EnigmaModel.M4 && preset.greekRotorSpec != null) {
                Rotor(preset.greekRotorSpec, preset.greekRotorPosition, preset.greekRotorRing)
            } else null

            return EnigmaMachine(
                model = preset.model,
                reflector = Reflector(preset.reflectorSpec),
                greekRotor = greek,
                leftRotor = Rotor(preset.rotorSpecs[0], preset.positions[0], preset.ringSettings[0]),
                middleRotor = Rotor(preset.rotorSpecs[1], preset.positions[1], preset.ringSettings[1]),
                rightRotor = Rotor(preset.rotorSpecs[2], preset.positions[2], preset.ringSettings[2]),
                plugboard = Plugboard(preset.plugboardPairs)
            )
        }
    }
}
