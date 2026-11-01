package com.enigma.android.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.enigma.engine.EnigmaMachine
import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.configuration.EnigmaConfiguration
import com.enigma.engine.configuration.RotorConfiguration
import com.enigma.engine.model.Alphabet
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.ReflectorSpec
import com.enigma.engine.model.RotorSpec
import com.enigma.engine.model.SignalPathTrace
import com.enigma.engine.preset.EnigmaPreset
import com.enigma.engine.preset.PresetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The three screens of the app. The ordinal doubles as the [TabRow] index. */
enum class MachineTab {
    MACHINE,
    SETTINGS,
    MESSAGE
}

data class EnigmaUiState(
    val model: EnigmaModel = EnigmaModel.M3,
    val selectedPresetId: String? = PresetCatalog.WEHRMACHT_I.id,
    val activeTab: MachineTab = MachineTab.MACHINE,
    val reflectorSpec: ReflectorSpec = ReflectorCatalog.REFLECTOR_B,
    val greekRotorSpec: RotorSpec = RotorCatalog.ROTOR_BETA,
    val greekPosition: Char = 'A',
    val greekRing: Int = 1,
    val rotorSpecs: List<RotorSpec> = listOf(RotorCatalog.ROTOR_I, RotorCatalog.ROTOR_II, RotorCatalog.ROTOR_III),
    /** Live window letters: they advance with every key press. */
    val positions: List<Char> = listOf('A', 'A', 'A'),
    val ringSettings: List<Int> = listOf(1, 1, 1),
    /** Window letters the machine is reset to (the key as it was set up before use). */
    val startPositions: List<Char> = listOf('A', 'A', 'A'),
    val startGreekPosition: Char = 'A',
    val plugboardPairs: List<PlugboardPair> = emptyList(),
    val litLamp: Char? = null,
    val outputTapeText: String = "",
    val latestTrace: SignalPathTrace? = null,
    val plugboardErrorMessage: String? = null,
    val messageInput: String = "",
    val messageOutput: String = "",
    val messageErrorMessage: String? = null
) {
    /** Window letters as one string, Greek rotor first for the M4. */
    val windowNotation: String
        get() = buildString {
            if (model.requiresThinReflector) append(greekPosition)
            positions.forEach { append(it) }
        }

    val isAtStartPositions: Boolean
        get() = positions == startPositions && (!model.requiresThinReflector || greekPosition == startGreekPosition)

    val canReset: Boolean get() = !isAtStartPositions
}

/**
 * Holds the machine, the reactive UI state and all user intents.
 *
 * The whole key (rotor order, window letters, ring settings, reflector, plugboard) plus the
 * output tape is mirrored into [SavedStateHandle], so a rotated device - or a process that was
 * killed in the background - resumes exactly where the operator left off.
 */
class EnigmaViewModel(
    private val savedState: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnigmaUiState())
    val uiState: StateFlow<EnigmaUiState> = _uiState.asStateFlow()

    private var machine: EnigmaMachine

    init {
        val restored = savedState.get<String>(KEY_LIVE_CONFIGURATION)?.let(::decodeConfiguration)
        val restoredStart = savedState.get<String>(KEY_START_CONFIGURATION)?.let(::decodeConfiguration)
        val restoredTape = savedState.get<String>(KEY_TAPE).orEmpty()

        if (restored != null) {
            machine = EnigmaMachine.fromConfiguration(restored)
            _uiState.value = uiStateOf(restored, restoredStart ?: restored, restoredTape)
        } else {
            val preset = PresetCatalog.WEHRMACHT_I
            machine = EnigmaMachine.fromPreset(preset)
            val configuration = machine.configuration()
            _uiState.value = uiStateOf(configuration, configuration, tape = "")
                .copy(selectedPresetId = preset.id)
        }
    }

    // ------------------------------------------------------------------ navigation

    fun selectTab(tab: MachineTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ------------------------------------------------------------------ model & presets

    fun selectModel(model: EnigmaModel) {
        if (_uiState.value.model == model) return
        _uiState.update { current ->
            val reflector = when {
                model.requiresThinReflector && !current.reflectorSpec.isThin -> ReflectorCatalog.REFLECTOR_B_THIN
                !model.requiresThinReflector && current.reflectorSpec.isThin -> ReflectorCatalog.REFLECTOR_B
                else -> current.reflectorSpec
            }
            current.copy(
                model = model,
                selectedPresetId = null,
                reflectorSpec = reflector,
                startPositions = current.positions,
                startGreekPosition = current.greekPosition,
                litLamp = null,
                latestTrace = null
            )
        }
        rebuildMachine()
        persist()
    }

    fun applyPreset(preset: EnigmaPreset) {
        machine = EnigmaMachine.fromPreset(preset)
        val configuration = machine.configuration()
        _uiState.update { current ->
            current.copy(
                model = configuration.model,
                selectedPresetId = preset.id,
                reflectorSpec = ReflectorCatalog.getById(configuration.reflectorId),
                greekRotorSpec = configuration.greekRotor?.let { RotorCatalog.getById(it.specId) }
                    ?: current.greekRotorSpec,
                greekPosition = configuration.greekRotor?.normalizedPosition ?: current.greekPosition,
                greekRing = configuration.greekRotor?.ringSetting ?: current.greekRing,
                rotorSpecs = configuration.rotors.map { RotorCatalog.getById(it.specId) },
                positions = configuration.rotors.map { it.normalizedPosition },
                ringSettings = configuration.rotors.map { it.ringSetting },
                startPositions = configuration.rotors.map { it.normalizedPosition },
                startGreekPosition = configuration.greekRotor?.normalizedPosition ?: current.startGreekPosition,
                plugboardPairs = configuration.plugboard,
                litLamp = null,
                latestTrace = null,
                plugboardErrorMessage = null,
                messageOutput = "",
                messageErrorMessage = null
            )
        }
        persist()
    }

    // ------------------------------------------------------------------ hardware

    fun setReflector(reflectorSpec: ReflectorSpec) {
        // A thin reflector only fits the M4 and vice versa; ignore mismatched requests.
        if (reflectorSpec.isThin != _uiState.value.model.requiresThinReflector) return
        _uiState.update { it.copy(reflectorSpec = reflectorSpec, selectedPresetId = null) }
        rebuildMachine()
        persist()
    }

    fun setRotorSpec(index: Int, spec: RotorSpec) {
        if (spec.isThin) return // Beta/Gamma only fit the fourth slot
        _uiState.update { current ->
            val updatedSpecs = current.rotorSpecs.toMutableList()
            updatedSpecs[index] = spec
            current.copy(rotorSpecs = updatedSpecs, selectedPresetId = null)
        }
        rebuildMachine()
        persist()
    }

    fun setGreekRotorSpec(spec: RotorSpec) {
        if (!spec.isThin) return // the fourth slot only takes Beta or Gamma
        _uiState.update { it.copy(greekRotorSpec = spec, selectedPresetId = null) }
        rebuildMachine()
        persist()
    }

    fun setRotorPosition(index: Int, position: Char) {
        _uiState.update { current ->
            val updatedPositions = current.positions.toMutableList()
            updatedPositions[index] = position.uppercaseChar()
            val updatedStart = current.startPositions.toMutableList()
            updatedStart[index] = position.uppercaseChar()
            current.copy(
                positions = updatedPositions,
                startPositions = updatedStart,
                selectedPresetId = null,
                litLamp = null,
                latestTrace = null
            )
        }
        rebuildMachine()
        persist()
    }

    fun setGreekPosition(position: Char) {
        _uiState.update {
            it.copy(
                greekPosition = position.uppercaseChar(),
                startGreekPosition = position.uppercaseChar(),
                selectedPresetId = null,
                litLamp = null,
                latestTrace = null
            )
        }
        rebuildMachine()
        persist()
    }

    fun setRingSetting(index: Int, ring: Int) {
        _uiState.update { current ->
            val updatedRings = current.ringSettings.toMutableList()
            updatedRings[index] = ring
            current.copy(ringSettings = updatedRings, selectedPresetId = null)
        }
        rebuildMachine()
        persist()
    }

    fun setGreekRing(ring: Int) {
        _uiState.update { it.copy(greekRing = ring, selectedPresetId = null) }
        rebuildMachine()
        persist()
    }

    /** Brings every wheel back to the window letters of the current key. */
    fun resetPositions() {
        _uiState.update {
            it.copy(
                positions = it.startPositions,
                greekPosition = it.startGreekPosition,
                litLamp = null,
                latestTrace = null
            )
        }
        rebuildMachine()
        persist()
    }

    // ------------------------------------------------------------------ plugboard

    fun addPlugboardPair(char1: Char, char2: Char) {
        val c1 = char1.uppercaseChar()
        val c2 = char2.uppercaseChar()
        if (c1 !in 'A'..'Z' || c2 !in 'A'..'Z') {
            _uiState.update { it.copy(plugboardErrorMessage = "Only letters A-Z can be connected") }
            return
        }
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
        if (currentPairs.size >= Plugboard.MAX_PAIRS) {
            _uiState.update {
                it.copy(plugboardErrorMessage = "All ${Plugboard.MAX_PAIRS} cables are in use")
            }
            return
        }

        _uiState.update {
            it.copy(
                plugboardPairs = currentPairs + PlugboardPair(c1, c2),
                selectedPresetId = null,
                plugboardErrorMessage = null,
                litLamp = null,
                latestTrace = null
            )
        }
        rebuildMachine()
        persist()
    }

    fun removePlugboardPair(pair: PlugboardPair) {
        _uiState.update { current ->
            current.copy(
                plugboardPairs = current.plugboardPairs.filterNot {
                    it.first == pair.first && it.second == pair.second
                },
                selectedPresetId = null,
                plugboardErrorMessage = null
            )
        }
        rebuildMachine()
        persist()
    }

    fun clearPlugboard() {
        _uiState.update {
            it.copy(plugboardPairs = emptyList(), selectedPresetId = null, plugboardErrorMessage = null)
        }
        rebuildMachine()
        persist()
    }

    // ------------------------------------------------------------------ typing

    fun onKeyPress(char: Char) {
        val upper = char.uppercaseChar()
        if (!Alphabet.isLetter(upper)) return

        val trace = machine.processWithTrace(upper)

        _uiState.update { current ->
            current.copy(
                greekPosition = machine.greekRotor?.position ?: current.greekPosition,
                positions = listOf(
                    machine.leftRotor.position,
                    machine.middleRotor.position,
                    machine.rightRotor.position
                ),
                litLamp = trace.outputChar,
                outputTapeText = (current.outputTapeText + trace.outputChar).takeLast(MAX_TAPE_LENGTH),
                latestTrace = trace
            )
        }
        persist()
    }

    fun onKeyRelease() {
        _uiState.update { it.copy(litLamp = null) }
    }

    fun clearTape() {
        _uiState.update { it.copy(outputTapeText = "", latestTrace = null) }
        persist()
    }

    // ------------------------------------------------------------------ message console

    fun setMessageInput(text: String) {
        _uiState.update {
            it.copy(
                messageInput = text.uppercase().take(MAX_MESSAGE_LENGTH),
                messageErrorMessage = null
            )
        }
    }

    /**
     * Runs a whole message through the machine. Enigma is reciprocal, so the same action
     * encrypts plain text and decrypts cipher text; the rotors advance while doing so.
     */
    fun processMessage() {
        val normalized = Alphabet.normalize(_uiState.value.messageInput)
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(messageOutput = "", messageErrorMessage = "Type at least one letter (A-Z).")
            }
            return
        }

        val result = StringBuilder(normalized.length)
        for (char in normalized) {
            result.append(machine.encrypt(char))
        }

        _uiState.update {
            it.copy(
                messageOutput = result.toString(),
                messageErrorMessage = null,
                positions = listOf(
                    machine.leftRotor.position,
                    machine.middleRotor.position,
                    machine.rightRotor.position
                ),
                greekPosition = machine.greekRotor?.position ?: it.greekPosition,
                litLamp = null,
                latestTrace = null
            )
        }
        persist()
    }

    fun clearMessage() {
        _uiState.update { it.copy(messageInput = "", messageOutput = "", messageErrorMessage = null) }
    }

    // ------------------------------------------------------------------ persistence

    private fun rebuildMachine() {
        machine = EnigmaMachine.fromConfiguration(configurationOf(_uiState.value))
    }

    private fun configurationOf(state: EnigmaUiState, useStartPositions: Boolean = false): EnigmaConfiguration {
        val positions = if (useStartPositions) state.startPositions else state.positions
        val greekPosition = if (useStartPositions) state.startGreekPosition else state.greekPosition
        return EnigmaConfiguration(
            model = state.model,
            reflectorId = state.reflectorSpec.id,
            rotors = listOf(
                RotorConfiguration(state.rotorSpecs[0].id, positions[0], state.ringSettings[0]),
                RotorConfiguration(state.rotorSpecs[1].id, positions[1], state.ringSettings[1]),
                RotorConfiguration(state.rotorSpecs[2].id, positions[2], state.ringSettings[2])
            ),
            greekRotor = if (state.model.requiresThinReflector) {
                RotorConfiguration(state.greekRotorSpec.id, greekPosition, state.greekRing)
            } else {
                null
            },
            plugboard = state.plugboardPairs
        )
    }

    private fun uiStateOf(
        live: EnigmaConfiguration,
        start: EnigmaConfiguration,
        tape: String
    ): EnigmaUiState = EnigmaUiState(
        model = live.model,
        selectedPresetId = null,
        reflectorSpec = ReflectorCatalog.getById(live.reflectorId),
        greekRotorSpec = live.greekRotor?.let { RotorCatalog.getById(it.specId) } ?: RotorCatalog.ROTOR_BETA,
        greekPosition = live.greekRotor?.normalizedPosition ?: 'A',
        greekRing = live.greekRotor?.ringSetting ?: 1,
        rotorSpecs = live.rotors.map { RotorCatalog.getById(it.specId) },
        positions = live.rotors.map { it.normalizedPosition },
        ringSettings = live.rotors.map { it.ringSetting },
        startPositions = start.rotors.map { it.normalizedPosition },
        startGreekPosition = start.greekRotor?.normalizedPosition ?: 'A',
        plugboardPairs = live.plugboard,
        outputTapeText = tape
    )

    private fun persist() {
        // Never let a corrupt/unsupported state crash the operator: saving is best effort.
        runCatching {
            val state = _uiState.value
            savedState[KEY_LIVE_CONFIGURATION] = configurationOf(state).encode()
            savedState[KEY_START_CONFIGURATION] = configurationOf(state, useStartPositions = true).encode()
            savedState[KEY_TAPE] = state.outputTapeText
        }
    }

    private fun decodeConfiguration(value: String): EnigmaConfiguration? =
        runCatching { EnigmaConfiguration.decode(value) }.getOrNull()

    companion object {
        private const val KEY_LIVE_CONFIGURATION = "enigma.liveConfiguration"
        private const val KEY_START_CONFIGURATION = "enigma.startConfiguration"
        private const val KEY_TAPE = "enigma.tape"
        private const val MAX_TAPE_LENGTH = 4000
        private const val MAX_MESSAGE_LENGTH = 2000
    }
}
