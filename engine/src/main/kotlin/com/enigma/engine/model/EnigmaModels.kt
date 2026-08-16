package com.enigma.engine.model

object Alphabet {
    const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val SIZE = 26

    fun toIndex(char: Char): Int {
        val upper = char.uppercaseChar()
        require(upper in 'A'..'Z') { "Character must be an uppercase English letter (A-Z), got '$char'" }
        return upper - 'A'
    }

    fun toChar(index: Int): Char {
        val normalized = Math.floorMod(index, SIZE)
        return 'A' + normalized
    }
}

enum class EnigmaModel(val displayName: String, val hasFourthRotor: Boolean) {
    M3("Enigma M3 (3-Rotor)", false),
    M4("Enigma M4 (4-Rotor / U-Boat)", true)
}

data class PlugboardPair(val char1: Char, val char2: Char) {
    init {
        val c1 = char1.uppercaseChar()
        val c2 = char2.uppercaseChar()
        require(c1 in 'A'..'Z' && c2 in 'A'..'Z') { "Plugboard characters must be A-Z" }
        require(c1 != c2) { "Plugboard cannot connect a character to itself: $char1" }
    }

    val first: Char = char1.uppercaseChar()
    val second: Char = char2.uppercaseChar()

    fun swaps(char: Char): Boolean = char.uppercaseChar() == first || char.uppercaseChar() == second

    fun swap(char: Char): Char {
        val upper = char.uppercaseChar()
        return when (upper) {
            first -> second
            second -> first
            else -> upper
        }
    }
}

class Plugboard(pairs: List<PlugboardPair> = emptyList()) {
    private val map = CharArray(26) { 'A' + it }

    init {
        val used = mutableSetOf<Char>()
        for (pair in pairs) {
            require(!used.contains(pair.first) && !used.contains(pair.second)) {
                "Duplicate character in plugboard configuration: ${pair.first} or ${pair.second}"
            }
            used.add(pair.first)
            used.add(pair.second)
            map[pair.first - 'A'] = pair.second
            map[pair.second - 'A'] = pair.first
        }
    }

    fun process(char: Char): Char {
        val upper = char.uppercaseChar()
        if (upper !in 'A'..'Z') return upper
        return map[upper - 'A']
    }

    val pairsList: List<PlugboardPair>
        get() {
            val list = mutableListOf<PlugboardPair>()
            for (i in 0 until 26) {
                val targetIndex = map[i] - 'A'
                if (targetIndex > i) {
                    list.add(PlugboardPair('A' + i, 'A' + targetIndex))
                }
            }
            return list
        }
}

data class RotorSpec(
    val id: String,
    val name: String,
    val wiring: String,
    val turnoverNotches: Set<Char>,
    val isThin: Boolean = false
)

class Rotor(
    val spec: RotorSpec,
    position: Char = 'A',
    ringSetting: Int = 1 // 1..26 (A=1)
) {
    var position: Char = position.uppercaseChar()
        set(value) {
            require(value.uppercaseChar() in 'A'..'Z')
            field = value.uppercaseChar()
        }

    var ringSetting: Int = ringSetting
        set(value) {
            require(value in 1..26) { "Ring setting must be between 1 and 26" }
            field = value
        }

    val positionIndex: Int get() = Alphabet.toIndex(position)
    val ringOffset: Int get() = ringSetting - 1

    fun isAtNotch(): Boolean = spec.turnoverNotches.contains(position)

    fun step(): Char {
        position = Alphabet.toChar(positionIndex + 1)
        return position
    }

    fun forward(inputIndex: Int): Int {
        val shift = Math.floorMod(positionIndex - ringOffset, 26)
        val enteredWiringIndex = Math.floorMod(inputIndex + shift, 26)
        val wiredChar = spec.wiring[enteredWiringIndex]
        val wiredIndex = Alphabet.toIndex(wiredChar)
        return Math.floorMod(wiredIndex - shift, 26)
    }

    fun backward(inputIndex: Int): Int {
        val shift = Math.floorMod(positionIndex - ringOffset, 26)
        val enteredWiringIndex = Math.floorMod(inputIndex + shift, 26)
        val wiredChar = Alphabet.toChar(enteredWiringIndex)
        val originalWiringIndex = spec.wiring.indexOf(wiredChar)
        return Math.floorMod(originalWiringIndex - shift, 26)
    }

    fun copy(
        position: Char = this.position,
        ringSetting: Int = this.ringSetting
    ): Rotor = Rotor(spec, position, ringSetting)
}

data class ReflectorSpec(
    val id: String,
    val name: String,
    val wiring: String,
    val isThin: Boolean = false
)

class Reflector(val spec: ReflectorSpec) {
    fun reflect(inputIndex: Int): Int {
        val reflectedChar = spec.wiring[inputIndex]
        return Alphabet.toIndex(reflectedChar)
    }
}

data class SignalStep(
    val stage: String,
    val character: Char,
    val index: Int,
    val description: String
)

data class SignalPathTrace(
    val inputChar: Char,
    val outputChar: Char,
    val steps: List<SignalStep>
)
