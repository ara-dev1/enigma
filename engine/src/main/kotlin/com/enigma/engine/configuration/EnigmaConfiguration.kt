package com.enigma.engine.configuration

import com.enigma.engine.EnigmaMachine
import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair

/** Complete state of a single rotor: which wheel is fitted, its window letter and Ringstellung. */
data class RotorConfiguration(
    val specId: String,
    val position: Char = 'A',
    val ringSetting: Int = 1
) {
    init {
        require(position.uppercaseChar() in 'A'..'Z') {
            "Rotor position must be a letter A-Z, got '$position'"
        }
        require(ringSetting in 1..26) {
            "Ring setting must be between 1 and 26, got $ringSetting"
        }
        RotorCatalog.getById(specId)
    }

    val normalizedPosition: Char get() = position.uppercaseChar()

    internal fun encode(): String = "$specId:$normalizedPosition:$ringSetting"

    companion object {
        internal fun decode(value: String): RotorConfiguration {
            val parts = value.split(':')
            require(parts.size == 3) { "Malformed rotor configuration '$value'" }
            val position = parts[1].singleOrNull()
                ?: throw IllegalArgumentException("Malformed rotor position in '$value'")
            val ring = parts[2].toIntOrNull()
                ?: throw IllegalArgumentException("Malformed ring setting in '$value'")
            return RotorConfiguration(parts[0], position, ring)
        }
    }
}

/**
 * Immutable, serialisable description of a completely configured Enigma machine.
 *
 * Because it carries rotor order, window letters, ring settings, the reflector, the optional
 * Greek rotor and the plugboard, it can be used to save, restore or share a key setting:
 *
 * ```kotlin
 * val snapshot: String = machine.configuration().encode()
 * val restored = EnigmaMachine.fromConfiguration(EnigmaConfiguration.decode(snapshot))
 * ```
 *
 * The textual form is stable within a [FORMAT_VERSION]; unknown or malformed input fails with
 * an [IllegalArgumentException] instead of silently producing a different machine.
 */
data class EnigmaConfiguration(
    val model: EnigmaModel,
    val reflectorId: String,
    val rotors: List<RotorConfiguration>,
    val greekRotor: RotorConfiguration? = null,
    val plugboard: List<PlugboardPair> = emptyList()
) {
    init {
        require(rotors.size == REQUIRED_ROTOR_COUNT) {
            "An Enigma machine needs exactly $REQUIRED_ROTOR_COUNT main rotors, got ${rotors.size}"
        }
        val reflector = ReflectorCatalog.getById(reflectorId)
        if (model.requiresThinReflector) {
            require(reflector.isThin) { "Model ${model.name} requires a thin reflector, got '${reflector.id}'" }
            val greek = requireNotNull(greekRotor) { "Model ${model.name} requires a Greek (thin) rotor" }
            require(RotorCatalog.getById(greek.specId).isThin) {
                "The fourth rotor of model ${model.name} must be Beta or Gamma, got '${greek.specId}'"
            }
        } else {
            require(!reflector.isThin) { "Model ${model.name} requires a standard reflector, got '${reflector.id}'" }
            require(greekRotor == null) { "Model ${model.name} has only three rotors; got a fourth one" }
        }
        require(plugboard.size <= Plugboard.MAX_PAIRS) {
            "At most ${Plugboard.MAX_PAIRS} plugboard pairs are allowed, got ${plugboard.size}"
        }
        val used = plugboard.flatMap { listOf(it.first, it.second) }
        require(used.size == used.toSet().size) { "A letter may appear in one plugboard pair only: $used" }
    }

    fun encode(): String {
        val rotorsPart = rotors.joinToString(";") { it.encode() }
        val greekPart = greekRotor?.encode() ?: NO_ROTOR
        val plugPart = if (plugboard.isEmpty()) NO_PLUG else plugboard.joinToString(",") { it.notation }
        return listOf(FORMAT_VERSION, model.name, reflectorId, greekPart, rotorsPart, plugPart)
            .joinToString(FIELD_SEPARATOR)
    }

    /** Builds a live, mutable machine from this configuration. */
    fun toMachine(): EnigmaMachine = EnigmaMachine.fromConfiguration(this)

    companion object {
        const val FORMAT_VERSION = "v1"
        const val REQUIRED_ROTOR_COUNT = 3

        private const val FIELD_SEPARATOR = "|"
        private const val ROTOR_SEPARATOR = ";"
        private const val PLUG_SEPARATOR = ","
        private const val NO_ROTOR = "-"
        private const val NO_PLUG = "-"

        /** Captures the current state (positions included) of an existing machine. */
        fun of(machine: EnigmaMachine): EnigmaConfiguration = EnigmaConfiguration(
            model = machine.model,
            reflectorId = machine.reflector.spec.id,
            rotors = listOf(machine.leftRotor, machine.middleRotor, machine.rightRotor)
                .map { RotorConfiguration(it.spec.id, it.position, it.ringSetting) },
            greekRotor = machine.greekRotor?.let {
                RotorConfiguration(it.spec.id, it.position, it.ringSetting)
            },
            plugboard = machine.plugboard.pairsList
        )

        fun decode(value: String): EnigmaConfiguration {
            val fields = value.split(FIELD_SEPARATOR)
            require(fields.size == 6) { "Malformed Enigma configuration: '$value'" }
            require(fields[0] == FORMAT_VERSION) {
                "Unsupported configuration version '${fields[0]}', expected '$FORMAT_VERSION'"
            }
            val model = EnigmaModel.fromId(fields[1])
            val reflectorId = fields[2]
            val greek = if (fields[3] == NO_ROTOR) null else RotorConfiguration.decode(fields[3])
            val rotors = fields[4].split(ROTOR_SEPARATOR)
                .filter { it.isNotEmpty() }
                .map { RotorConfiguration.decode(it) }
            val plugboard = if (fields[5] == NO_PLUG || fields[5].isEmpty()) {
                emptyList()
            } else {
                fields[5].split(PLUG_SEPARATOR).map { token ->
                    require(token.length == 2) { "Malformed plugboard pair '$token' in '$value'" }
                    PlugboardPair(token[0], token[1])
                }
            }
            return EnigmaConfiguration(
                model = model,
                reflectorId = reflectorId,
                rotors = rotors,
                greekRotor = greek,
                plugboard = plugboard
            )
        }
    }
}

/** Convenience accessor: `machine.configuration()`. */
fun EnigmaMachine.configuration(): EnigmaConfiguration = EnigmaConfiguration.of(this)
