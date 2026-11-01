package com.enigma.engine

import com.enigma.engine.catalog.ReflectorCatalog
import com.enigma.engine.catalog.RotorCatalog
import com.enigma.engine.configuration.EnigmaConfiguration
import com.enigma.engine.configuration.RotorConfiguration
import com.enigma.engine.model.EnigmaModel
import com.enigma.engine.model.Reflector
import com.enigma.engine.model.Rotor
import com.enigma.engine.preset.PresetCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationTest {

    @Test
    fun `m4 snapshot round trips through text`() {
        val machine = TestMachines.m4(
            greek = "GAMMA",
            rotors = "VI VII VIII",
            reflectorId = "C_THIN",
            positions = "QWER",
            rings = "BDFH",
            plugboard = "AV BS CG DL FU HZ IN KM OW RX"
        )
        val snapshot = machine.configuration().encode()

        val decoded = EnigmaConfiguration.decode(snapshot)
        assertEquals(snapshot, decoded.encode())
        assertEquals(EnigmaModel.M4, decoded.model)
        assertEquals("C_THIN", decoded.reflectorId)
        assertEquals(10, decoded.plugboard.size)
        assertEquals("GAMMA", decoded.greekRotor?.specId)
        assertEquals("VI", decoded.rotors[0].specId)
        assertEquals('R', decoded.rotors[2].normalizedPosition)

        val rebuilt = EnigmaMachine.fromConfiguration(decoded)
        assertEquals(machine.display(), rebuilt.display())
        assertEquals(machine.processText("THEQUICKBROWNFOX"), rebuilt.processText("THEQUICKBROWNFOX"))
    }

    @Test
    fun `m3 snapshot round trips without a greek rotor`() {
        val machine = TestMachines.m3(
            rotors = "IV V VI",
            reflectorId = "A",
            positions = "XYZ",
            rings = "ZAB",
            plugboard = "AB CD"
        )
        val decoded = EnigmaConfiguration.decode(machine.configuration().encode())
        assertNull(decoded.greekRotor)
        assertEquals(EnigmaModel.M3, decoded.model)
        val rebuilt = EnigmaMachine.fromConfiguration(decoded)
        assertEquals(machine.display(), rebuilt.display())
        assertEquals(machine.processText("SECRET"), rebuilt.processText("SECRET"))
    }

    @Test
    fun `snapshots survive an empty plugboard`() {
        val machine = EnigmaMachine.createDefaultM3()
        val snapshot = machine.configuration().encode()
        val decoded = EnigmaConfiguration.decode(snapshot)
        assertTrue(decoded.plugboard.isEmpty())
        assertEquals(snapshot, decoded.encode())
    }

    @Test
    fun `invalid snapshots are rejected`() {
        val invalid = listOf(
            "",
            "v1|M3|B|-|I:A:1;II:A:1",
            "v9|M3|B|-|I:A:1;II:A:1;III:A:1|-",
            "v1|M3|B_THIN|-|I:A:1;II:A:1;III:A:1|-",
            "v1|M4|B_THIN|-|I:A:1;II:A:1;III:A:1|-",
            "v1|M4|B_THIN|-|I:A:1;II:A:1;III:A:1|AB",
            "v1|M3|B|-|I:A:1;II:A:1|-",
            "v1|M3|B|-|I:A:1;II:A:1;III:A:27|-",
            "v1|M3|B|-|I:A:1;II:A:1;III:A:1|ABC",
            "v1|M3|B|-|I:A:1;II:A:1;III:A:1|AA",
            "v1|M3|Z|-|I:A:1;II:A:1;III:A:1|-",
            "v1|M3|B|-|IX:A:1;II:A:1;III:A:1|-",
            "v1|M3|B|BETA:A:1|I:A:1;II:A:1;III:A:1|-"
        )
        invalid.forEach { snapshot ->
            assertThrows(
                "Snapshot '$snapshot' should have been rejected",
                IllegalArgumentException::class.java
            ) { EnigmaConfiguration.decode(snapshot) }
        }
    }

    @Test
    fun `restorePositions rewinds a used machine`() {
        val machine = TestMachines.m3(rotors = "I II III", positions = "ABC")
        val snapshot = machine.configuration()
        machine.processText("HELLOWORLD")
        assertNotEquals("ABC", machine.display())
        machine.restorePositions(snapshot)
        assertEquals("ABC", machine.display())
    }

    @Test
    fun `configuration rejects illegal model combinations`() {
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaConfiguration(
                model = EnigmaModel.M3,
                reflectorId = "B_THIN",
                rotors = defaultRotors(),
                greekRotor = null
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaConfiguration(
                model = EnigmaModel.M4,
                reflectorId = "B_THIN",
                rotors = defaultRotors(),
                greekRotor = null
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaConfiguration(model = EnigmaModel.M3, reflectorId = "B", rotors = defaultRotors().take(2))
        }
    }

    @Test
    fun `machine topology is enforced`() {
        // M4 with a standard reflector.
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaMachine(
                model = EnigmaModel.M4,
                reflector = Reflector(ReflectorCatalog.REFLECTOR_B),
                greekRotor = Rotor(RotorCatalog.ROTOR_BETA),
                leftRotor = Rotor(RotorCatalog.ROTOR_I),
                middleRotor = Rotor(RotorCatalog.ROTOR_II),
                rightRotor = Rotor(RotorCatalog.ROTOR_III)
            )
        }
        // M4 without a Greek rotor.
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaMachine(
                model = EnigmaModel.M4,
                reflector = Reflector(ReflectorCatalog.REFLECTOR_B_THIN),
                greekRotor = null,
                leftRotor = Rotor(RotorCatalog.ROTOR_I),
                middleRotor = Rotor(RotorCatalog.ROTOR_II),
                rightRotor = Rotor(RotorCatalog.ROTOR_III)
            )
        }
        // M3 with a fourth rotor.
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaMachine(
                model = EnigmaModel.M3,
                reflector = Reflector(ReflectorCatalog.REFLECTOR_B),
                greekRotor = Rotor(RotorCatalog.ROTOR_BETA),
                leftRotor = Rotor(RotorCatalog.ROTOR_I),
                middleRotor = Rotor(RotorCatalog.ROTOR_II),
                rightRotor = Rotor(RotorCatalog.ROTOR_III)
            )
        }
        // M3 with a thin reflector.
        assertThrows(IllegalArgumentException::class.java) {
            EnigmaMachine(
                model = EnigmaModel.M3,
                reflector = Reflector(ReflectorCatalog.REFLECTOR_C_THIN),
                leftRotor = Rotor(RotorCatalog.ROTOR_I),
                middleRotor = Rotor(RotorCatalog.ROTOR_II),
                rightRotor = Rotor(RotorCatalog.ROTOR_III)
            )
        }
    }

    @Test
    fun `applying a preset of the wrong model fails loudly instead of silently dropping the greek rotor`() {
        val m3 = EnigmaMachine.createDefaultM3()
        assertThrows(IllegalArgumentException::class.java) { m3.applyPreset(PresetCatalog.KRIEGSMARINE_M4) }

        val m4 = EnigmaMachine.createDefaultM4()
        assertThrows(IllegalArgumentException::class.java) { m4.applyPreset(PresetCatalog.WEHRMACHT_I) }

        // The matching preset still works and keeps the machine consistent.
        m4.applyPreset(PresetCatalog.KRIEGSMARINE_M4)
        assertEquals(EnigmaModel.M4, m4.model)
        assertEquals(PresetCatalog.KRIEGSMARINE_M4.positionNotation, m4.display())
    }

    @Test
    fun `rotor configuration validates its inputs`() {
        assertEquals('A', RotorConfiguration("I", 'a', 1).normalizedPosition)
        assertThrows(IllegalArgumentException::class.java) { RotorConfiguration("I", '1', 1) }
        assertThrows(IllegalArgumentException::class.java) { RotorConfiguration("I", 'A', 0) }
        assertThrows(IllegalArgumentException::class.java) { RotorConfiguration("XX", 'A', 1) }
    }

    private fun defaultRotors(): List<RotorConfiguration> = listOf(
        RotorConfiguration("I", 'A', 1),
        RotorConfiguration("II", 'A', 1),
        RotorConfiguration("III", 'A', 1)
    )
}
