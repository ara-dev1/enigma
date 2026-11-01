package com.enigma.engine

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wiring tables. A single mistyped letter used to be able to produce a machine that
 * runs fine but no longer matches the real device, so every table entry is verified here.
 */
class CatalogIntegrityTest {

    @Test
    fun `every rotor wiring is a permutation of the alphabet`() {
        RotorCatalog.allRotors.forEach { spec ->
            assertEquals("${spec.id}: wiring length", Alphabet.SIZE, spec.wiring.length)
            assertEquals("${spec.id}: every letter once", Alphabet.SIZE, spec.wiring.toSet().size)
            assertTrue("${spec.id}: A-Z only", spec.wiring.all { it in 'A'..'Z' })
        }
    }

    @Test
    fun `every reflector is reciprocal and has no fixed points`() {
        ReflectorCatalog.allReflectors.forEach { spec ->
            assertEquals("${spec.id}: wiring length", Alphabet.SIZE, spec.wiring.length)
            for (index in 0 until Alphabet.SIZE) {
                val target = Alphabet.toIndex(spec.wiring[index])
                assertNotEquals("${spec.id}: '${Alphabet.toChar(index)}' maps to itself", index, target)
                assertEquals(
                    "${spec.id}: '${Alphabet.toChar(index)}' is not reciprocal",
                    index,
                    Alphabet.toIndex(spec.wiring[target])
                )
            }
        }
    }

    @Test
    fun `catalog wirings match the published tables`() {
        assertEquals("EKMFLGDQVZNTOWYHXUSPAIBRCJ", RotorCatalog.ROTOR_I.wiring)
        assertEquals("AJDKSIRUXBLHWTMCQGZNPYFVOE", RotorCatalog.ROTOR_II.wiring)
        assertEquals("BDFHJLCPRTXVZNYEIWGAKMUSQO", RotorCatalog.ROTOR_III.wiring)
        assertEquals("ESOVPZJAYQUIRHXLNFTGKDCMWB", RotorCatalog.ROTOR_IV.wiring)
        assertEquals("VZBRGITYUPSDNHLXAWMJQOFECK", RotorCatalog.ROTOR_V.wiring)
        assertEquals("JPGVOUMFYQBENHZRDKASXLICTW", RotorCatalog.ROTOR_VI.wiring)
        assertEquals("NZJHGRCXMYSWBOUFAIVLPEKQDT", RotorCatalog.ROTOR_VII.wiring)
        assertEquals("FKQHTLXOCBJSPDZRAMEWNIUYGV", RotorCatalog.ROTOR_VIII.wiring)
        assertEquals("LEYJVCNIXWPBQMDRTAKZGFUHOS", RotorCatalog.ROTOR_BETA.wiring)
        assertEquals("FSOKANUERHMBTIYCWLQPZXVGJD", RotorCatalog.ROTOR_GAMMA.wiring)

        assertEquals("EJMZALYXVBWFCRQUONTSPIKHGD", ReflectorCatalog.REFLECTOR_A.wiring)
        assertEquals("YRUHQSLDPXNGOKMIEBFZCWVJAT", ReflectorCatalog.REFLECTOR_B.wiring)
        assertEquals("FVPJIAOYEDRZXWGCTKUQSBNMHL", ReflectorCatalog.REFLECTOR_C.wiring)
        assertEquals("ENKQAUYWJICOPBLMDXZVFTHRGS", ReflectorCatalog.REFLECTOR_B_THIN.wiring)
        assertEquals("RDOBJNTKVEHMLFCWZAXGYIPSUQ", ReflectorCatalog.REFLECTOR_C_THIN.wiring)
    }

    @Test
    fun `navy rotors carry two notches at M and Z`() {
        listOf(RotorCatalog.ROTOR_VI, RotorCatalog.ROTOR_VII, RotorCatalog.ROTOR_VIII).forEach { spec ->
            assertEquals("${spec.id}: notches", setOf('M', 'Z'), spec.turnoverNotches)
            assertEquals("${spec.id}: notch notation", "MZ", spec.notchLetters)
        }
    }

    @Test
    fun `army and air force rotors carry a single notch`() {
        assertEquals(setOf('Q'), RotorCatalog.ROTOR_I.turnoverNotches)
        assertEquals(setOf('E'), RotorCatalog.ROTOR_II.turnoverNotches)
        assertEquals(setOf('V'), RotorCatalog.ROTOR_III.turnoverNotches)
        assertEquals(setOf('J'), RotorCatalog.ROTOR_IV.turnoverNotches)
        assertEquals(setOf('Z'), RotorCatalog.ROTOR_V.turnoverNotches)
        assertTrue("Beta never steps", RotorCatalog.ROTOR_BETA.turnoverNotches.isEmpty())
        assertTrue("Gamma never steps", RotorCatalog.ROTOR_GAMMA.turnoverNotches.isEmpty())
    }

    @Test
    fun `malformed specs are rejected immediately`() {
        // Truncated wiring (the historical bug this test suite was written for).
        assertThrows(IllegalArgumentException::class.java) {
            ReflectorSpec(id = "BROKEN", name = "Broken", wiring = "EJMZALYXVBWFCONRTPUSIKHGD")
        }
        assertThrows(IllegalArgumentException::class.java) {
            RotorSpec(id = "BROKEN", name = "Broken", wiring = "ABC", turnoverNotches = setOf('Q'))
        }
        // A 26 letter string that is a permutation but not reciprocal.
        assertThrows(IllegalArgumentException::class.java) {
            ReflectorSpec(id = "BROKEN", name = "Broken", wiring = "BCDEFGHIJKLMNOPQRSTUVWXYZA")
        }
        // Duplicated letter instead of a permutation.
        assertThrows(IllegalArgumentException::class.java) {
            RotorSpec(
                id = "BROKEN",
                name = "Broken",
                wiring = "AACDEFGHIJKLMNOPQRSTUVWXYZ",
                turnoverNotches = setOf('Q')
            )
        }
    }

    @Test
    fun `catalog lookups are case insensitive and informative`() {
        assertEquals("I", RotorCatalog.getById("i").id)
        assertEquals("B_THIN", ReflectorCatalog.getById("b_thin").id)
        assertTrue(ReflectorCatalog.forModel(hasFourthRotor = true).all { it.isThin })
        assertTrue(ReflectorCatalog.forModel(hasFourthRotor = false).none { it.isThin })
        assertThrows(IllegalArgumentException::class.java) { RotorCatalog.getById("IX") }
        assertThrows(IllegalArgumentException::class.java) { ReflectorCatalog.getById("D") }
    }
}
