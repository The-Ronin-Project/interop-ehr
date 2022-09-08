package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetPatientAppointmentsRequestTest {
    @Test
    fun `ensure json serialization conventions are correct`() {
        val patientRequestString = jacksonObjectMapper().writeValueAsString(
            GetPatientAppointmentsRequest(
                "1",
                "1/1/2015",
                "11/1/2015",
                "E5597",
                "EPI"
            )
        )
        assertEquals(
            """{"UserID":"1","StartDate":"1/1/2015","EndDate":"11/1/2015","PatientId":"E5597","PatientIdType":"EPI","UserIDType":"External","IncludeAllStatuses":"true"}""",
            patientRequestString
        )
    }
}
