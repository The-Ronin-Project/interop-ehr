package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GetFHIRIDResponseTest {

    @Test
    fun test() {
        val patient = Patient(id = Id("12345"))
        val response = GetFHIRIDResponse("12345", patient)
        assertEquals(response.fhirID, "12345")
        assertEquals(response.newPatientObject?.id?.value, "12345")
    }
}
