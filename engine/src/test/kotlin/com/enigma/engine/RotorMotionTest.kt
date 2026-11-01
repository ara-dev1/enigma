package com.enigma.engine

import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.Rotor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RotorMotionTest {

    @Test
    fun `forward and backward are inverse for every position and ring setting`() {
        RotorCatalog.standardRotors.forEach { spec ->
            for (position in 0 until Alphabet.SIZE) {
                for (ring in 1..Alphabet.SIZE) {
                    val rotor = Rotor(spec, Alphabet.toChar(position), ring)
                    for (index in 0 until Alphabet.SIZE) {
                        assertEquals(
                            "${spec.id} at ${rotor.position} ring $ring: $index",
                            index,
                            rotor.backward(rotor.forward(index))
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `turnover is driven by the window letter, never by the ring setting`() {
        // The notch sits on the alphabet ring, so turning the ring moves the wiring core but not
        // the window letter at which the pawl engages.
        for (ring in 1..Alphabet.SIZE) {
            val rotor = Rotor(RotorCatalog.ROTOR_I, 'Q', ring)
            assertTrue("ring $ring should turn over at Q", rotor.isAtNotch())
            rotor.position = 'R'
            assertFalse("ring $ring must not turn over at R", rotor.isAtNotch())
        }
    }

    @Test
    fun `one ring step behaves like one position step for the wiring`() {
        val ringShifted = Rotor(RotorCatalog.ROTOR_V, 'A', 2)
        val positionShifted = Rotor(RotorCatalog.ROTOR_V, 'Z', 1)
        for (index in 0 until Alphabet.SIZE) {
            assertEquals(positionShifted.forward(index), ringShifted.forward(index))
            assertEquals(positionShifted.backward(index), ringShifted.backward(index))
        }
        // ... but only the position moves the notch.
        assertTrue(positionShifted.isAtNotch())
        assertFalse(ringShifted.isAtNotch())
    }

    @Test
    fun `stepping wraps around the alphabet`() {
        val rotor = Rotor(RotorCatalog.ROTOR_III, 'Z', 26)
        assertEquals('A', rotor.step())
        assertEquals('Z', rotor.rotate(-1))
        assertEquals('A', rotor.rotate(1))
        assertEquals(25, Rotor(RotorCatalog.ROTOR_III, 'Z', 1).positionIndex)
        assertEquals(25, Rotor(RotorCatalog.ROTOR_III, 'A', 26).ringOffset)
    }

    @Test
    fun `rotor state is validated`() {
        val rotor = Rotor(RotorCatalog.ROTOR_I)
        assertThrows(IllegalArgumentException::class.java) { rotor.position = '1' }
        assertThrows(IllegalArgumentException::class.java) { rotor.ringSetting = 0 }
        assertThrows(IllegalArgumentException::class.java) { rotor.ringSetting = 27 }
        rotor.position = 'q'
        assertEquals('Q', rotor.position)
    }

    @Test
    fun `copy keeps position and ring setting independent`() {
        val rotor = Rotor(RotorCatalog.ROTOR_II, 'M', 7)
        val copy = rotor.copy(position = 'N')
        assertEquals('N', copy.position)
        assertEquals(7, copy.ringSetting)
        assertEquals('M', rotor.position)
        assertNotEquals(rotor.forward(3), copy.forward(3))
    }

    @Test
    fun `thin rotors are flagged`() {
        assertTrue(RotorCatalog.ROTOR_BETA.isThin)
        assertTrue(RotorCatalog.ROTOR_GAMMA.isThin)
        assertFalse(RotorCatalog.ROTOR_I.isThin)
        assertTrue(Rotor(RotorCatalog.ROTOR_BETA).isThin)
    }
}
