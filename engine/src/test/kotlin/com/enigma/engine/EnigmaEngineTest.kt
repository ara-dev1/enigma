package com.enigma.engine

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.Rotor
import com.enigma.engine.preset.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnigmaEngineTest {

    @Test
    fun testAlphabetUtility() {
        assertEquals(0, Alphabet.toIndex('A'))
        assertEquals(25, Alphabet.toIndex('Z'))
        assertEquals('A', Alphabet.toChar(0))
        assertEquals('Z', Alphabet.toChar(25))
        assertEquals('A', Alphabet.toChar(26))
    }

    @Test
    fun testPlugboardSwapping() {
        val pairs = listOf(
            PlugboardPair('A', 'B'),
            PlugboardPair('C', 'D')
        )
        val plugboard = Plugboard(pairs)

        assertEquals('B', plugboard.process('A'))
        assertEquals('A', plugboard.process('B'))
        assertEquals('D', plugboard.process('C'))
        assertEquals('C', plugboard.process('D'))
        assertEquals('E', plugboard.process('E'))
    }

    @Test
    fun testRotorForwardAndBackward() {
        // Test Rotor I without ring offset at position A
        val rotorI = Rotor(RotorCatalog.ROTOR_I, 'A', 1)
        // Wiring for Rotor I: EKMFLGDQVZNTOWYHXUSPAIBRCJ
        // 'A' (index 0) maps to 'E' (index 4)
        assertEquals(4, rotorI.forward(0))
        // Inverse: 'E' (index 4) backward maps to 'A' (index 0)
        assertEquals(0, rotorI.backward(4))
    }

    @Test
    fun testRotorRingSettingOffset() {
        // Rotor I with ring setting 2 (B=2) and position A
        val rotorWithRing = Rotor(RotorCatalog.ROTOR_I, 'A', 2)
        // Check that forward and backward are inverse functions
        for (i in 0 until 26) {
            val fwd = rotorWithRing.forward(i)
            val bwd = rotorWithRing.backward(fwd)
            assertEquals(i, bwd)
        }
    }

    @Test
    fun testRotorSteppingAndDoubleStepping() {
        // Rotor I notch is at 'Q', Rotor II notch is at 'E', Rotor III notch is at 'V'
        val left = Rotor(RotorCatalog.ROTOR_I, 'A', 1)
        val middle = Rotor(RotorCatalog.ROTOR_II, 'D', 1) // 1 step away from notch 'E'
        val right = Rotor(RotorCatalog.ROTOR_III, 'U', 1)  // 1 step away from notch 'V'

        val machine = EnigmaMachine(
            model = EnigmaModel.M3,
            reflector = Reflector(ReflectorCatalog.REFLECTOR_B),
            leftRotor = left,
            middleRotor = middle,
            rightRotor = right
        )

        // Initial state: A D U
        assertEquals('A', machine.leftRotor.position)
        assertEquals('D', machine.middleRotor.position)
        assertEquals('U', machine.rightRotor.position)

        // Step 1: Right rotor steps U -> V (hits notch V)
        machine.step()
        assertEquals('A', machine.leftRotor.position)
        assertEquals('D', machine.middleRotor.position)
        assertEquals('V', machine.rightRotor.position)

        // Step 2: Right rotor steps V -> W. Since right was at notch V, middle steps D -> E
        machine.step()
        assertEquals('A', machine.leftRotor.position)
        assertEquals('E', machine.middleRotor.position)
        assertEquals('W', machine.rightRotor.position)

        // Step 3: Double-stepping anomaly!
        // Middle rotor is now at notch 'E'. Middle steps E -> F, AND left steps A -> B!
        machine.step()
        assertEquals('B', machine.leftRotor.position)
        assertEquals('F', machine.middleRotor.position)
        assertEquals('X', machine.rightRotor.position)
    }

    @Test
    fun testEnigmaSymmetryDecryption() {
        val originalText = "ENIGMAMACHINEPROJECTIONTEST"

        val machine1 = EnigmaMachine.createDefaultM3()
        val ciphertext = machine1.processText(originalText)

        assertNotEquals(originalText, ciphertext)

        val machine2 = EnigmaMachine.createDefaultM3()
        val decryptedText = machine2.processText(ciphertext)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testM4DecryptionSymmetry() {
        val originalText = "SECRETUBOATTRANSMISSION"

        val machine1 = EnigmaMachine.createDefaultM4()
        val ciphertext = machine1.processText(originalText)

        assertNotEquals(originalText, ciphertext)

        val machine2 = EnigmaMachine.createDefaultM4()
        val decryptedText = machine2.processText(ciphertext)

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testSignalPathTrace() {
        val machine = EnigmaMachine.createDefaultM3()
        val trace = machine.processWithTrace('A')

        assertEquals('A', trace.inputChar)
        assertTrue(trace.steps.isNotEmpty())
        assertEquals(trace.outputChar, trace.steps.last().character)
    }

    @Test
    fun testPresetWehrmachtI() {
        val preset = PresetCatalog.WEHRMACHT_I
        val machine = EnigmaMachine.fromPreset(preset)

        val plaintext = "HELLOWORLD"
        val ciphertext = machine.processText(plaintext)

        val machineDec = EnigmaMachine.fromPreset(preset)
        val decrypted = machineDec.processText(ciphertext)

        assertEquals(plaintext, decrypted)
    }
}
