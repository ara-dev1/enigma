package com.enigma.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.enigma.android.ui.components.MainScreen
import com.enigma.android.ui.theme.EnigmaTheme
import com.enigma.android.viewmodel.EnigmaViewModel

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
}
