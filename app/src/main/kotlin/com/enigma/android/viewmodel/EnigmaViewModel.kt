package com.enigma.android.viewmodel

import androidx.lifecycle.ViewModel
import com.enigma.engine.EnigmaMachine
import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.Rotor
import com.enigma.engine.model.RotorSpec
import com.enigma.engine.model.SignalPathTrace
import com.enigma.engine.preset.EnigmaPreset
import com.enigma.engine.preset.PresetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EnigmaUiState(
    val model: EnigmaModel = EnigmaModel.M3,
    val selectedPresetId: String? = "wehrmacht_i",
    val reflectorSpec: ReflectorSpec = ReflectorCatalog.REFLECTOR_B,
    val greekRotorSpec: RotorSpec = RotorCatalog.ROTOR_BETA,
    val greekPosition: Char = 'A',
    val greekRing: Int = 1,
    val rotorSpecs: List<RotorSpec> = listOf(RotorCatalog.ROTOR_I, RotorCatalog.ROTOR_II, RotorCatalog.ROTOR_III),
    val positions: List<Char> = listOf('A', 'A', 'A'),
    val ringSettings: List<Int> = listOf(1, 1, 1),
    val plugboardPairs: List<PlugboardPair> = emptyList(),
    val litLamp: Char? = null,
    val outputTapeText: String = "",
    val latestTrace: SignalPathTrace? = null,
    val plugboardErrorMessage: String? = null
)

class EnigmaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EnigmaUiState())
    val uiState: StateFlow<EnigmaUiState> = _uiState.asStateFlow()

    private var machine: EnigmaMachine = EnigmaMachine.createDefaultM3()

    init {
        applyPreset(PresetCatalog.WEHRMACHT_I)
    }

    private fun rebuildMachine(): EnigmaMachine {
        val state = _uiState.value
        val ref = Reflector(state.reflectorSpec)
        val left = Rotor(state.rotorSpecs[0], state.positions[0], state.ringSettings[0])
        val middle = Rotor(state.rotorSpecs[1], state.positions[1], state.ringSettings[1])
        val right = Rotor(state.rotorSpecs[2], state.positions[2], state.ringSettings[2])
        val greek = if (state.model == EnigmaModel.M4) {
            Rotor(state.greekRotorSpec, state.greekPosition, state.greekRing)
        } else null
        val plug = Plugboard(state.plugboardPairs)

        return EnigmaMachine(
            model = state.model,
            reflector = ref,
            greekRotor = greek,
            leftRotor = left,
            middleRotor = middle,
            rightRotor = right,
            plugboard = plug
        )
    }

    fun selectModel(model: EnigmaModel) {
        if (_uiState.value.model == model) return
        _uiState.update { current ->
            val refSpec = if (model == EnigmaModel.M4) ReflectorCatalog.REFLECTOR_B_THIN else ReflectorCatalog.REFLECTOR_B
            current.copy(
                model = model,
                selectedPresetId = null,
                reflectorSpec = refSpec
            )
        }
        machine = rebuildMachine()
    }

    fun applyPreset(preset: EnigmaPreset) {
        _uiState.update { current ->
            current.copy(
                model = preset.model,
                selectedPresetId = preset.id,
                reflectorSpec = preset.reflectorSpec,
                greekRotorSpec = preset.greekRotorSpec ?: RotorCatalog.ROTOR_BETA,
                greekPosition = preset.greekRotorPosition,
                greekRing = preset.greekRotorRing,
                rotorSpecs = preset.rotorSpecs,
                positions = preset.positions,
                ringSettings = preset.ringSettings,
                plugboardPairs = preset.plugboardPairs,
                litLamp = null,
                latestTrace = null,
                plugboardErrorMessage = null
            )
        }
        machine = rebuildMachine()
    }

    fun setReflector(reflectorSpec: ReflectorSpec) {
        _uiState.update { it.copy(reflectorSpec = reflectorSpec, selectedPresetId = null) }
        machine = rebuildMachine()
    }

    fun setRotorSpec(index: Int, spec: RotorSpec) {
        _uiState.update { current ->
            val updatedSpecs = current.rotorSpecs.toMutableList()
            updatedSpecs[index] = spec
            current.copy(rotorSpecs = updatedSpecs, selectedPresetId = null)
        }
        machine = rebuildMachine()
    }

    fun setGreekRotorSpec(spec: RotorSpec) {
        _uiState.update { it.copy(greekRotorSpec = spec, selectedPresetId = null) }
        machine = rebuildMachine()
    }

    fun setRotorPosition(index: Int, position: Char) {
        _uiState.update { current ->
            val updatedPositions = current.positions.toMutableList()
            updatedPositions[index] = position.uppercaseChar()
            current.copy(positions = updatedPositions, selectedPresetId = null)
        }
        machine = rebuildMachine()
    }

    fun setGreekPosition(position: Char) {
        _uiState.update { it.copy(greekPosition = position.uppercaseChar(), selectedPresetId = null) }
        machine = rebuildMachine()
    }

    fun setRingSetting(index: Int, ring: Int) {
        _uiState.update { current ->
            val updatedRings = current.ringSettings.toMutableList()
            updatedRings[index] = ring
            current.copy(ringSettings = updatedRings, selectedPresetId = null)
        }
        machine = rebuildMachine()
    }

    fun setGreekRing(ring: Int) {
        _uiState.update { it.copy(greekRing = ring, selectedPresetId = null) }
        machine = rebuildMachine()
    }

    fun addPlugboardPair(char1: Char, char2: Char) {
        val c1 = char1.uppercaseChar()
        val c2 = char2.uppercaseChar()
        if (c1 == c2) {
            _uiState.update { it.copy(plugboardErrorMessage = "Cannot connect character to itself: $c1") }
            return
        }

        val currentPairs = _uiState.value.plugboardPairs
        val used = currentPairs.flatMap { listOf(it.first, it.second) }.toSet()
        if (used.contains(c1) || used.contains(c2)) {
            _uiState.update { it.copy(plugboardErrorMessage = "Character $c1 or $c2 is already connected") }
            return
        }

        val newPairs = currentPairs + PlugboardPair(c1, c2)
        _uiState.update {
            it.copy(
                plugboardPairs = newPairs,
                selectedPresetId = null,
                plugboardErrorMessage = null
            )
        }
        machine = rebuildMachine()
    }

    fun removePlugboardPair(pair: PlugboardPair) {
        _uiState.update { current ->
            val updated = current.plugboardPairs.filterNot { it.first == pair.first && it.second == pair.second }
            current.copy(plugboardPairs = updated, selectedPresetId = null, plugboardErrorMessage = null)
        }
        machine = rebuildMachine()
    }

    fun clearPlugboard() {
        _uiState.update { it.copy(plugboardPairs = emptyList(), selectedPresetId = null, plugboardErrorMessage = null) }
        machine = rebuildMachine()
    }

    fun onKeyPress(char: Char) {
        val upper = char.uppercaseChar()
        if (upper !in 'A'..'Z') return

        val trace = machine.processWithTrace(upper)

        // Read back updated rotor positions from engine
        val newGreekPos = machine.greekRotor?.position ?: _uiState.value.greekPosition
        val newPositions = listOf(
            machine.leftRotor.position,
            machine.middleRotor.position,
            machine.rightRotor.position
        )

        _uiState.update { current ->
            current.copy(
                greekPosition = newGreekPos,
                positions = newPositions,
                litLamp = trace.outputChar,
                outputTapeText = current.outputTapeText + trace.outputChar,
                latestTrace = trace
            )
        }
    }

    fun onKeyRelease() {
        _uiState.update { it.copy(litLamp = null) }
    }

    fun clearTape() {
        _uiState.update { it.copy(outputTapeText = "", latestTrace = null) }
    }
}
