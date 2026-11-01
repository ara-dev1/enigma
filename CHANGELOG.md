# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- **Reflector A wiring** was truncated to 25 letters, so selecting Reflector A crashed with an
  `IndexOutOfBoundsException` (or produced garbage). The correct historical table
  `EJMZALYXVBWFCRQUONTSPIKHGD` is now used.
- **Thin Reflector B** used `ENKQAMZYXVCWFBPODTURSJLIHG` instead of the historical
  `ENKQAUYWJICOPBLMDXZVFTHRGS`; every M4 cipher text was therefore wrong.
- **Thin Reflector C** used `RDOBJNTKVEHMLFCZSAQGWIUYXP` instead of
  `RDOBJNTKVEHMLFCWZAXGYIPSUQ`.
- **Kriegsmarine rotors VI-VIII** turned over at A and N instead of **M and Z**, which broke
  every navy key that used them.
- `EnigmaMachine.applyPreset` could silently turn an M4 into a three rotor machine by dropping
  the Greek rotor. Mismatched presets now fail with a clear message.
- `Theme.Enigma` no longer flashes a white window before the first Compose frame.

### Added

- Self-validating catalog specs: wirings must be permutations, reflectors must be reciprocal
  and free of fixed points, so a typo cannot produce a silently wrong machine again.
- Known-answer test suite with published traffic (Wikipedia example, Rijmenants manual,
  Scharnhorst break, U-boat M4 messages, Rasch message) plus thin-reflector and Reflector A
  regression vectors.
- `EnigmaConfiguration`: serialisable snapshot of a complete key (rotor order, window letters,
  ring settings, reflector, Greek rotor, plugboard) with `encode()` / `decode()`.
- `EnigmaMachine.fromConfiguration`, `configuration()`, `restorePositions()` and
  `parsePlugboard()`.
- Message console: paste a whole message and process it in one go, with five letter grouping and
  copy to clipboard.
- "Reset" action that rewinds all wheels to the key start position.
- Hardware keyboard support on the machine screen.
- Full state persistence through `SavedStateHandle`: the key and the output tape survive
  rotation and process death.
- App level unit tests for the ViewModel.
- `LICENSE` (MIT), CI workflow, ProGuard rules and `.editorconfig`.
- `Scharnhorst (1943)` preset that reproduces a published, fully broken key.

### Changed

- Presets carry validated metadata and are rendered in historical notation; the U-505 preset is
  now labelled as a teaching example instead of a transcribed document.
- Engine code is documented with the mechanical reasoning behind stepping and ring settings.

## [1.0.0] - 2024-01-01

### Added

- Initial public release: Enigma M3/M4 engine with double-stepping, plugboard, signal path
  tracing and a Jetpack Compose user interface.
