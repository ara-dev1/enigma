package com.enigma.engine.model

object Alphabet {
    const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val SIZE = 26

    fun isLetter(char: Char): Boolean = char.uppercaseChar() in 'A'..'Z'

    fun toIndex(char: Char): Int {
        val upper = char.uppercaseChar()
        require(upper in 'A'..'Z') { "Character must be an uppercase English letter (A-Z), got '$char'" }
        return upper - 'A'
    }

    fun toChar(index: Int): Char {
        val normalized = Math.floorMod(index, SIZE)
        return 'A' + normalized
    }

    /** Keeps only A-Z characters and converts them to upper case. */
    fun normalize(text: String): String = buildString(text.length) {
        for (char in text) {
            if (isLetter(char)) append(char.uppercaseChar())
        }
    }

    /** Formats text in the historical five letter groups, e.g. `ENIGM AABCD`. */
    fun groupInFives(text: String, groupSize: Int = 5): String =
        if (text.isEmpty()) "" else text.chunked(groupSize).joinToString(" ")
}

enum class EnigmaModel(val displayName: String, val hasFourthRotor: Boolean) {
    M3("Enigma M3 (3-Rotor)", false),
    M4("Enigma M4 (4-Rotor / U-Boat)", true);

    /** Reflector thickness required by the model: the M4 only works with thin reflectors. */
    val requiresThinReflector: Boolean get() = hasFourthRotor

    companion object {
        fun fromId(id: String): EnigmaModel =
            values().firstOrNull { it.name.equals(id, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown Enigma model id: $id")
    }
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

    /** Two letter form used by configuration snapshots, e.g. `AV`. */
    val notation: String get() = "$first$second"

    override fun toString(): String = notation
}

class Plugboard(pairs: List<PlugboardPair> = emptyList()) {
    private val map = CharArray(Alphabet.SIZE) { 'A' + it }

    init {
        require(pairs.size <= MAX_PAIRS) {
            "A plugboard supports at most $MAX_PAIRS cable pairs, got ${pairs.size}"
        }
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

    /** Passes a whole string through the plugboard, leaving non-letters untouched. */
    fun process(text: String): String = buildString(text.length) {
        for (char in text) append(process(char))
    }

    fun isPlugged(char: Char): Boolean {
        val upper = char.uppercaseChar()
        return upper in 'A'..'Z' && map[upper - 'A'] != upper
    }

    val pairsList: List<PlugboardPair>
        get() {
            val list = mutableListOf<PlugboardPair>()
            for (i in 0 until Alphabet.SIZE) {
                val targetIndex = map[i] - 'A'
                if (targetIndex > i) {
                    list.add(PlugboardPair('A' + i, 'A' + targetIndex))
                }
            }
            return list
        }

    val size: Int get() = pairsList.size

    val isEmpty: Boolean get() = size == 0

    override fun toString(): String = pairsList.joinToString(" ") { it.notation }

    companion object {
        /** The historical Steckerbrett allowed 13 cables at most. */
        const val MAX_PAIRS = 13
    }
}

/** Shared validation for wiring tables, so a typo can never silently break the cipher. */
internal object WiringValidator {

    fun requirePermutation(component: String, wiring: String) {
        require(wiring.length == Alphabet.SIZE) {
            "$component wiring must contain exactly ${Alphabet.SIZE} letters, got ${wiring.length} ('$wiring')"
        }
        require(wiring.all { it in 'A'..'Z' }) {
            "$component wiring must contain upper case letters A-Z only, got '$wiring'"
        }
        require(wiring.toSet().size == Alphabet.SIZE) {
            "$component wiring must use every letter exactly once, got '$wiring'"
        }
    }
}

data class RotorSpec(
    val id: String,
    val name: String,
    val wiring: String,
    val turnoverNotches: Set<Char>,
    val isThin: Boolean = false
) {
    init {
        WiringValidator.requirePermutation("Rotor '$id'", wiring)
        require(turnoverNotches.all { it in 'A'..'Z' }) {
            "Rotor '$id' notches must be A-Z letters, got $turnoverNotches"
        }
    }

    val notchLetters: String get() = turnoverNotches.sorted().joinToString("")
}

class Rotor(
    val spec: RotorSpec,
    position: Char = 'A',
    ringSetting: Int = 1 // 1..26 (A=1)
) {
    var position: Char = position.uppercaseChar()
        set(value) {
            require(value.uppercaseChar() in 'A'..'Z') { "Rotor position must be A-Z, got '$value'" }
            field = value.uppercaseChar()
        }

    var ringSetting: Int = ringSetting
        set(value) {
            require(value in 1..Alphabet.SIZE) { "Ring setting must be between 1 and 26, got $value" }
            field = value
        }

    val positionIndex: Int get() = Alphabet.toIndex(position)
    val ringOffset: Int get() = ringSetting - 1
    val isThin: Boolean get() = spec.isThin

    /**
     * Returns true when the turnover notch is aligned with the stepping pawl, i.e. when the
     * letter shown in the operator window is one of the rotor's notch letters.
     *
     * The notch is machined into the *alphabet ring*, together with the letters that the
     * operator sees through the window. Turning the ring (Ringstellung) rotates the wiring
     * core against the ring, so it changes the internal position - and the cipher - but it
     * does **not** change the window letter at which turnover happens.
     *
     * Do not add [ringSetting] here: that is a well known trap. Shifting the notch by the ring
     * setting yields ciphers that no longer match the historical messages, which the
     * known-answer tests (Scharnhorst, U-boat M4, ...) guard against.
     */
    fun isAtNotch(): Boolean = spec.turnoverNotches.contains(position)

    /** Steps the rotor by one position and returns the new window letter. */
    fun step(): Char {
        position = Alphabet.toChar(positionIndex + 1)
        return position
    }

    /** Rotates the rotor by [steps] positions (negative values rotate backwards). */
    fun rotate(steps: Int): Char {
        position = Alphabet.toChar(positionIndex + steps)
        return position
    }

    fun forward(inputIndex: Int): Int {
        val shift = Math.floorMod(positionIndex - ringOffset, Alphabet.SIZE)
        val enteredWiringIndex = Math.floorMod(inputIndex + shift, Alphabet.SIZE)
        val wiredChar = spec.wiring[enteredWiringIndex]
        val wiredIndex = Alphabet.toIndex(wiredChar)
        return Math.floorMod(wiredIndex - shift, Alphabet.SIZE)
    }

    fun backward(inputIndex: Int): Int {
        val shift = Math.floorMod(positionIndex - ringOffset, Alphabet.SIZE)
        val enteredWiringIndex = Math.floorMod(inputIndex + shift, Alphabet.SIZE)
        val wiredChar = Alphabet.toChar(enteredWiringIndex)
        val originalWiringIndex = spec.wiring.indexOf(wiredChar)
        return Math.floorMod(originalWiringIndex - shift, Alphabet.SIZE)
    }

    fun copy(
        position: Char = this.position,
        ringSetting: Int = this.ringSetting
    ): Rotor = Rotor(spec, position, ringSetting)

    override fun toString(): String = "${spec.id}@$position/$ringSetting"
}

data class ReflectorSpec(
    val id: String,
    val name: String,
    val wiring: String,
    val isThin: Boolean = false
) {
    init {
        WiringValidator.requirePermutation("Reflector '$id'", wiring)
        for (index in 0 until Alphabet.SIZE) {
            val letter = Alphabet.toChar(index)
            val target = Alphabet.toIndex(wiring[index])
            require(target != index) { "Reflector '$id' must not map '$letter' onto itself" }
            require(Alphabet.toIndex(wiring[target]) == index) {
                "Reflector '$id' must be reciprocal: '$letter' -> '${wiring[index]}' -> '${wiring[target]}'"
            }
        }
    }
}

class Reflector(val spec: ReflectorSpec) {
    val isThin: Boolean get() = spec.isThin

    fun reflect(inputIndex: Int): Int {
        val reflectedChar = spec.wiring[inputIndex]
        return Alphabet.toIndex(reflectedChar)
    }

    override fun toString(): String = spec.id
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
) {
    /** Compact, human readable rendering of the trace. */
    fun format(): String = buildString {
        appendLine("Signal trace for '")
        append(inputChar)
        append("' -> lamp '")
        append(outputChar)
        appendLine("'")
        steps.forEachIndexed { index, step ->
            append(index + 1)
            append(". ")
            append(step.stage)
            append(" = ")
            append(step.character)
            append(" (")
            append(step.description)
            appendLine(")")
        }
    }.trimEnd()
}
