package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.RoninMedicationRequest
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninMedicationRequestGeneratorTest {
    private lateinit var rcdmMedicationRequest: RoninMedicationRequest
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @BeforeEach
    fun setup() {
        val normalizer: Normalizer =
            mockk {
                every { normalize(any(), tenant) } answers { firstArg() }
            }
        val localizer: Localizer =
            mockk {
                every { localize(any(), tenant) } answers { firstArg() }
            }
        val medicationExtractor =
            mockk<MedicationExtractor> {
                every { extractMedication(any(), any(), any()) } returns null
            }
        rcdmMedicationRequest = RoninMedicationRequest(normalizer, localizer, medicationExtractor)
    }

    @Test
    fun `example use for rcdmMedicationRequest`() {
        // create medication request resource with attributes you need, provide the tenant
        val rcdmMedicationRequest =
            rcdmMedicationRequest("test") {
                // to test an attribute like status - provide the value
                status of Code("on-hold")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmMedicationRequestJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmMedicationRequest)

        // Uncomment to take a peek at the JSON
        // println(rcdmMedicationRequestJSON)
        assertNotNull(rcdmMedicationRequestJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmMedicationRequest - missing required fields generated`() {
        // create patient and medication request for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val rcdmMedicationRequest = rcdmPatient.rcdmMedicationRequest {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmMedicationRequestJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmMedicationRequest)

        // Uncomment to take a peek at the JSON
        // println(rcdmMedicationRequestJSON)
        assertNotNull(rcdmMedicationRequestJSON)
        assertNotNull(rcdmPatient)
        assertTrue(rcdmPatient.id?.value?.startsWith("test-") == true)
        assertNotNull(rcdmMedicationRequest.meta)
        assertEquals(
            rcdmMedicationRequest.meta!!.profile[0].value,
            RoninProfile.MEDICATION_REQUEST.value,
        )
        assertEquals(3, rcdmMedicationRequest.identifier.size)
        assertNotNull(rcdmMedicationRequest.status)
        assertNotNull(rcdmMedicationRequest.intent)
        assertNotNull(rcdmMedicationRequest.medication)
        assertEquals(DynamicValueType.REFERENCE, rcdmMedicationRequest.medication?.type)
        assertEquals("Medication", (rcdmMedicationRequest.medication?.value as Reference).decomposedType())
        assertNotNull(rcdmMedicationRequest.subject)
        assertEquals("Patient", rcdmMedicationRequest.subject?.decomposedType())
        assertNull(rcdmMedicationRequest.encounter)
        assertNotNull(rcdmMedicationRequest.requester)
        assertTrue(rcdmMedicationRequest.requester?.decomposedType() in requesterReferenceOptions)
        assertNull(rcdmMedicationRequest.performer)
        assertNull(rcdmMedicationRequest.recorder)
        assertNull(rcdmMedicationRequest.priorPrescription)
        assertEquals(emptyList<Reference>(), rcdmMedicationRequest.reasonReference)
        assertEquals(emptyList<Reference>(), rcdmMedicationRequest.basedOn)
        assertEquals(emptyList<Reference>(), rcdmMedicationRequest.insurance)
        assertEquals(emptyList<Reference>(), rcdmMedicationRequest.detectedIssue)
        assertEquals(emptyList<Reference>(), rcdmMedicationRequest.eventHistory)
        assertNotNull(rcdmMedicationRequest.id)
        val patientFHIRId =
            rcdmMedicationRequest.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            rcdmMedicationRequest.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmMedicationRequest.id?.value.toString())
        assertEquals("test", tenant)
    }

    @Test
    fun `rcdmMedicationRequest validates`() {
        val medicationRequest = rcdmMedicationRequest("test") {}
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `rcdmMedicationRequest validates with identifier added`() {
        val medicationRequest =
            rcdmMedicationRequest("test") {
                identifier of listOf(Identifier(value = "identifier".asFHIR()))
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(4, medicationRequest.identifier.size)
        val values = medicationRequest.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `generates rcdmMedicationRequest with given status but fails validation because status is bad`() {
        val medicationRequest =
            rcdmMedicationRequest("test") {
                status of Code("this is a bad status")
            }
        assertEquals(medicationRequest.status, Code("this is a bad status"))

        // validate should fail
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertTrue(validation.hasErrors())
        assertEquals(validation.issues()[0].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[0].description, "'this is a bad status' is outside of required value set")
        assertEquals(validation.issues()[0].location, LocationContext(element = "MedicationRequest", field = "status"))
    }

    @Test
    fun `rcdmMedicationRequest - valid subject input - validate succeeds`() {
        val medicationRequest =
            rcdmMedicationRequest("test") {
                subject of rcdmReference("Patient", "456")
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", medicationRequest.subject?.reference?.value)
    }

    @Test
    fun `rcdmMedicationRequest - invalid subject input - validate fails`() {
        val medicationRequest =
            rcdmMedicationRequest("test") {
                subject of rcdmReference("Device", "456")
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertTrue(validation.hasIssues())
    }

    @Test
    fun `rcdmMedicationRequest - valid requester input - validate succeeds`() {
        val medicationRequest =
            rcdmMedicationRequest("test") {
                requester of rcdmReference("Practitioner", "456")
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Practitioner/456", medicationRequest.requester?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationRequest validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val medicationRequest = rcdmPatient.rcdmMedicationRequest {}
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationRequest.subject?.reference?.value)
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `rcdmPatient rcdmMedicationRequest - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val medicationRequest =
            rcdmPatient.rcdmMedicationRequest {
                subject of reference("Patient", "456")
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationRequest.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationRequest - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val medicationRequest =
            rcdmPatient.rcdmMedicationRequest {
                subject of rcdmReference("Patient", "456")
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", medicationRequest.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmMedicationRequest - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val medicationRequest =
            rcdmPatient.rcdmMedicationRequest {
                id of "88"
            }
        val validation = rcdmMedicationRequest.validate(medicationRequest, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(3, medicationRequest.identifier.size)
        val values = medicationRequest.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", medicationRequest.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", medicationRequest.subject?.reference?.value)
    }
}
