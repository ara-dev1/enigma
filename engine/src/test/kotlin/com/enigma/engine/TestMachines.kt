package com.enigma.engine

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Plugboard
import com.enigma.engine.model.PlugboardPair
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.Rotor

/**
 * Small factory used by the tests to describe historical key sheets in the notation that is
 * used by the literature, e.g. `m3(rotors = "II IV V", rings = "BUL", plugboard = "AV BS")`.
 */
internal object TestMachines {

    /** Ringstellen written as letters: A = 1, B = 2, ... Z = 26. */
    fun ringOf(letter: Char): Int = letter.uppercaseChar() - 'A' + 1

    fun plugboardOf(notation: String): Plugboard {
        val tokens = notation.split(" ", ",").filter { it.isNotBlank() }
        return Plugboard(tokens.map { token ->
            require(token.length == 2) { "Malformed plugboard pair '$token'" }
            PlugboardPair(token[0], token[1])
        })
    }

    fun m3(
        rotors: String = "I II III",
        reflectorId: String = "B",
        positions: String = "AAA",
        rings: String = "AAA",
        plugboard: String = ""
    ): EnigmaMachine {
        val specs = rotors.trim().split(Regex("\\s+")).map { RotorCatalog.getById(it) }
        require(specs.size == 3) { "An M3 needs three rotors, got ${specs.size}" }
        require(positions.length == 3 && rings.length == 3) { "M3 positions/rings must have three letters" }
        return EnigmaMachine(
            model = EnigmaModel.M3,
            reflector = Reflector(ReflectorCatalog.getById(reflectorId)),
            leftRotor = Rotor(specs[0], positions[0], ringOf(rings[0])),
            middleRotor = Rotor(specs[1], positions[1], ringOf(rings[1])),
            rightRotor = Rotor(specs[2], positions[2], ringOf(rings[2])),
            plugboard = plugboardOf(plugboard)
        )
    }

    /** Greek rotor first, then left, middle and right. */
    fun m4(
        greek: String = "BETA",
        rotors: String = "I II III",
        reflectorId: String = "B_THIN",
        positions: String = "AAAA",
        rings: String = "AAAA",
        plugboard: String = ""
    ): EnigmaMachine {
        val specs = rotors.trim().split(Regex("\\s+")).map { RotorCatalog.getById(it) }
        require(specs.size == 3) { "An M4 needs three main rotors, got ${specs.size}" }
        require(positions.length == 4 && rings.length == 4) {
            "M4 positions/rings must have four letters (Greek rotor first)"
        }
        return EnigmaMachine(
            model = EnigmaModel.M4,
            reflector = Reflector(ReflectorCatalog.getById(reflectorId)),
            greekRotor = Rotor(RotorCatalog.getById(greek), positions[0], ringOf(rings[0])),
            leftRotor = Rotor(specs[0], positions[1], ringOf(rings[1])),
            middleRotor = Rotor(specs[1], positions[2], ringOf(rings[2])),
            rightRotor = Rotor(specs[2], positions[3], ringOf(rings[3])),
            plugboard = plugboardOf(plugboard)
        )
    }
}
