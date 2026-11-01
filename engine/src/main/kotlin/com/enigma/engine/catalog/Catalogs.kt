package com.enigma.engine.catalog

import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec

/**
 * Rotor and reflector catalogue.
 *
 * The wirings are the standard published tables (Dirk Rijmenants, "Technical Details of the
 * Enigma Machine"; the rotor tables used by the M4 project). A wiring string lists the
 * contacts in A-Z order: character `i` is the contact reached by pin `i` when the rotor sits
 * at window position A with ring setting A.
 *
 * Notches are given as the **window letters** at which the pawl engages. Kriegsmarine rotors
 * VI-VIII have two notches (M and Z), all other rotors have one.
 *
 * [RotorSpec] and [ReflectorSpec] validate themselves: wirings must be permutations and
 * reflectors must be reciprocal without fixed points, so a mistyped table fails immediately
 * instead of producing a silently wrong cipher.
 */
object RotorCatalog {
    val ROTOR_I = RotorSpec("I", "Rotor I", "EKMFLGDQVZNTOWYHXUSPAIBRCJ", setOf('Q'))
    val ROTOR_II = RotorSpec("II", "Rotor II", "AJDKSIRUXBLHWTMCQGZNPYFVOE", setOf('E'))
    val ROTOR_III = RotorSpec("III", "Rotor III", "BDFHJLCPRTXVZNYEIWGAKMUSQO", setOf('V'))
    val ROTOR_IV = RotorSpec("IV", "Rotor IV", "ESOVPZJAYQUIRHXLNFTGKDCMWB", setOf('J'))
    val ROTOR_V = RotorSpec("V", "Rotor V", "VZBRGITYUPSDNHLXAWMJQOFECK", setOf('Z'))

    // Kriegsmarine rotors: two turnover notches each (at M and Z).
    val ROTOR_VI = RotorSpec("VI", "Rotor VI", "JPGVOUMFYQBENHZRDKASXLICTW", setOf('M', 'Z'))
    val ROTOR_VII = RotorSpec("VII", "Rotor VII", "NZJHGRCXMYSWBOUFAIVLPEKQDT", setOf('M', 'Z'))
    val ROTOR_VIII = RotorSpec("VIII", "Rotor VIII", "FKQHTLXOCBJSPDZRAMEWNIUYGV", setOf('M', 'Z'))

    // M4 thin rotors (Greek rotors) - they never step during encryption.
    val ROTOR_BETA = RotorSpec("BETA", "Beta (Thin)", "LEYJVCNIXWPBQMDRTAKZGFUHOS", emptySet(), isThin = true)
    val ROTOR_GAMMA = RotorSpec("GAMMA", "Gamma (Thin)", "FSOKANUERHMBTIYCWLQPZXVGJD", emptySet(), isThin = true)

    val standardRotors: List<RotorSpec> =
        listOf(ROTOR_I, ROTOR_II, ROTOR_III, ROTOR_IV, ROTOR_V, ROTOR_VI, ROTOR_VII, ROTOR_VIII)
    val greekRotors: List<RotorSpec> = listOf(ROTOR_BETA, ROTOR_GAMMA)
    val allRotors: List<RotorSpec> = standardRotors + greekRotors

    fun getById(id: String): RotorSpec {
        return allRotors.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown rotor spec ID: $id")
    }

    fun findById(id: String?): RotorSpec? =
        if (id == null) null else allRotors.firstOrNull { it.id.equals(id, ignoreCase = true) }
}

object ReflectorCatalog {
    // Reflector A (Umkehrwalze A) of the commercial Enigma models.
    val REFLECTOR_A = ReflectorSpec("A", "Reflector A", "EJMZALYXVBWFCRQUONTSPIKHGD")
    val REFLECTOR_B = ReflectorSpec("B", "Reflector B", "YRUHQSLDPXNGOKMIEBFZCWVJAT")
    val REFLECTOR_C = ReflectorSpec("C", "Reflector C", "FVPJIAOYEDRZXWGCTKUQSBNMHL")

    // M4 thin reflectors (Umkehrwalze B Thin / C Thin), used together with Beta or Gamma.
    val REFLECTOR_B_THIN =
        ReflectorSpec("B_THIN", "Reflector B Thin", "ENKQAUYWJICOPBLMDXZVFTHRGS", isThin = true)
    val REFLECTOR_C_THIN =
        ReflectorSpec("C_THIN", "Reflector C Thin", "RDOBJNTKVEHMLFCWZAXGYIPSUQ", isThin = true)

    val standardReflectors: List<ReflectorSpec> = listOf(REFLECTOR_A, REFLECTOR_B, REFLECTOR_C)
    val thinReflectors: List<ReflectorSpec> = listOf(REFLECTOR_B_THIN, REFLECTOR_C_THIN)
    val allReflectors: List<ReflectorSpec> = standardReflectors + thinReflectors

    fun getById(id: String): ReflectorSpec {
        return allReflectors.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown reflector spec ID: $id")
    }

    fun findById(id: String?): ReflectorSpec? =
        if (id == null) null else allReflectors.firstOrNull { it.id.equals(id, ignoreCase = true) }

    /** Reflectors that are legal for the given model. */
    fun forModel(hasFourthRotor: Boolean): List<ReflectorSpec> =
        if (hasFourthRotor) thinReflectors else standardReflectors
}
