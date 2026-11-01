package com.enigma.android

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.enigma.android.ui.components.MainScreen
import com.enigma.android.ui.theme.EnigmaTheme
import com.enigma.android.viewmodel.EnigmaViewModel
import com.enigma.android.viewmodel.MachineTab

class MainActivity : ComponentActivity() {

    private val viewModel: EnigmaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnigmaTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    /**
     * Hardware keyboards behave like the original front panel: while the machine screen is
     * open, typed letters press the Enigma keys. The message console keeps normal text input,
     * so letters are only intercepted on the machine tab.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isMachineKey(event)) {
            viewModel.onKeyPress(event.unicodeChar.toChar().uppercaseChar())
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isMachineKey(event)) {
            viewModel.onKeyRelease()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isMachineKey(event: KeyEvent): Boolean {
        if (viewModel.uiState.value.activeTab != MachineTab.MACHINE) return false
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
        val char = event.unicodeChar.toChar().uppercaseChar()
        return char in 'A'..'Z'
    }
}
