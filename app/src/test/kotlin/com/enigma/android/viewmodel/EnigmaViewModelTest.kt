package com.enigma.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.enigma.engine.EnigmaMachine
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.preset.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM tests for the app's state holder: no Android runtime, no instrumentation.
 * They cover the parts that used to be untested glue between the engine and the UI.
 */
class EnigmaViewModelTest {

    @Test
    fun `starts with the wehrmacht preset`() {
        val viewModel = EnigmaViewModel()
        val state = viewModel.uiState.value

        assertEquals(PresetCatalog.WEHRMACHT_I.id, state.selectedPresetId)
        assertEquals("AAA", state.windowNotation)
        assertEquals(10, state.plugboardPairs.size)
        assertTrue(state.isAtStartPositions)
        assertFalse(state.canReset)
    }

    @Test
    fun `key presses follow the engine`() {
        val viewModel = EnigmaViewModel()
        val expected = EnigmaMachine.fromPreset(PresetCatalog.WEHRMACHT_I)
        val expectedLamps = expected.processText("AAAAA")

        repeat(5) { viewModel.onKeyPress('a') }

        val state = viewModel.uiState.value
        assertEquals(expected.display(), state.positions.joinToString(""))
        assertEquals(5, state.outputTapeText.length)
        assertEquals(expectedLamps.last(), state.outputTapeText.last())
        assertFalse(state.isAtStartPositions)
        assertTrue(state.canReset)
    }

    @Test
    fun `reset returns the wheels to the key positions`() {
        val viewModel = EnigmaViewModel()
        repeat(7) { viewModel.onKeyPress('X') }
        assertTrue(viewModel.uiState.value.canReset)

        viewModel.resetPositions()

        val state = viewModel.uiState.value
        assertEquals("AAA", state.windowNotation)
        assertTrue(state.isAtStartPositions)
    }

    @Test
    fun `message console round trips through the machine`() {
        val viewModel = EnigmaViewModel()

        viewModel.setMessageInput("hello world")
        assertEquals("HELLO WORLD", viewModel.uiState.value.messageInput)

        viewModel.processMessage()
        val cipherText = viewModel.uiState.value.messageOutput
        assertEquals(10, cipherText.length)
        assertNotEquals("HELLOWORLD", cipherText)

        // Same key, wheels back at the start position: the cipher text decrypts again.
        viewModel.resetPositions()
        viewModel.setMessageInput(cipherText)
        viewModel.processMessage()
        assertEquals("HELLOWORLD", viewModel.uiState.value.messageOutput)
    }

    @Test
    fun `empty messages are reported instead of silently ignored`() {
        val viewModel = EnigmaViewModel()
        viewModel.setMessageInput("   ")
        viewModel.processMessage()

        assertNotNull(viewModel.uiState.value.messageErrorMessage)
        assertEquals("", viewModel.uiState.value.messageOutput)
    }

    @Test
    fun `plugboard rejects duplicates and self connections`() {
        val viewModel = EnigmaViewModel()
        viewModel.clearPlugboard()
        assertTrue(viewModel.uiState.value.plugboardPairs.isEmpty())

        viewModel.addPlugboardPair('A', 'B')
        assertEquals(1, viewModel.uiState.value.plugboardPairs.size)

        viewModel.addPlugboardPair('A', 'C')
        assertNotNull(viewModel.uiState.value.plugboardErrorMessage)
        assertEquals(1, viewModel.uiState.value.plugboardPairs.size)

        viewModel.addPlugboardPair('D', 'D')
        assertNotNull(viewModel.uiState.value.plugboardErrorMessage)
        assertEquals(1, viewModel.uiState.value.plugboardPairs.size)

        viewModel.removePlugboardPair(viewModel.uiState.value.plugboardPairs.first())
        assertTrue(viewModel.uiState.value.plugboardPairs.isEmpty())
    }

    @Test
    fun `presets switch the model and keep the machine consistent`() {
        val viewModel = EnigmaViewModel()

        viewModel.applyPreset(PresetCatalog.KRIEGSMARINE_M4)
        val m4 = viewModel.uiState.value
        assertEquals(EnigmaModel.M4, m4.model)
        assertEquals("VNCL", m4.windowNotation)
        assertEquals(10, m4.plugboardPairs.size)
        assertTrue(m4.reflectorSpec.isThin)

        viewModel.selectModel(EnigmaModel.M3)
        val m3 = viewModel.uiState.value
        assertEquals(EnigmaModel.M3, m3.model)
        assertFalse(m3.reflectorSpec.isThin)
        assertEquals(3, m3.windowNotation.length)
        assertNull(m3.selectedPresetId)
    }

    @Test
    fun `rotor position changes keep machine and start position in sync`() {
        val viewModel = EnigmaViewModel()

        viewModel.setRotorPosition(2, 'Z')
        assertEquals('Z', viewModel.uiState.value.positions[2])
        assertEquals('Z', viewModel.uiState.value.startPositions[2])

        viewModel.onKeyPress('A')
        // Rotor III turns over at V, so Z simply wraps to A and the middle wheel stays put.
        assertEquals('A', viewModel.uiState.value.positions[2])
        assertEquals('A', viewModel.uiState.value.positions[1])
    }

    @Test
    fun `state survives process death through the saved state handle`() {
        val handle = SavedStateHandle()
        val first = EnigmaViewModel(handle)
        first.applyPreset(PresetCatalog.U_505_PRESET)
        repeat(3) { first.onKeyPress('Q') }

        val expectedWindow = first.uiState.value.windowNotation
        val expectedTape = first.uiState.value.outputTapeText
        val expectedStart = first.uiState.value.startPositions

        val second = EnigmaViewModel(handle)
        val restored = second.uiState.value
        assertEquals(expectedWindow, restored.windowNotation)
        assertEquals(expectedTape, restored.outputTapeText)
        assertEquals(expectedStart, restored.startPositions)
        assertEquals(EnigmaModel.M4, restored.model)
        assertEquals(PresetCatalog.U_505_PRESET.plugboardPairs.size, restored.plugboardPairs.size)

        // The restored machine keeps encrypting where the old one stopped.
        second.onKeyPress('Q')
        assertEquals(expectedWindow.length, second.uiState.value.windowNotation.length)
        assertEquals(expectedTape.length + 1, second.uiState.value.outputTapeText.length)
    }

    @Test
    fun `tabs and message helpers`() {
        val viewModel = EnigmaViewModel()
        assertEquals(MachineTab.MACHINE, viewModel.uiState.value.activeTab)

        viewModel.selectTab(MachineTab.MESSAGE)
        assertEquals(MachineTab.MESSAGE, viewModel.uiState.value.activeTab)

        viewModel.setMessageInput("abc")
        viewModel.processMessage()
        assertEquals(3, viewModel.uiState.value.messageOutput.length)

        viewModel.clearMessage()
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals("", viewModel.uiState.value.messageOutput)
    }

    @Test
    fun `output tape can be cleared`() {
        val viewModel = EnigmaViewModel()
        viewModel.onKeyPress('A')
        assertFalse(viewModel.uiState.value.outputTapeText.isEmpty())
        viewModel.clearTape()
        assertEquals("", viewModel.uiState.value.outputTapeText)
    }
}
