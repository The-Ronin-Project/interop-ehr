package com.projectronin.interop.ehr.epic.apporchard.model

import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetAppointmentResponseTest {

    @Test
    fun `errorOrAppointments errors`() {
        val response = GetAppointmentsResponse(null, null)
        assertThrows<AppOrchardError> { response.errorOrAppointments() }
    }

    @Test
    fun `errorOrAppointments returns appointments`() {
        val response = GetAppointmentsResponse(listOf(mockk()), null)
        assertEquals(1, response.errorOrAppointments().size)
    }
}
