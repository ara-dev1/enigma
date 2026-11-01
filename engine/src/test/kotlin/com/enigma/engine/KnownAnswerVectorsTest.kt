package com.enigma.engine

import com.enigma.engine.model.Alphabet
import com.enigma.engine.preset.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Known-answer tests.
 *
 * Every vector below is taken from published, independently broken Enigma traffic (Wikipedia,
 * Dirk Rijmenants' simulator manual, the bytereef M4 project and the Rasch message) or, where
 * no published message exists for a component, was generated with the `py-enigma` reference
 * implementation and is kept as a regression guard.
 *
 * These vectors are the reason the engine can be trusted: they cover the Kriegsmarine double
 * notches (VI-VIII), both thin reflectors, Reflector A, ring settings other than A and full
 * ten cable plugboards.
 */
class KnownAnswerVectorsTest {

    // ---------------------------------------------------------------- basic behaviour

    @Test
    fun `wikipedia example encrypts AAAAA to BDZGO`() {
        val machine = TestMachines.m3(rotors = "I II III", reflectorId = "B", positions = "AAA", rings = "AAA")
        assertEquals("BDZGO", machine.processText("AAAAA"))
        assertEquals("AAF", machine.display())
    }

    @Test
    fun `wikipedia example is reciprocal`() {
        val machine = TestMachines.m3(rotors = "I II III", reflectorId = "B", positions = "AAA", rings = "AAA")
        assertEquals("AAAAA", machine.processText("BDZGO"))
    }

    @Test
    fun `mechanical stepping matches the published truth data`() {
        // Rotors III-II-I from a window of KDO step through the famous double-step sequence.
        val machine = TestMachines.m3(rotors = "III II I", positions = "KDO", rings = "AAA")
        val expected = listOf("KDP", "KDQ", "KER", "LFS", "LFT", "LFU")
        val actual = expected.map {
            machine.step()
            machine.display()
        }
        assertEquals(expected, actual)
    }

    // ---------------------------------------------------------------- M3, ten cables

    @Test
    fun `rijmenants M3 message decrypts with ring settings B U L`() {
        // Key: rotors II-IV-V, Reflector B, rings B-U-L, ten cables, message key BLA.
        val machine = TestMachines.m3(
            rotors = "II IV V",
            reflectorId = "B",
            positions = "WXC",
            rings = "BUL",
            plugboard = "AV BS CG DL FU HZ IN KM OW RX"
        )
        assertEquals("BLA", machine.processText("KCH"))

        val decoder = TestMachines.m3(
            rotors = "II IV V",
            reflectorId = "B",
            positions = "BLA",
            rings = "BUL",
            plugboard = "AV BS CG DL FU HZ IN KM OW RX"
        )
        val plaintext = decoder.processText(RIJMENANTS_CIPHER_BODY)
        assertEquals(RIJMENANTS_PLAINTEXT, plaintext)
        assertTrue(plaintext.contains("ANGRIFFXINFXRGTX"))
    }

    // ---------------------------------------------------------------- navy double notches

    @Test
    fun `scharnhorst message decrypts with navy rotors and double notches`() {
        val machine = TestMachines.m3(
            rotors = "III VI VIII",
            reflectorId = "B",
            positions = "UZV",
            rings = "AHM",
            plugboard = "AN EZ HK IJ LR MQ OT PV SW UX"
        )
        assertEquals(SCHARN_PLAINTEXT, machine.processText(SCHARN_CIPHERTEXT))
        assertEquals("VGX", machine.display())
    }

    @Test
    fun `scharnhorst preset reproduces the published key`() {
        val preset = PresetCatalog.SCHARNHORST_1943
        assertEquals("III VI VIII", preset.rotorOrder)
        assertEquals("UZV", preset.positionNotation)
        assertEquals("AHM", preset.ringNotation)
        assertEquals("AN EZ HK IJ LR MQ OT PV SW UX", preset.plugboardNotation)

        val machine = EnigmaMachine.fromPreset(preset)
        assertEquals(SCHARN_PLAINTEXT, machine.processText(SCHARN_CIPHERTEXT))
    }

    @Test
    fun `navy rotors turn over twice per revolution`() {
        // Rotor VI-VIII notches sit at M and Z; a full revolution therefore triggers two kicks.
        val machine = TestMachines.m3(rotors = "I VI VIII", positions = "AAY", rings = "AAA")
        val kicks = mutableListOf<String>()
        repeat(4) {
            machine.step()
            if (machine.middleRotor.position != 'A') kicks.add(machine.display())
        }
        assertEquals(listOf("AAZ", "ABA", "ABZ"), kicks)
    }

    // ---------------------------------------------------------------- M4, thin reflectors

    @Test
    fun `u boat M4 message decrypts with beta wheel and thin reflector B`() {
        val machine = TestMachines.m4(
            greek = "BETA",
            rotors = "II IV I",
            reflectorId = "B_THIN",
            positions = "VJNA",
            rings = "AAAV",
            plugboard = "AT BL DF GJ HM NW OP QY RZ VX"
        )
        assertEquals(M4_PLAINTEXT, machine.processText(M4_CIPHERTEXT_BODY))
        assertEquals("VJWY", machine.display())
    }

    @Test
    fun `rasch message decrypts with a navy wheel in the M4`() {
        val machine = TestMachines.m4(
            greek = "BETA",
            rotors = "VI I III",
            reflectorId = "B_THIN",
            positions = "NAQL",
            rings = "ZZDG",
            plugboard = "BQ CR DI EJ KW MT OS PX UZ GH"
        )
        assertEquals(RASCH_PLAINTEXT, machine.processText(RASCH_CIPHERTEXT))
    }

    @Test
    fun `thin reflector C regression vector`() {
        val machine = TestMachines.m4(
            greek = "GAMMA",
            rotors = "IV VII II",
            reflectorId = "C_THIN",
            positions = "AQWD",
            rings = "CCFM",
            plugboard = "AV BS DL"
        )
        assertEquals("NXBLZNGOTHRBRHBQRQVFPXPAXWBQHWLRBNS", machine.processText("THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG"))
        assertEquals("AQYM", machine.display())
    }

    // ---------------------------------------------------------------- other reflectors

    @Test
    fun `reflector A regression vector`() {
        val machine = TestMachines.m3(
            rotors = "V IV II",
            reflectorId = "A",
            positions = "XYZ",
            rings = "DKP",
            plugboard = ""
        )
        assertEquals("BOLSLODDVFRXOKXHDWYCKWGJLTOOFVBIMFM", machine.processText("CRYPTANALYSISISTHEARTOFTHESECRETWAR"))
        assertEquals("XAI", machine.display())
    }

    @Test
    fun `navy rotors with ring settings ZZZ regression vector`() {
        val machine = TestMachines.m3(
            rotors = "VII VI V",
            reflectorId = "C",
            positions = "ZZZ",
            rings = "ZZZ",
            plugboard = "AT CL DH EP FG IO JN KQ MU RX"
        )
        assertEquals("VPEUDNJYWBTJLMGVRIEIHYEWPGWHTC", machine.processText("ATTACKATDAWNREPEATATTACKATDAWN"))
        assertEquals("ABD", machine.display())
    }

    // ---------------------------------------------------------------- invariants

    @Test
    fun `no letter is ever encrypted to itself`() {
        val machine = TestMachines.m4(
            greek = "GAMMA",
            rotors = "V VII III",
            reflectorId = "C_THIN",
            positions = "QWER",
            rings = "BDEF",
            plugboard = "AB CD EF GH IJ KL MN OP QR ST"
        )
        val plaintext = Alphabet.LETTERS.repeat(8)
        val ciphertext = machine.processText(plaintext)
        assertEquals(plaintext.length, ciphertext.length)
        for (index in plaintext.indices) {
            assertNotEquals(
                "Letter '${plaintext[index]}' encrypted to itself at index $index",
                plaintext[index],
                ciphertext[index]
            )
        }
    }

    @Test
    fun `encryption is reciprocal for every rotor position`() {
        val plaintext = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG"
        val encrypting = TestMachines.m3(rotors = "III V VII", positions = "MAK", rings = "CER", plugboard = "AT BL DP")
        val ciphertext = encrypting.processText(plaintext)
        assertNotEquals(plaintext, ciphertext)
        val decrypting = TestMachines.m3(rotors = "III V VII", positions = "MAK", rings = "CER", plugboard = "AT BL DP")
        assertEquals(plaintext, decrypting.processText(ciphertext))
    }

    private companion object {
        /** Ciphertext of the Rijmenants example without its five letter indicator group. */
        const val RIJMENANTS_CIPHER_BODY =
            "EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYSIOZVEQMIKUBPMMYLKLTTDEISMDICA" +
                "GYKUACTCDOMOHWXMUUIAUBSTSLRNBZSZWNRFXWFYSSXJZVIJHIDISHPRKLKAYUPADTXQSPINQMA" +
                "TLPIFSVKDASCTACDPBOPVHJK"

        const val RIJMENANTS_PLAINTEXT =
            "AUFKLXABTEILUNGXVONXKURTINOWAXKURTINOWAXNORDWESTLXSEBEZXSEBEZXUAFFLIEGERSTRASZER" +
                "IQTUNGXDUBROWKIXDUBROWKIXOPOTSCHKAXOPOTSCHKAXUMXEINSAQTDREINULLXUHRANGETRETENXAN" +
                "GRIFFXINFXRGTX"

        const val SCHARN_CIPHERTEXT =
            "YKAENZAPMSCHZBFOCUVMRMDPYCOFHADZIZMEFXTHFLOLPZLFGGBOTGOXGRETDWTJIQHLMXVJWKZUASTR"

        const val SCHARN_PLAINTEXT =
            "STEUEREJTANAFJORDJANSTANDORTQUAAACCCVIERNEUNNEUNZWOFAHRTZWONULSMXXSCHARNHORSTHCO"

        const val M4_CIPHERTEXT_BODY =
            "NCZWVUSXPNYMINHZXMQXSFWXWLKJAHSHNMCOCCAKUQPMKCSMHKSEINJUSBLKIOSXCKUBHMLLXCSJUSRR" +
                "DVKOHULXWCCBGVLIYXEOAHXRHKKFVDREWEZLXOBAFGYUJQUKGRTVUKAMEURBVEKSUHHVOYHABCJWMAKL" +
                "FKLMYFVNRIZRVVRTKOFDANJMOLBGFFLEOPRGTFLVRHOWOPBEKVWMUQFMPWPARMFHAGKXIIBG"

        const val M4_PLAINTEXT =
            "VONVONJLOOKSJHFFTTTEINSEINSDREIZWOYYQNNSNEUNINHALTXXBEIANGRIFFUNTERWASSERGEDRUECKT" +
                "YWABOSXLETZTERGEGNERSTANDNULACHTDREINULUHRMARQUANTONJOTANEUNACHTSEYHSDREIYZWOZWON" +
                "ULGRADYACHTSMYSTOSSENACHXEKNSVIERMBFAELLTYNNNNNNOOOVIERYSICHTEINSNULL"

        const val RASCH_CIPHERTEXT =
            "HCEYZTCSOPUPPZDICQRDLWXXFACTTJMBRDVCJJMMZRPYIKHZAWGLYXWTMJPQUEFSZBOTVRLALZXWVXTSL" +
                "FFFAUDQFBWRRYAPSBOWJMKLDUYUPFUQDOWVHAHCDWAUARSWTKOFVOYFPUFHVZFDGGPOOVGRMBPXXZCA" +
                "NKMONFHXPCKHJZBUMXJWXKAUODXZUCVCXPFT"

        const val RASCH_PLAINTEXT =
            "BOOTKLARXBEIJSCHNOORBETWAZWOSIBENXNOVXSECHSNULCBMXPROVIANTBISZWONULXDEZXBENOETIGEG" +
                "LMESERYNOCHVIEFKLHRXSTEHEMARQUBRUNOBRUNFZWOFUHFXLAGWWIEJKCHAEFERJXNNTWWWFUNFYEINS" +
                "FUNFMBSTEIGENDYGUTESIWXDVVVJRASCH"
    }
}
