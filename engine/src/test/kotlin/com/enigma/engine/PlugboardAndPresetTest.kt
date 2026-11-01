package com.enigma.engine

import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.preset.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlugboardAndPresetTest {

    // ------------------------------------------------------------------ plugboard

    @Test
    fun `pairs are swapped in both directions`() {
        val plugboard = Plugboard(listOf(PlugboardPair('A', 'B'), PlugboardPair('C', 'D')))
        assertEquals('B', plugboard.process('A'))
        assertEquals('A', plugboard.process('B'))
        assertEquals('D', plugboard.process('C'))
        assertEquals('C', plugboard.process('D'))
        assertEquals('E', plugboard.process('E'))
        assertEquals(2, plugboard.size)
        assertEquals("AB CD", plugboard.toString())
    }

    @Test
    fun `a letter can only be plugged once`() {
        assertThrows(IllegalArgumentException::class.java) {
            Plugboard(listOf(PlugboardPair('A', 'B'), PlugboardPair('A', 'C')))
        }
        assertThrows(IllegalArgumentException::class.java) { PlugboardPair('A', 'A') }
        assertThrows(IllegalArgumentException::class.java) { PlugboardPair('A', '1') }
    }

    @Test
    fun `the board supports thirteen cables`() {
        val pairs = (0 until 13).map { index ->
            PlugboardPair(('A' + index * 2), ('A' + index * 2 + 1))
        }
        val plugboard = Plugboard(pairs)
        assertEquals(13, plugboard.size)
        assertFalse(plugboard.isEmpty)
        assertEquals(26, plugboard.pairsList.flatMap { listOf(it.first, it.second) }.toSet().size)
    }

    @Test
    fun `non letters pass through untouched`() {
        val plugboard = Plugboard(listOf(PlugboardPair('A', 'V')))
        assertEquals("AVZ 1-", plugboard.process("VAZ 1-"))
        assertTrue(plugboard.isPlugged('A'))
        assertFalse(plugboard.isPlugged('Z'))
        assertTrue(Plugboard().isEmpty)
    }

    @Test
    fun `plugboard notation can be parsed`() {
        val plugboard = EnigmaMachine.parsePlugboard("AV BS,CG")
        assertEquals(3, plugboard.size)
        assertEquals('V', plugboard.process('A'))
        assertThrows(IllegalArgumentException::class.java) { EnigmaMachine.parsePlugboard("AV BSX") }
    }

    // ------------------------------------------------------------------ presets

    @Test
    fun `every preset builds a valid machine`() {
        PresetCatalog.presets.forEach { preset ->
            val machine = EnigmaMachine.fromPreset(preset)
            assertEquals("${preset.id}: model", preset.model, machine.model)
            assertEquals("${preset.id}: display", preset.positionNotation, machine.display())
            assertEquals("${preset.id}: cables", preset.plugboardPairs.size, machine.plugboard.size)
            assertEquals("${preset.id}: reflector", preset.reflectorSpec.id, machine.reflector.spec.id)
        }
    }

    @Test
    fun `every preset is reciprocal`() {
        PresetCatalog.presets.forEach { preset ->
            val plaintext = "ENIGMAISNOTBROKEN"
            val ciphertext = EnigmaMachine.fromPreset(preset).processText(plaintext)
            assertNotEquals("${preset.id}: ciphertext looks like plaintext", plaintext, ciphertext)
            assertEquals("${preset.id}: decryption", plaintext, EnigmaMachine.fromPreset(preset).processText(ciphertext))
        }
    }

    @Test
    fun `preset ids are unique and searchable`() {
        assertThrows(IllegalArgumentException::class.java) { PresetCatalog.getById("does-not-exist") }
        assertEquals(PresetCatalog.WEHRMACHT_I, PresetCatalog.getById("WEHRMACHT_I"))
        assertTrue(PresetCatalog.forModel(EnigmaModel.M4).all { it.model == EnigmaModel.M4 })
        assertTrue(PresetCatalog.forModel(EnigmaModel.M3).isNotEmpty())
    }

    @Test
    fun `preset notation is rendered left to right with the greek rotor first`() {
        val m4 = PresetCatalog.KRIEGSMARINE_M4
        assertEquals(4, m4.positionNotation.length)
        assertEquals('V', m4.positionNotation[0])
        assertEquals(4, m4.ringNotation.length)

        val m3 = PresetCatalog.WEHRMACHT_I
        assertEquals("AAA", m3.positionNotation)
        assertEquals("AAA", m3.ringNotation)
        assertEquals("I II III", m3.rotorOrder)
        assertEquals("AV BS CG DL FU HZ IN KM OW RX", m3.plugboardNotation)
    }
}
