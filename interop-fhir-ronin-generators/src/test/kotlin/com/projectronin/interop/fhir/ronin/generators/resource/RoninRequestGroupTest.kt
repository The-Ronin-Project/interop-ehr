package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.identifier
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.dateTime
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.RoninRequestGroup
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninRequestGroupTest {
    private lateinit var rcdmRequestGroup: RoninRequestGroup
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        val normalizer: Normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        val localizer: Localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        rcdmRequestGroup = RoninRequestGroup(normalizer, localizer)
    }

    @Test
    fun `example use for rcdmRequestGroup`() {
        // create request group resource with attributes you need, provide the tenant
        val rcdmRequestGroup = rcdmRequestGroup("test") {
            // to test an attribute like status - provide the value
            status of Code("on-hold")
            authoredOn of dateTime {
                year of 1990
                day of 8
                month of 4
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmRequestGroupJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmRequestGroup)

        // Uncomment to take a peek at the JSON
        // println(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroupJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmRequestGroup - missing required fields generated`() {
        // create patient and request group for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val rcdmRequestGroup = rcdmPatient.rcdmRequestGroup {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmRequestGroupJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmRequestGroup)

        // Uncomment to take a peek at the JSON
        // println(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroupJSON)
        assertNotNull(rcdmRequestGroup.meta)
        assertEquals(
            rcdmRequestGroup.meta!!.profile[0].value,
            RoninProfile.REQUEST_GROUP.value
        )
        assertEquals(3, rcdmRequestGroup.identifier.size)
        assertNotNull(rcdmRequestGroup.status)
        assertNotNull(rcdmRequestGroup.intent)
        assertNotNull(rcdmRequestGroup.subject)
        assertNotNull(rcdmRequestGroup.id)
        val patientFHIRId = rcdmRequestGroup.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = rcdmRequestGroup.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmRequestGroup.id?.value.toString())
        assertEquals("test", tenant)
    }

    @Test
    fun `rcdmRequestGroup validates`() {
        val requestGroup = rcdmRequestGroup("test") {}
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `rcdmRequestGroup validates with identifier added`() {
        val requestGroup = rcdmRequestGroup("test") {
            identifier of listOf(Identifier(value = "identifier".asFHIR()))
        }
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(4, requestGroup.identifier.size)
        val values = requestGroup.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `generates rcdmRequestGroup with given status but fails validation because status is bad`() {
        val requestGroup = rcdmRequestGroup("test") {
            status of Code("this is a bad status")
        }
        assertEquals(requestGroup.status, Code("this is a bad status"))

        // validate should fail
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertTrue(validation.hasErrors())
        assertEquals(validation.issues()[0].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[0].description, "'this is a bad status' is outside of required value set")
        assertEquals(validation.issues()[0].location, LocationContext(element = "RequestGroup", field = "status"))
    }

    @Test
    fun `rcdmRequestGroup - valid subject input - validate succeeds`() {
        val requestGroup = rcdmRequestGroup("test") {
            subject of rcdmReference("Patient", "456")
        }
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", requestGroup.subject?.reference?.value)
    }

    @Test
    fun `rcdmRequestGroup - invalid subject input - validate fails`() {
        val requestGroup = rcdmRequestGroup("test") {
            subject of rcdmReference("Device", "456")
        }
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals("RONIN_INV_REF_TYPE", validation.issues()[0].code)
        assertEquals("The referenced resource type was not one of Group, Patient", validation.issues()[0].description)
        assertEquals(LocationContext(element = "RequestGroup", field = "subject"), validation.issues()[0].location)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val requestGroup = rcdmPatient.rcdmRequestGroup {}
        assertEquals("Patient/${rcdmPatient.id?.value}", requestGroup.subject?.reference?.value)
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup - any subject input - base patient overrides input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val requestGroup = rcdmPatient.rcdmRequestGroup {
            subject of rcdmReference("Device", "456")
        }
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/${rcdmPatient.id?.value}", requestGroup.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmRequestGroup - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val requestGroup = rcdmPatient.rcdmRequestGroup {
            id of "88"
        }
        val validation = rcdmRequestGroup.validate(requestGroup, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(3, requestGroup.identifier.size)
        val values = requestGroup.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", requestGroup.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", requestGroup.subject?.reference?.value)
    }
}
