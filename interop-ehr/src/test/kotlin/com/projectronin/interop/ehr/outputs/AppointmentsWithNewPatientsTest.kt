package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.util.asCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AppointmentsWithNewPatientsTest {
    @Test
    fun test() {
        val patients = listOf(Patient(id = Id("12345")))
        val appointments =
            listOf(Appointment(id = Id("54321"), participant = listOf(), status = AppointmentStatus.BOOKED.asCode()))
        val obj = AppointmentsWithNewPatients(appointments, patients)
        assertEquals(appointments, obj.appointments)
        assertEquals(patients, obj.newPatients)
    }
}
