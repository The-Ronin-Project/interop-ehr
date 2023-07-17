package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.RoninDocumentReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninDocumentReferenceTest {
    private lateinit var rcdmDocumentReference: RoninDocumentReference
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
        rcdmDocumentReference = RoninDocumentReference(normalizer, localizer)
    }

    @Test
    fun `example use for rcdmDocumentReference`() {
        // create document reference resource with attributes you need, provide the tenant
        val rcdmDocumentReference = rcdmDocumentReference("test") {
            // to test an attribute like status - provide the value
            status of Code("on-hold")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmDocumentReferenceJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmDocumentReference)

        // Uncomment to take a peek at the JSON
        // println(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmDocumentReferenceJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmDocumentReference - missing required fields generated`() {
        // create patient and document reference for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val rcdmDocumentReference = rcdmPatient.rcdmDocumentReference {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val rcdmDocumentReferenceJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcdmDocumentReference)

        // Uncomment to take a peek at the JSON
        // println(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmDocumentReferenceJSON)
        assertNotNull(rcdmPatient)
        assertTrue(rcdmPatient.id?.value?.startsWith("test-") == true)
        assertNotNull(rcdmDocumentReference.meta)
        assertEquals(
            RoninProfile.DOCUMENT_REFERENCE.value,
            rcdmDocumentReference.meta!!.profile[0].value
        )
        assertEquals(3, rcdmDocumentReference.identifier.size)
        assertNotNull(rcdmDocumentReference.status)
        assertTrue(rcdmDocumentReference.status in possibleDocumentReferenceStatusCodes)
        assertNotNull(rcdmDocumentReference.type)
        assertTrue(rcdmDocumentReference.type?.coding?.first() in possibleDocumentReferenceTypeCodes)
        assertEquals(1, rcdmDocumentReference.category.size)
        assertTrue(rcdmDocumentReference.category.first().coding.first() in possibleDocumentReferenceCategoryCodes)
        assertNotNull(rcdmDocumentReference.subject)
        assertTrue(rcdmDocumentReference.status in possibleDocumentReferenceStatusCodes)
        assertNotNull(rcdmDocumentReference.id)
        val patientFHIRId = rcdmDocumentReference.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = rcdmDocumentReference.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", rcdmDocumentReference.id?.value.toString())
        assertEquals("test", tenant)
    }

    @Test
    fun `rcdmDocumentReference validates`() {
        val documentReference = rcdmDocumentReference("test") {}
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
    }

    @Test
    fun `rcdmDocumentReference validates with identifier added`() {
        val documentReference = rcdmDocumentReference("test") {
            identifier of listOf(Identifier(value = "identifier".asFHIR()))
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals(4, documentReference.identifier.size)
        val values = documentReference.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("identifier".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        // the fourth value is a generated identifier string
    }

    @Test
    fun `generates rcdmDocumentReference with given status but fails validation because status is bad`() {
        val documentReference = rcdmDocumentReference("test") {
            status of Code("this is a bad status")
        }
        assertEquals(Code("this is a bad status"), documentReference.status)

        // validate should fail
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertTrue(validation.hasErrors())
        assertEquals("INV_VALUE_SET", validation.issues()[0].code)
        assertEquals("'this is a bad status' is outside of required value set", validation.issues()[0].description)
        assertEquals(LocationContext(element = "DocumentReference", field = "status"), validation.issues()[0].location)
    }

    @Test
    fun `rcdmDocumentReference - valid subject input - validate succeeds`() {
        val documentReference = rcdmDocumentReference("test") {
            subject of rcdmReference("Patient", "456")
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals("Patient/456", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmDocumentReference - invalid subject input - validate fails`() {
        val documentReference = rcdmDocumentReference("test") {
            subject of rcdmReference("Device", "456")
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(true, validation.hasErrors())
        assertEquals("RONIN_INV_REF_TYPE", validation.issues()[0].code)
        assertEquals("The referenced resource type was not Patient", validation.issues()[0].description)
        assertEquals(LocationContext(element = "DocumentReference", field = "subject"), validation.issues()[0].location)
    }

    @Test
    fun `rcdmDocumentReference - valid type input - validate succeeds`() {
        val documentReference = rcdmDocumentReference("test") {
            type of codeableConcept {
                coding of listOf(
                    coding {
                        system of CodeSystem.LOINC.uri
                        code of Code("100")
                        display of "Fake"
                    }
                )
            }
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals(Code("100"), documentReference.type?.coding?.first()?.code)
    }

    @Test
    fun `rcdmDocumentReference - invalid type input - validate fails`() {
        val documentReference = rcdmDocumentReference("test") {
            type of codeableConcept {
                coding of listOf(
                    coding {
                        system of CodeSystem.LOINC.uri
                        code of Code("100")
                        display of "Fake"
                    },
                    coding {
                        system of CodeSystem.LOINC.uri
                        code of Code("200")
                        display of "Also Fake"
                    }
                )
            }
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(true, validation.hasErrors())
        assertEquals("RONIN_DOCREF_002", validation.issues()[0].code)
        assertEquals("One, and only one, coding entry is allowed for type", validation.issues()[0].description)
        assertEquals(LocationContext(element = "DocumentReference", field = "type.coding"), validation.issues()[0].location)
    }

    @Test
    fun `rcdmDocumentReference - valid category input - validate succeeds`() {
        val documentReference = rcdmDocumentReference("test") {
            category of listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri,
                            code = Code("clinical-note"),
                            display = "Clinical Note".asFHIR()
                        )
                    )
                )
            )
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals("Clinical Note", documentReference.category.first().coding.first().display?.value)
    }

    @Test
    fun `rcdmDocumentReference - invalid category input - validate fails`() {
        val documentReference = rcdmDocumentReference("test") {
            category of listOf(
                CodeableConcept(
                    coding =
                    listOf(
                        Coding(
                            system = Uri("bad system"),
                            code = Code("bad code"),
                            display = "Clinical Note".asFHIR()
                        )
                    )
                )
            )
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(true, validation.hasErrors())
        assertEquals("INV_VALUE_SET", validation.issues()[0].code)
        assertEquals("'bad system|bad code' is outside of required value set", validation.issues()[0].description)
        assertEquals(LocationContext(element = "DocumentReference", field = "category"), validation.issues()[0].location)
        assertEquals("R4_INV_PRIM", validation.issues()[1].code)
        assertEquals("Supplied value is not valid for a Uri", validation.issues()[1].description)
        assertEquals(LocationContext(element = "DocumentReference", field = "category[0].coding[0].system"), validation.issues()[1].location)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val documentReference = rcdmPatient.rcdmDocumentReference {}
        assertEquals("Patient/${rcdmPatient.id?.value}", documentReference.subject?.reference?.value)
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val documentReference = rcdmPatient.rcdmDocumentReference {
            subject of rcdmReference("Patient", "456")
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals("Patient/456", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val documentReference = rcdmPatient.rcdmDocumentReference {
            subject of reference("Patient", "456")
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals("Patient/${rcdmPatient.id?.value}", documentReference.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmDocumentReference - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val documentReference = rcdmPatient.rcdmDocumentReference {
            id of "88"
        }
        val validation = rcdmDocumentReference.validate(documentReference, null)
        assertEquals(false, validation.hasErrors())
        assertEquals(3, documentReference.identifier.size)
        val values = documentReference.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", documentReference.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", documentReference.subject?.reference?.value)
    }
}
