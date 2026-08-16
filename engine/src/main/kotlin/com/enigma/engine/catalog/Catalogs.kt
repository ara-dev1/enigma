package com.enigma.engine.catalog

import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec

object RotorCatalog {
    val ROTOR_I = RotorSpec("I", "Rotor I", "EKMFLGDQVZNTOWYHXUSPAIBRCJ", setOf('Q'))
    val ROTOR_II = RotorSpec("II", "Rotor II", "AJDKSIRUXBLHWTMCQGZNPYFVOE", setOf('E'))
    val ROTOR_III = RotorSpec("III", "Rotor III", "BDFHJLCPRTXVZNYEIWGAKMUSQO", setOf('V'))
    val ROTOR_IV = RotorSpec("IV", "Rotor IV", "ESOVPZJAYQUIRHXLNFTGKDCMWB", setOf('J'))
    val ROTOR_V = RotorSpec("V", "Rotor V", "VZBRGITYUPSDNHLXAWMJQOFECK", setOf('Z'))
    val ROTOR_VI = RotorSpec("VI", "Rotor VI", "JPGVOUMFYQBENHZRDKASXLICTW", setOf('A', 'N'))
    val ROTOR_VII = RotorSpec("VII", "Rotor VII", "NZJHGRCXMYSWBOUFAIVLPEKQDT", setOf('A', 'N'))
    val ROTOR_VIII = RotorSpec("VIII", "Rotor VIII", "FKQHTLXOCBJSPDZRAMEWNIUYGV", setOf('A', 'N'))

    // M4 Thin rotors (Greek rotors) - thin rotors do not step during encryption
    val ROTOR_BETA = RotorSpec("BETA", "Beta (Thin)", "LEYJVCNIXWPBQMDRTAKZGFUHOS", emptySet(), isThin = true)
    val ROTOR_GAMMA = RotorSpec("GAMMA", "Gamma (Thin)", "FSOKANUERHMBTIYCWLQPZXVGJD", emptySet(), isThin = true)

    val standardRotors: List<RotorSpec> = listOf(ROTOR_I, ROTOR_II, ROTOR_III, ROTOR_IV, ROTOR_V, ROTOR_VI, ROTOR_VII, ROTOR_VIII)
    val greekRotors: List<RotorSpec> = listOf(ROTOR_BETA, ROTOR_GAMMA)
    val allRotors: List<RotorSpec> = standardRotors + greekRotors

    fun getById(id: String): RotorSpec {
        return allRotors.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown rotor spec ID: $id")
    }
}

object ReflectorCatalog {
    val REFLECTOR_A = ReflectorSpec("A", "Reflector A", "EJMZALYXVBWFCONRTPUSIKHGD")
    val REFLECTOR_B = ReflectorSpec("B", "Reflector B", "YRUHQSLDPXNGOKMIEBFZCWVJAT")
    val REFLECTOR_C = ReflectorSpec("C", "Reflector C", "FVPJIAOYEDRZXWGCTKUQSBNMHL")

    // M4 Thin Reflectors
    val REFLECTOR_B_THIN = ReflectorSpec("B_THIN", "Reflector B Thin", "ENKQAMZYXVCWFBPODTURSJLIHG", isThin = true)
    val REFLECTOR_C_THIN = ReflectorSpec("C_THIN", "Reflector C Thin", "RDOBJNTKVEHMLFCZSAQGWIUYXP", isThin = true)

    val standardReflectors: List<ReflectorSpec> = listOf(REFLECTOR_A, REFLECTOR_B, REFLECTOR_C)
    val thinReflectors: List<ReflectorSpec> = listOf(REFLECTOR_B_THIN, REFLECTOR_C_THIN)
    val allReflectors: List<ReflectorSpec> = standardReflectors + thinReflectors

    fun getById(id: String): ReflectorSpec {
        return allReflectors.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown reflector spec ID: $id")
    }
}
