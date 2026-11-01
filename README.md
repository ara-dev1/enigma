# Enigma Machine Simulator (Android & Pure Kotlin Engine)

An Android application and a pure Kotlin library simulating the historical WWII Enigma cipher
machines: **Enigma M3** (three rotors) and **Enigma M4** (four rotor U-boat edition).

The project is split into a dependency free domain module and a Jetpack Compose UI, and its
cipher is verified against published, independently broken wartime messages.

```
┌──────────────────────────┐        ┌───────────────────────────────┐
│ :app  (Android/Compose)  │ ─────▶ │ :engine  (pure Kotlin, no UI) │
│ ViewModel · StateFlow    │        │ rotors · stepping · plugboard │
│ keyboard · lampboard     │        │ traces · presets · snapshots  │
└──────────────────────────┘        └───────────────────────────────┘
```

---

## Features

### Cipher engine (`:engine`, no Android dependencies)

* Enigma **M3** and **M4** including the thin Beta/Gamma wheels and thin reflectors B/C.
* The mechanical **double-stepping** anomaly of the middle rotor, implemented from the pawl
  geometry rather than from a lookup table.
* Rotor catalogue I-VIII with the correct **two notches (M and Z)** on the Kriegsmarine wheels
  VI-VIII, plus the standard reflectors A/B/C.
* **Steckerbrett** (plugboard) with historical limits (13 cables, one letter per cable).
* **Ringstellung** handling: the ring rotates the wiring core against the alphabet ring.
* Step-by-step **signal path tracing** for teaching and debugging.
* **Serialisable key snapshots** (`EnigmaConfiguration`) for saving, restoring or sharing a key.
* Self-checking catalogue: wirings must be permutations, reflectors must be reciprocal without
  fixed points, so a typo fails immediately instead of silently producing a wrong cipher.

### App (`:app`, Jetpack Compose + Material 3)

* Interactive **QWERTZ keyboard** with press feedback and an illuminated **lampboard**.
* Rotor dials with wheel selection, window letter and ring setting.
* Plugboard manager with live validation.
* **Output tape** with historical five letter grouping and copy to clipboard.
* **Signal path inspector** that animates the current through every stage.
* **Message console**: paste a whole message, process it in one go, copy the result.
* **Reset** button that rewinds every wheel to the key start position.
* Hardware keyboard support on the machine screen.
* Full state persistence: the key and tape survive rotation and process death.
* Seven historical presets, including a published broken key (Scharnhorst, 1943).

---

## Correctness: known-answer tests

The engine is pinned against messages that were decrypted historically, not just against
self-consistency checks. `engine/src/test/kotlin/com/enigma/engine/KnownAnswerVectorsTest.kt`
covers:

| Vector | What it proves |
| --- | --- |
| `AAAAA -> BDZGO` (Wikipedia, rotors I-II-III, Reflector B) | basic wiring and stepping |
| `KDO -> KDP, KDQ, KER, LFS, LFT, LFU` | double-stepping sequence |
| Rijmenants manual, M3, rings `B U L`, 10 cables | ring settings and full plugboard |
| Scharnhorst break, M3, rings `A H M` | Kriegsmarine double notches (VI-VIII) |
| U-boat M4 message, Beta wheel, thin Reflector B | thin reflector + Greek rotor |
| Rasch message, M4 with rotor VI | navy wheel inside an M4 |
| Generated regression vectors | thin Reflector C and Reflector A |

`CatalogIntegrityTest` additionally asserts every wiring table digit by digit, which is what
catches the classic transcription errors (a truncated Reflector A, a wrong thin reflector, or
notches at A/N instead of M/Z).

---

## Building & running

### Prerequisites

* JDK 17
* Android SDK 34 (minimum device level: API 26 / Android 8.0)

```bash
./gradlew :engine:test        # cipher test suite (fast, no Android SDK needed)
./gradlew :app:testDebugUnitTest   # ViewModel tests
./gradlew :app:assembleDebug       # debug APK
```

The `:engine` module is a plain Kotlin/JVM library, so it can be consumed by any JVM project:

```kotlin
val machine = EnigmaMachine.fromPreset(PresetCatalog.SCHARNHORST_1943)
val cipher = machine.processText("ATTACK AT DAWN")

val snapshot: String = machine.configuration().encode()   // save the key
val restored = EnigmaMachine.fromConfiguration(EnigmaConfiguration.decode(snapshot))
```

---

## Project structure

```
├── app/                                   # Android application module (Compose, Material 3)
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/enigma/android/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/components/EnigmaUiComponents.kt
│       │   │   ├── ui/theme/Theme.kt
│       │   │   └── viewmodel/EnigmaViewModel.kt
│       │   └── res/
│       └── test/kotlin/com/enigma/android/viewmodel/EnigmaViewModelTest.kt
├── engine/                                # Pure Kotlin/JVM cipher engine
│   └── src/
│       ├── main/kotlin/com/enigma/engine/
│       │   ├── EnigmaMachine.kt
│       │   ├── catalog/Catalogs.kt
│       │   ├── configuration/EnigmaConfiguration.kt
│       │   ├── model/EnigmaModels.kt
│       │   └── preset/Presets.kt
│       └── test/kotlin/com/enigma/engine/
│           ├── CatalogIntegrityTest.kt
│           ├── ConfigurationTest.kt
│           ├── EnigmaEngineTest.kt
│           ├── KnownAnswerVectorsTest.kt
│           ├── PlugboardAndPresetTest.kt
│           ├── RotorMotionTest.kt
│           └── TestMachines.kt
├── gradle/libs.versions.toml              # version catalog
├── .github/workflows/ci.yml               # engine tests + APK build
├── CHANGELOG.md
└── LICENSE
```

---

## Historical notes

### Double-stepping

Enigma's ratchet system makes the middle rotor step twice in a row when it sits on its own
notch. `EnigmaMachine.step()` models the mechanism directly:

1. the **right rotor** always steps;
2. the **middle rotor** steps if the right rotor is at its notch **or** if the middle rotor
   itself is at its notch (the double step);
3. the **left rotor** steps when the middle rotor leaves its notch;
4. the fourth (Greek) rotor of the M4 never steps while typing.

### Ring settings and notches

The turnover notch is machined into the *alphabet ring*, together with the letters seen in the
window. Changing the Ringstellung therefore rotates the wiring core against the ring - it shifts
the cipher and the internal position - but it does **not** move the window letter at which
turnover happens. Adjusting the notch test by the ring setting is a well known trap that makes
every decryption of real traffic fail; `RotorMotionTest` locks the correct behaviour in place.

---

## License

MIT - see [LICENSE](LICENSE). Change history is documented in [CHANGELOG.md](CHANGELOG.md).
