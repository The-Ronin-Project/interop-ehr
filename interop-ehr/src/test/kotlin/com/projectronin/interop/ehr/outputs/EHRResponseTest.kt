package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.TypeInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EHRResponseTest {
    @Test
    fun `adds UUID to all resources in Bundle`() {
        val bundle = Bundle(
            type = null,
            entry = listOf(
                BundleEntry(resource = Patient()),
                BundleEntry(resource = null),
                BundleEntry(resource = Patient())
            )
        )
        val ret = bundle.addMetaSource("12345") as Bundle
        assertEquals("12345", ret.meta!!.source!!.value)
        assertEquals(
            "12345",
            ret.entry.first().resource!!.meta!!.source!!.value
        )
        assertEquals(
            "12345",
            ret.entry.last().resource!!.meta!!.source!!.value
        )
    }

    @Test
    fun `body inline works`() {
        val patient = Patient()
        val httpResponse = mockk<HttpResponse> {
            coEvery { body<Patient>() } returns patient
        }
        val ehrResponse = EHRResponse(httpResponse, "12345")
        val body = runBlocking { ehrResponse.body<Patient>() }
        assertEquals(body.meta!!.source!!.value, "12345")
    }

    @Test
    fun `body function works`() {
        val patient = Patient(meta = Meta(FHIRString("12345")))
        val httpResponse = mockk<HttpResponse> {
            coEvery { body<Patient>(TypeInfo(Patient::class, Patient::class.java)) } returns patient
        }
        val ehrResponse = EHRResponse(httpResponse, "12345")
        val body = runBlocking { ehrResponse.body<Patient>(TypeInfo(Patient::class, Patient::class.java)) }
        assertEquals(body.meta!!.source!!.value, "12345")
    }

    @Test
    fun `else works`() {
        val nonResourceObject = GetFHIRIDResponse(fhirID = "12345")
        val httpResponse = mockk<HttpResponse> {
            coEvery { body<GetFHIRIDResponse>() } returns nonResourceObject
        }
        val ehrResponse = EHRResponse(httpResponse, "12345")
        val body = runBlocking { ehrResponse.body<GetFHIRIDResponse>() }
        assertEquals(body, nonResourceObject)
    }
}
