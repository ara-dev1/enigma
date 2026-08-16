# Enigma Machine Simulator (Android & Pure Kotlin Engine)

An Android application and pure Kotlin library simulating the historical WWII Enigma cipher machines, including **Enigma M3** (3-rotor) and **Enigma M4** (4-rotor U-boat edition).

Designed with a clean **multi-module architecture** separating core domain logic from the Jetpack Compose user interface.

---

## 🏛️ Project Architecture

The project is divided into two distinct Gradle modules:

* **`:engine`** — A pure Kotlin/JVM module containing no Android framework dependencies.
  * Core cipher algorithms and rotor stepping logic
  * Mechanical double-stepping anomaly implementation
  * Support for Enigma M3 and M4 models
  * Catalogs for standard rotors (I-VIII) and M4 Greek rotors (Beta, Gamma)
  * Standard reflectors (A, B, C) and thin reflectors (B Thin, C Thin)
  * Steckerbrett (Plugboard) pair mapping and validation
  * Step-by-step electrical signal path tracing
  * Historical presets (Wehrmacht I, Kriegsmarine M3, U-Boat M4, Barbarossa, U-505)
  * Comprehensive JUnit unit tests

* **`:app`** — Android application module built with **Jetpack Compose** and **Material 3**.
  * ViewModels with `StateFlow` reactive state management
  * Interactive QWERTZ Enigma Keyboard with press/release feedback
  * Illuminated Lampboard output visualizer
  * Rotor position dials with increment/decrement controls
  * Steckerbrett (Plugboard) visual manager with real-time pair setup
  * Signal Path Inspector displaying step-by-step electrical current routing
  * Output Tape with copy-to-clipboard and 5-letter grouping format

---

## 🚀 Building & Running

### Prerequisites
* Java JDK 17
* Android SDK 34 (Android 8.0 / API 26 minimum)

### Build Engine Module
```bash
./gradlew :engine:build
```

### Run Engine Tests
```bash
./gradlew :engine:test
```

### Build Android App
```bash
./gradlew :app:assembleDebug
```

---

## 📂 Project Structure

```
├── app/                      # Android Application Module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/enigma/android/
│       │   ├── MainActivity.kt
│       │   ├── ui/components/EnigmaUiComponents.kt
│       │   ├── ui/theme/Theme.kt
│       │   └── viewmodel/EnigmaViewModel.kt
│       └── res/
├── engine/                   # Pure Kotlin Cipher Engine
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/com/enigma/engine/
│       │   ├── EnigmaMachine.kt
│       │   ├── catalog/Catalogs.kt
│       │   ├── model/EnigmaModels.kt
│       │   └── preset/Presets.kt
│       └── test/kotlin/com/enigma/engine/
│           └── EnigmaEngineTest.kt
├── build.gradle.kts          # Root Gradle script
├── settings.gradle.kts       # Multi-module settings
├── gradle.properties
├── gradlew
└── README.md
```

---

## 📜 Historical Cipher Features

### Double-Stepping Anomaly
Enigma's mechanical ratchet system causes the middle rotor to step twice in succession when it reaches its notch position. The `:engine` module models this mechanical anomaly:
1. **Right Rotor** always steps on every key press.
2. **Middle Rotor** steps if the right rotor is at its turnover notch **OR** if the middle rotor itself is at its notch (the double step).
3. **Left Rotor** steps when the middle rotor advances out of its notch position.

---

## 📄 License
This project is open-source and available under the MIT License.
