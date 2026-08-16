package com.enigma.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enigma.android.ui.theme.BrassAccent
import com.enigma.android.ui.theme.LampGold
import com.enigma.android.viewmodel.EnigmaUiState
import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec
import com.enigma.engine.preset.PresetCatalog

@Composable
fun TopMenuBar(
    uiState: EnigmaUiState,
    onModelSelected: (EnigmaModel) -> Unit,
    onPresetSelected: (String) -> Unit
) {
    var presetExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENIGMA MACHINE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrassAccent,
                    fontFamily = FontFamily.Monospace
                )

                // Enigma Model Switcher
                Row {
                    listOf(EnigmaModel.M3, EnigmaModel.M4).forEach { model ->
                        val isSelected = uiState.model == model
                        Button(
                            onClick = { onModelSelected(model) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) BrassAccent else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(model.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Dropdown Selector
            Box {
                val currentPresetName = PresetCatalog.presets.firstOrNull { it.id == uiState.selectedPresetId }?.name ?: "Custom Configuration"
                OutlinedButton(
                    onClick = { presetExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preset: $currentPresetName", color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Preset")
                }

                DropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false }
                ) {
                    PresetCatalog.presets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.name, fontWeight = FontWeight.Bold)
                                    Text(preset.description, fontSize = 11.sp, color = Color.Gray)
                                }
                            },
                            onClick = {
                                presetExpanded = false
                                onPresetSelected(preset.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RotorControlPanel(
    uiState: EnigmaUiState,
    onReflectorChanged: (ReflectorSpec) -> Unit,
    onRotorSpecChanged: (Int, RotorSpec) -> Unit,
    onGreekSpecChanged: (RotorSpec) -> Unit,
    onPositionChanged: (Int, Char) -> Unit,
    onGreekPositionChanged: (Char) -> Unit,
    onRingChanged: (Int, Int) -> Unit,
    onGreekRingChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "ROTOR & REFLECTOR CONFIGURATION",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrassAccent,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Reflector selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reflector: ", fontWeight = FontWeight.Medium, modifier = Modifier.width(90.dp))
                val availableReflectors = if (uiState.model == EnigmaModel.M4) ReflectorCatalog.thinReflectors else ReflectorCatalog.standardReflectors
                availableReflectors.forEach { refSpec ->
                    val isSelected = uiState.reflectorSpec.id == refSpec.id
                    OutlinedButton(
                        onClick = { onReflectorChanged(refSpec) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) BrassAccent else Color.Transparent,
                            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(refSpec.name.replace("Reflector ", ""))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rotor Wheels (4 for M4, 3 for M3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (uiState.model == EnigmaModel.M4) {
                    SingleRotorDial(
                        label = "Greek",
                        currentSpec = uiState.greekRotorSpec,
                        availableSpecs = RotorCatalog.greekRotors,
                        position = uiState.greekPosition,
                        ring = uiState.greekRing,
                        onSpecChanged = onGreekSpecChanged,
                        onPositionChanged = onGreekPositionChanged,
                        onRingChanged = onGreekRingChanged
                    )
                }

                val labels = listOf("Left (1)", "Middle (2)", "Right (3)")
                for (i in 0..2) {
                    SingleRotorDial(
                        label = labels[i],
                        currentSpec = uiState.rotorSpecs[i],
                        availableSpecs = RotorCatalog.standardRotors,
                        position = uiState.positions[i],
                        ring = uiState.ringSettings[i],
                        onSpecChanged = { spec -> onRotorSpecChanged(i, spec) },
                        onPositionChanged = { pos -> onPositionChanged(i, pos) },
                        onRingChanged = { ring -> onRingChanged(i, ring) }
                    )
                }
            }
        }
    }
}

@Composable
fun SingleRotorDial(
    label: String,
    currentSpec: RotorSpec,
    availableSpecs: List<RotorSpec>,
    position: Char,
    ring: Int,
    onSpecChanged: (RotorSpec) -> Unit,
    onPositionChanged: (Char) -> Unit,
    onRingChanged: (Int) -> Unit
) {
    var specExpanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        // Rotor spec dropdown
        Box {
            OutlinedButton(
                onClick = { specExpanded = true },
                modifier = Modifier.width(80.dp)
            ) {
                Text(currentSpec.id, fontSize = 12.sp)
            }
            DropdownMenu(expanded = specExpanded, onDismissRequest = { specExpanded = false }) {
                availableSpecs.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec.name) },
                        onClick = {
                            specExpanded = false
                            onSpecChanged(spec)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Position Dial (A-Z)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.border(1.dp, BrassAccent, RoundedCornerShape(8.dp))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                IconButton(
                    onClick = {
                        val prev = Alphabet.toChar(Alphabet.toIndex(position) - 1)
                        onPositionChanged(prev)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("▲", fontSize = 12.sp)
                }

                Text(
                    text = "$position",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrassAccent,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                IconButton(
                    onClick = {
                        val next = Alphabet.toChar(Alphabet.toIndex(position) + 1)
                        onPositionChanged(next)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("▼", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ring Setting (1..26)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ring: ", fontSize = 10.sp)
            Button(
                onClick = {
                    val newRing = if (ring == 1) 26 else ring - 1
                    onRingChanged(newRing)
                },
                modifier = Modifier.size(22.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("-", fontSize = 10.sp)
            }
            Text(
                text = String.format("%02d", ring),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            Button(
                onClick = {
                    val newRing = if (ring == 26) 1 else ring + 1
                    onRingChanged(newRing)
                },
                modifier = Modifier.size(22.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlugboardPanel(
    pairs: List<PlugboardPair>,
    errorMessage: String?,
    onAddPair: (Char, Char) -> Unit,
    onRemovePair: (PlugboardPair) -> Unit,
    onClearAll: () -> Unit
) {
    var char1Input by remember { mutableStateOf("") }
    var char2Input by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "STECKERBRETT (PLUGBOARD)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BrassAccent,
                    fontFamily = FontFamily.Monospace
                )
                if (pairs.isNotEmpty()) {
                    IconButton(onClick = onClearAll, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Plugboard", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active pair chips
            FlowRow(
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                pairs.forEach { pair ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${pair.first} ↔ ${pair.second}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrassAccent
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove Pair",
                                modifier = Modifier.size(14.dp).clickable { onRemovePair(pair) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add new pair input row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = char1Input,
                    onValueChange = { if (it.length <= 1) char1Input = it.uppercase() },
                    label = { Text("Letter 1", fontSize = 10.sp) },
                    modifier = Modifier.width(70.dp).height(50.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = char2Input,
                    onValueChange = { if (it.length <= 1) char2Input = it.uppercase() },
                    label = { Text("Letter 2", fontSize = 10.sp) },
                    modifier = Modifier.width(70.dp).height(50.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (char1Input.isNotEmpty() && char2Input.isNotEmpty()) {
                            onAddPair(char1Input[0], char2Input[0])
                            char1Input = ""
                            char2Input = ""
                        }
                    },
                    enabled = char1Input.isNotEmpty() && char2Input.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Connect")
                    Text(" Connect")
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LampboardAndKeyboardPanel(
    litLamp: Char?,
    onKeyPress: (Char) -> Unit,
    onKeyRelease: () -> Unit
) {
    // Standard Enigma QWERTZ layout
    val keyboardRows = listOf(
        listOf('Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O'),
        listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K'),
        listOf('P', 'Y', 'X', 'C', 'V', 'B', 'N', 'M', 'L')
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LAMPBOARD (LIGHT OUTPUT)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BrassAccent,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Lampboard Grid
            keyboardRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    row.forEach { char ->
                        val isLit = litLamp == char
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isLit) LampGold else Color(0xFF2B2B2B))
                                .shadow(if (isLit) 8.dp else 0.dp, CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isLit) Color.White else Color.DarkGray,
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = "$char",
                                color = if (isLit) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "ENIGMA KEYBOARD (INPUT)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BrassAccent,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Keyboard Grid
            keyboardRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    row.forEach { char ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E1E))
                                .border(2.dp, BrassAccent, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            onKeyPress(char)
                                            tryAwaitRelease()
                                            onKeyRelease()
                                        }
                                    )
                                }
                        ) {
                            Text(
                                text = "$char",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutputTapePanel(
    text: String,
    onClear: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    // Format text into blocks of 5 characters (historical Enigma message format)
    val formattedText = text.chunked(5).joinToString(" ")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "OUTPUT TAPE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BrassAccent,
                    fontFamily = FontFamily.Monospace
                )
                Row {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(formattedText)) },
                        enabled = text.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text")
                    }
                    IconButton(onClick = onClear, enabled = text.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Tape")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Vintage paper tape visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFDD0), RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (formattedText.isEmpty()) "TYPED CHARACTERS WILL APPEAR HERE..." else formattedText,
                    color = if (formattedText.isEmpty()) Color.LightGray else Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SignalPathTraceView(uiState: EnigmaUiState) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Trace", tint = BrassAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ELECTRICAL SIGNAL PATH TRACE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrassAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(if (expanded) "▲" else "▼", color = BrassAccent)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val trace = uiState.latestTrace
                    if (trace == null) {
                        Text("Press a key on the keyboard to trace the electrical signal path.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Text("Signal Trace for Key '${trace.inputChar}' -> Lamp '${trace.outputChar}':", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        trace.steps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}. ", fontSize = 11.sp, color = BrassAccent, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = step.stage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(110.dp)
                                )
                                Text(
                                    text = "-> '${step.character}' ",
                                    fontSize = 11.sp,
                                    color = LampGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(50.dp)
                                )
                                Text(
                                    text = step.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: com.enigma.android.viewmodel.EnigmaViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        TopMenuBar(
            uiState = uiState,
            onModelSelected = { viewModel.selectModel(it) },
            onPresetSelected = { presetId ->
                val preset = PresetCatalog.getById(presetId)
                viewModel.applyPreset(preset)
            }
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Enigma Machine", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Settings & Plugboard", modifier = Modifier.padding(12.dp))
            }
        }

        if (selectedTab == 0) {
            // Machine View: Lampboard, Keyboard, Output Tape & Signal Path
            LampboardAndKeyboardPanel(
                litLamp = uiState.litLamp,
                onKeyPress = { viewModel.onKeyPress(it) },
                onKeyRelease = { viewModel.onKeyRelease() }
            )

            OutputTapePanel(
                text = uiState.outputTapeText,
                onClear = { viewModel.clearTape() }
            )

            SignalPathTraceView(uiState = uiState)
        } else {
            // Configuration View: Rotors, Reflectors, Plugboard
            RotorControlPanel(
                uiState = uiState,
                onReflectorChanged = { viewModel.setReflector(it) },
                onRotorSpecChanged = { idx, spec -> viewModel.setRotorSpec(idx, spec) },
                onGreekSpecChanged = { viewModel.setGreekRotorSpec(it) },
                onPositionChanged = { idx, pos -> viewModel.setRotorPosition(idx, pos) },
                onGreekPositionChanged = { viewModel.setGreekPosition(it) },
                onRingChanged = { idx, ring -> viewModel.setRingSetting(idx, ring) },
                onGreekRingChanged = { viewModel.setGreekRing(it) }
            )

            PlugboardPanel(
                pairs = uiState.plugboardPairs,
                errorMessage = uiState.plugboardErrorMessage,
                onAddPair = { c1, c2 -> viewModel.addPlugboardPair(c1, c2) },
                onRemovePair = { viewModel.removePlugboardPair(it) },
                onClearAll = { viewModel.clearPlugboard() }
            )
        }
    }
}
