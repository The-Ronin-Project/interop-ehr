package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.r4.resource.Patient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
class RoninPatientGeneratorTest {
    @Test
    fun `generates valid RoninPatient`() {
        // TODO so I don't break anything
        val roninPatient = RoninPatientGenerator().generate()
        assertEquals(Patient(), roninPatient)
    }
}
