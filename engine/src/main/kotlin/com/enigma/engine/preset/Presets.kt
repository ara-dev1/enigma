package com.enigma.engine.preset

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec

/**
 * A complete, ready to use key setting.
 *
 * [rotorSpecs], [positions] and [ringSettings] are always ordered from left (slowest) to
 * right (fastest). [plugboardPairs] must not contain a letter twice.
 */
data class EnigmaPreset(
    val id: String,
    val name: String,
    val description: String,
    val model: EnigmaModel,
    val reflectorSpec: ReflectorSpec,
    val rotorSpecs: List<RotorSpec>, // [Left, Middle, Right]
    val positions: List<Char>,        // [Left, Middle, Right]
    val ringSettings: List<Int>,      // [Left, Middle, Right], 1..26
    val plugboardPairs: List<PlugboardPair> = emptyList(),
    val greekRotorSpec: RotorSpec? = null,
    val greekRotorPosition: Char = 'A',
    val greekRotorRing: Int = 1
) {
    init {
        require(rotorSpecs.size == 3) { "Preset '$id' needs three main rotors, got ${rotorSpecs.size}" }
        require(positions.size == 3) { "Preset '$id' needs three rotor positions, got ${positions.size}" }
        require(ringSettings.size == 3) { "Preset '$id' needs three ring settings, got ${ringSettings.size}" }
        require(ringSettings.all { it in 1..26 }) { "Preset '$id' ring settings must be 1..26" }
        require(positions.all { it.uppercaseChar() in 'A'..'Z' }) { "Preset '$id' positions must be A-Z" }
        require(plugboardPairs.size <= Plugboard.MAX_PAIRS) {
            "Preset '$id' uses ${plugboardPairs.size} plugboard pairs, the maximum is ${Plugboard.MAX_PAIRS}"
        }
        if (model.requiresThinReflector) {
            require(reflectorSpec.isThin) { "Preset '$id': model ${model.name} needs a thin reflector" }
            requireNotNull(greekRotorSpec) { "Preset '$id': model ${model.name} needs a Greek rotor" }
            require(greekRotorSpec.isThin) { "Preset '$id': the Greek rotor must be thin (Beta/Gamma)" }
        } else {
            require(!reflectorSpec.isThin) { "Preset '$id': model ${model.name} needs a standard reflector" }
            require(greekRotorSpec == null) { "Preset '$id': model ${model.name} has only three rotors" }
        }
    }

    /** Rotor wheel order in the historical notation, left to right. */
    val rotorOrder: String get() = rotorSpecs.joinToString(" ") { it.id }

    /** Window letters in the historical notation, Greek rotor first for the M4. */
    val positionNotation: String
        get() = buildString {
            if (greekRotorSpec != null) append(greekRotorPosition.uppercaseChar())
            positions.forEach { append(it.uppercaseChar()) }
        }

    /** Ring settings written as letters (A = 1), Greek rotor first for the M4. */
    val ringNotation: String
        get() = buildString {
            if (greekRotorSpec != null) append(('A' + (greekRotorRing - 1)))
            ringSettings.forEach { append('A' + (it - 1)) }
        }

    /** Plugboard pairs in the historical notation, e.g. `AV BS CG`. */
    val plugboardNotation: String get() = plugboardPairs.joinToString(" ") { it.notation }
}

/**
 * Historical key settings.
 *
 * The presets are starting points for exploration; the `kriegsmarine_m4` and `scharnhorst_1943`
 * entries reproduce published, fully broken messages, so they double as known-answer vectors
 * (see `KnownAnswerVectorsTest`).
 */
object PresetCatalog {

    val WEHRMACHT_I = EnigmaPreset(
        id = "wehrmacht_i",
        name = "Wehrmacht Standard (1939)",
        description = "Classic Army/Air Force Enigma I key: rotors I-II-III, Reflector B, ten cables.",
        model = EnigmaModel.M3,
        reflectorSpec = ReflectorCatalog.REFLECTOR_B,
        rotorSpecs = listOf(RotorCatalog.ROTOR_I, RotorCatalog.ROTOR_II, RotorCatalog.ROTOR_III),
        positions = listOf('A', 'A', 'A'),
        ringSettings = listOf(1, 1, 1),
        plugboardPairs = listOf(
            PlugboardPair('A', 'V'), PlugboardPair('B', 'S'), PlugboardPair('C', 'G'),
            PlugboardPair('D', 'L'), PlugboardPair('F', 'U'), PlugboardPair('H', 'Z'),
            PlugboardPair('I', 'N'), PlugboardPair('K', 'O'), PlugboardPair('M', 'Q'),
            PlugboardPair('R', 'W')
        )
    )

    val KRIEGSMARINE_M3 = EnigmaPreset(
        id = "kriegsmarine_m3",
        name = "Kriegsmarine M3 (Atlantic)",
        description = "Naval three rotor Enigma with Kriegsmarine wheels VI-VII-VIII and two turnover notches.",
        model = EnigmaModel.M3,
        reflectorSpec = ReflectorCatalog.REFLECTOR_B,
        rotorSpecs = listOf(RotorCatalog.ROTOR_VI, RotorCatalog.ROTOR_VII, RotorCatalog.ROTOR_VIII),
        positions = listOf('M', 'C', 'K'),
        ringSettings = listOf(1, 5, 12),
        plugboardPairs = listOf(
            PlugboardPair('A', 'N'), PlugboardPair('E', 'Z'), PlugboardPair('H', 'K'),
            PlugboardPair('I', 'J'), PlugboardPair('L', 'M'), PlugboardPair('O', 'Q')
        )
    )

    val KRIEGSMARINE_M4 = EnigmaPreset(
        id = "kriegsmarine_m4",
        name = "Kriegsmarine M4 (U-Boat)",
        description = "Four rotor U-boat Enigma: Beta Greek wheel with thin Reflector B, ten cables.",
        model = EnigmaModel.M4,
        reflectorSpec = ReflectorCatalog.REFLECTOR_B_THIN,
        greekRotorSpec = RotorCatalog.ROTOR_BETA,
        greekRotorPosition = 'V',
        greekRotorRing = 1,
        rotorSpecs = listOf(RotorCatalog.ROTOR_V, RotorCatalog.ROTOR_VI, RotorCatalog.ROTOR_VIII),
        positions = listOf('N', 'C', 'L'),
        ringSettings = listOf(13, 22, 1),
        plugboardPairs = listOf(
            PlugboardPair('A', 'T'), PlugboardPair('B', 'L'), PlugboardPair('D', 'F'),
            PlugboardPair('G', 'J'), PlugboardPair('H', 'M'), PlugboardPair('N', 'W'),
            PlugboardPair('O', 'P'), PlugboardPair('Q', 'R'), PlugboardPair('S', 'Z'),
            PlugboardPair('V', 'X')
        )
    )

    val OPERATION_BARBAROSSA = EnigmaPreset(
        id = "operation_barbarossa",
        name = "Operation Barbarossa (1941)",
        description = "Eastern Front Army radio net: rotors II-IV-V, Reflector B, four cables.",
        model = EnigmaModel.M3,
        reflectorSpec = ReflectorCatalog.REFLECTOR_B,
        rotorSpecs = listOf(RotorCatalog.ROTOR_II, RotorCatalog.ROTOR_IV, RotorCatalog.ROTOR_V),
        positions = listOf('B', 'L', 'A'),
        ringSettings = listOf(2, 12, 20),
        plugboardPairs = listOf(
            PlugboardPair('A', 'V'), PlugboardPair('B', 'N'), PlugboardPair('C', 'L'),
            PlugboardPair('E', 'X')
        )
    )

    /**
     * The settings used to break the "Scharnhorst" message (bytereef M4 project).
     *
     * Rotors III-VI-VIII, Reflector B, rings A-H-M, ten cables. With the machine set to `UZV`
     * the ciphertext of the published break decrypts to the original German text; this exact
     * key is exercised by the known-answer tests.
     */
    val SCHARNHORST_1943 = EnigmaPreset(
        id = "scharnhorst_1943",
        name = "Scharnhorst Break (1943)",
        description = "Documented M3 key: rotors III-VI-VIII, Reflector B, rings A-H-M, ten cables.",
        model = EnigmaModel.M3,
        reflectorSpec = ReflectorCatalog.REFLECTOR_B,
        rotorSpecs = listOf(RotorCatalog.ROTOR_III, RotorCatalog.ROTOR_VI, RotorCatalog.ROTOR_VIII),
        positions = listOf('U', 'Z', 'V'),
        ringSettings = listOf(1, 8, 13),
        plugboardPairs = listOf(
            PlugboardPair('A', 'N'), PlugboardPair('E', 'Z'), PlugboardPair('H', 'K'),
            PlugboardPair('I', 'J'), PlugboardPair('L', 'R'), PlugboardPair('M', 'Q'),
            PlugboardPair('O', 'T'), PlugboardPair('P', 'V'), PlugboardPair('S', 'W'),
            PlugboardPair('U', 'X')
        )
    )

    /**
     * Illustrative U-505 style M4 key (Beta wheel, thin Reflector C, six cables).
     *
     * The capture of U-505 in June 1944 is historical, the exact daily key used here is a
     * teaching example rather than a transcribed document.
     */
    val U_505_PRESET = EnigmaPreset(
        id = "u_505",
        name = "U-Boat U-505 (1944)",
        description = "Teaching key for the captured U-505: Gamma wheel, thin Reflector C, six cables.",
        model = EnigmaModel.M4,
        reflectorSpec = ReflectorCatalog.REFLECTOR_C_THIN,
        greekRotorSpec = RotorCatalog.ROTOR_GAMMA,
        greekRotorPosition = 'A',
        greekRotorRing = 1,
        rotorSpecs = listOf(RotorCatalog.ROTOR_I, RotorCatalog.ROTOR_II, RotorCatalog.ROTOR_III),
        positions = listOf('Q', 'W', 'E'),
        ringSettings = listOf(1, 1, 1),
        plugboardPairs = listOf(
            PlugboardPair('A', 'E'), PlugboardPair('B', 'F'), PlugboardPair('C', 'M'),
            PlugboardPair('D', 'Y'), PlugboardPair('G', 'H'), PlugboardPair('J', 'K')
        )
    )

    val presets: List<EnigmaPreset> = listOf(
        WEHRMACHT_I,
        KRIEGSMARINE_M3,
        KRIEGSMARINE_M4,
        OPERATION_BARBAROSSA,
        SCHARNHORST_1943,
        U_505_PRESET
    )

    init {
        require(presets.map { it.id }.toSet().size == presets.size) { "Preset ids must be unique" }
    }

    fun getById(id: String): EnigmaPreset {
        return presets.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown preset ID: $id")
    }

    fun forModel(model: EnigmaModel): List<EnigmaPreset> = presets.filter { it.model == model }
}
