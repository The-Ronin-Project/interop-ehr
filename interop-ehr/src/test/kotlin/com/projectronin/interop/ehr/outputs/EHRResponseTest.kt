package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.BundleEntry
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.stu3.element.STU3BundleEntry
import com.projectronin.interop.fhir.stu3.resource.STU3Appointment
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.fhir.util.asCode
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
    fun `adds UUID to all resources in STU3Bundle`() {
        val bundle = STU3Bundle(
            type = null,
            entry = listOf(
                STU3BundleEntry(
                    resource = STU3Appointment(
                        participant = listOf(),
                        status = AppointmentStatus.ARRIVED.asCode()
                    )
                ),
                STU3BundleEntry(resource = null),
                STU3BundleEntry(
                    resource = STU3Appointment(
                        participant = listOf(),
                        status = AppointmentStatus.BOOKED.asCode()
                    )
                )
            )
        )
        val ret = bundle.addMetaSource("12345") as STU3Bundle
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
    fun `body inline works for STU3`() {
        val appointment = STU3Appointment(
            participant = listOf(),
            status = AppointmentStatus.BOOKED.asCode()
        )
        val httpResponse = mockk<HttpResponse> {
            coEvery { body<STU3Appointment>() } returns appointment
        }
        val ehrResponse = EHRResponse(httpResponse, "12345")
        val body = runBlocking { ehrResponse.body<STU3Appointment>() }
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
    fun `stu3Body function works`() {
        val appointment = STU3Appointment(
            meta = Meta(FHIRString("67890")),
            participant = listOf(),
            status = AppointmentStatus.BOOKED.asCode()
        )
        val httpResponse = mockk<HttpResponse> {
            coEvery {
                body<STU3Appointment>(
                    TypeInfo(
                        STU3Appointment::class,
                        STU3Appointment::class.java
                    )
                )
            } returns appointment
        }
        val ehrResponse = EHRResponse(httpResponse, "12345")
        val body = runBlocking {
            ehrResponse.stu3Body<STU3Appointment>(
                TypeInfo(
                    STU3Appointment::class,
                    STU3Appointment::class.java
                )
            )
        }
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
