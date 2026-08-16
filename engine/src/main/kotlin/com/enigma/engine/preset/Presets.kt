package com.enigma.engine.preset

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec

data class EnigmaPreset(
    val id: String,
    val name: String,
    val description: String,
    val model: EnigmaModel,
    val reflectorSpec: ReflectorSpec,
    val greekRotorSpec: RotorSpec? = null,
    val greekRotorPosition: Char = 'A',
    val greekRotorRing: Int = 1,
    val rotorSpecs: List<RotorSpec>, // [Left, Middle, Right]
    val positions: List<Char>,        // [Left, Middle, Right]
    val ringSettings: List<Int>,     // [Left, Middle, Right]
    val plugboardPairs: List<PlugboardPair>
)

object PresetCatalog {
    val WEHRMACHT_I = EnigmaPreset(
        id = "wehrmacht_i",
        name = "Wehrmacht Standard (1939)",
        description = "Standard Army/Air Force Enigma I configuration (Rotors I-II-III with Reflector B).",
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
        description = "Naval Enigma M3 using naval rotors VI, VII, VIII.",
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
        description = "4-rotor Enigma M4 with Thin Beta rotor and Thin Reflector B.",
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
        description = "Eastern Front Army radio network setup with Reflector B and Rotors II, IV, V.",
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

    val U_505_PRESET = EnigmaPreset(
        id = "u_505",
        name = "U-Boat U-505 Capture",
        description = "Configuration matching the signal captured on U-505 in June 1944.",
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
        U_505_PRESET
    )

    fun getById(id: String): EnigmaPreset {
        return presets.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown preset ID: $id")
    }
}
