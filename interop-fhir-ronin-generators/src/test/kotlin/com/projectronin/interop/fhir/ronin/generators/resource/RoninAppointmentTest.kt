package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.RoninAppointment
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninAppointmentTest {
    private lateinit var roninAppointment: RoninAppointment
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        registry = mockk()
        val normalizer: Normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        val localizer: Localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        roninAppointment = RoninAppointment(registry, normalizer, localizer)
    }

    @Test
    fun `example use for roninAppointment`() {
        // create appointment resource with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninAppointment = rcdmAppointment("fake-tenant") {
            // to test an attribute like status - provide the value
            status of Code("testing-this-status")
            // same with other attributes
            // be sure attributes are provided correctly, below serviceCategory is a list of codeable concept
            serviceCategory of listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://example.org/service-category"),
                            code = Code("gp")
                        )
                    )
                )
            )
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninAppointmentJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninAppointment)

        // Uncomment to take a peek at the JSON
        // println(roninAppointmentJSON)
        assertNotNull(roninAppointmentJSON)
    }

    @Test
    fun `example use for roninAppointment - missing required fields generated`() {
        // create appointment resource with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninAppointment = rcdmAppointment("fake-tenant") {
            // meta, identifiers, status, extension and participant will all be generated
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninAppointmentJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninAppointment)

        // Uncomment to take a peek at the JSON
        // println(roninAppointmentJSON)
        assertNotNull(roninAppointmentJSON)
        assertNotNull(roninAppointment.meta)
        assertEquals(
            roninAppointment.meta!!.profile[0].value,
            RoninProfile.APPOINTMENT.value
        )
        assertEquals(3, roninAppointment.identifier.size)
        assertNotNull(roninAppointment.status)
        assertNotNull(roninAppointment.extension)
        assertNotNull(roninAppointment.participant)
    }

    @Test
    fun `validates rcdm appoinment`() {
        val appointment = rcdmAppointment("test") {}
        val validation = roninAppointment.validate(appointment, null).hasErrors()
        assertEquals(validation, false)
    }

    @Test
    fun `validates with identifier added`() {
        val appointment = rcdmAppointment("test") {
            identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
        }
        val validation = roninAppointment.validate(appointment, null).hasErrors()
        assertEquals(validation, false)
        assertEquals(4, appointment.identifier.size)
        val ids = appointment.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `generates rcdm appointmant with given status but fails validation`() {
        val appointment = rcdmAppointment("test") {
            status of Code("this is a bad status")
        }
        assertEquals(appointment.status, Code("this is a bad status"))

        // validate appointment should fail
        val validation = roninAppointment.validate(appointment, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("INV_VALUE_SET"), issueCodes)
    }

    @Test
    fun `generates rcdm appointment and validates with appointment status extension`() {
        val appointment = rcdmAppointment("test") {
            status of Code("booked")
        }
        val validation = roninAppointment.validate(appointment, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(appointment.meta)
        assertNotNull(appointment.identifier)
        assertEquals(3, appointment.identifier.size)
        assertNotNull(appointment.status)
        assertNull(appointment.cancelationReason)
        assertNotNull(appointment.extension)
        // assert that the status value is also the code value in the extension
        assertTrue(appointment.extension[0].value?.value.toString().contains(appointment.status?.value.toString()))
    }

    @Test
    fun `generates rcdm appointment with cancelationReason if status requires`() {
        val appointment = rcdmAppointment("test") {
            status of Code("noshow")
        }
        val validation = roninAppointment.validate(appointment, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(appointment.status?.value, "noshow")
        assertNotNull(appointment.cancelationReason)
    }

    @Test
    fun `generates rcdm appointment with status requiring cancelationReason and keeps provided cancelationReason`() {
        val appointment = rcdmAppointment("test") {
            status of Code("cancelled")
            cancelationReason of CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/appointment-cancellation-reason"),
                        code = Code("some-code-here"),
                        display = "some-display-here".asFHIR()
                    )
                )
            )
        }
        val validation = roninAppointment.validate(appointment, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(appointment.status?.value, "cancelled")
        assertNotNull(appointment.cancelationReason)
        assertEquals(appointment.cancelationReason!!.coding[0].code, Code("some-code-here"))
        assertEquals(appointment.cancelationReason!!.coding[0].display?.value, "some-display-here")
    }
}
