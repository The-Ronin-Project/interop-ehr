package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninStagingRelated
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninStagingRelatedGeneratorTest {
    private lateinit var roninStageRelated: RoninStagingRelated
    private lateinit var registry: NormalizationRegistryClient
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
        registry = mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_STAGING_RELATED.value)
            } returns possibleStagingRelatedCodes
        }
        roninStageRelated = RoninStagingRelated(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationStagingRelated`() {
        // Create StagingRelated Obs with attributes you need, provide the tenant
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {
            // if you want to test for a specific status
            status of Code("registered-different")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://could-be-anything"
                        code of Code("1234567-8")
                        display of "Staging related code"
                    }
                )
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsStagingRelatedJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsStagingRelated)

        // Uncomment to take a peek at the JSON
        // println(roninObsStagingRelatedJSON)
        assertNotNull(roninObsStagingRelatedJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationStagingRelated - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsStagingRelated = rcdmPatient.rcdmObservationStagingRelated {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsStagingRelatedJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsStagingRelated)

        // Uncomment to take a peek at the JSON
        // println(roninObsStagingRelatedJSON)
        assertNotNull(roninObsStagingRelatedJSON)
        assertNotNull(roninObsStagingRelated.meta)
        assertEquals(
            roninObsStagingRelated.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_STAGING_RELATED.value
        )
        assertNotNull(roninObsStagingRelated.status)
        assertEquals(1, roninObsStagingRelated.category.size)
        assertNotNull(roninObsStagingRelated.code)
        assertNotNull(roninObsStagingRelated.subject)
        assertNotNull(roninObsStagingRelated.subject?.type?.extension)
        assertEquals("staging-related", roninObsStagingRelated.category[0].coding[0].code?.value)
        assertEquals("staging-related-uri", roninObsStagingRelated.category[0].coding[0].system?.value)
        assertNotNull(roninObsStagingRelated.status)
        assertNotNull(roninObsStagingRelated.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsStagingRelated.id)
        val patientFHIRId =
            roninObsStagingRelated.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsStagingRelated.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsStagingRelated.id?.value.toString())
        assertEquals("test", tenant)
    }

    @Test
    fun `generates valid roninObservationStagingRelated Observation`() {
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {}
        assertNotNull(roninObsStagingRelated.id)
        assertNotNull(roninObsStagingRelated.meta)
        assertEquals(
            roninObsStagingRelated.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_STAGING_RELATED.value
        )
        assertNull(roninObsStagingRelated.implicitRules)
        assertNull(roninObsStagingRelated.language)
        assertNull(roninObsStagingRelated.text)
        assertEquals(0, roninObsStagingRelated.contained.size)
        assertEquals(1, roninObsStagingRelated.extension.size)
        assertEquals(0, roninObsStagingRelated.modifierExtension.size)
        assertTrue(roninObsStagingRelated.identifier.size >= 3)
        assertTrue(roninObsStagingRelated.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsStagingRelated.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsStagingRelated.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsStagingRelated.status)
        assertEquals(1, roninObsStagingRelated.category.size)
        assertNotNull(roninObsStagingRelated.code)
        assertNotNull(roninObsStagingRelated.subject)
        assertNotNull(roninObsStagingRelated.subject?.type?.extension)
        assertEquals("staging-related", roninObsStagingRelated.category[0].coding[0].code?.value)
        assertEquals("staging-related-uri", roninObsStagingRelated.category[0].coding[0].system?.value)
        assertNotNull(roninObsStagingRelated.status)
        assertNotNull(roninObsStagingRelated.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationStagingRelated`() {
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {}
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        assertFalse(validation.hasErrors())
    }

    @Test
    fun `validation passed for roninObservationStagingRelated with code`() {
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://snomed.info/sct"
                        version of "2023-03-01"
                        code of Code("60333009")
                        display of "Clinical stage II (finding)"
                    }
                )
            }
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        assertFalse(validation.hasErrors())
    }

    @Test
    fun `validation fails for roninObservationStagingRelated with bad code`() {
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "1000000"
                        code of Code("some code here")
                    }
                )
            }
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("RONIN_OBS_003"), issueCodes)
    }

    @Test
    fun `valid subject input - validation succeeds`() {
        val roninObsStagingRelated = rcdmObservationStagingRelated("test") {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        validation.alertIfErrors()
        assertTrue(roninObsStagingRelated.subject?.reference?.value == "Patient/456")
    }

    @Test
    fun `rcdmPatient rcdmObservationStagingRelated validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsStagingRelated = rcdmPatient.rcdmObservationStagingRelated {}
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        validation.alertIfErrors()
        assertNotNull(roninObsStagingRelated.meta)
        assertNotNull(roninObsStagingRelated.identifier)
        assertTrue(roninObsStagingRelated.identifier.size >= 3)
        assertNotNull(roninObsStagingRelated.status)
        assertNotNull(roninObsStagingRelated.code)
        assertNotNull(roninObsStagingRelated.subject?.type?.extension)
        assertTrue(roninObsStagingRelated.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `rcdmPatient rcdmObservationStagingRelated - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsStagingRelated = rcdmPatient.rcdmObservationStagingRelated {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        validation.alertIfErrors()
        assertEquals("Patient/456", roninObsStagingRelated.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationStagingRelated - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsStagingRelated = rcdmPatient.rcdmObservationStagingRelated {
            subject of reference("Patient", "456")
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        validation.alertIfErrors()
        assertEquals("Patient/${rcdmPatient.id?.value}", roninObsStagingRelated.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationStagingRelated - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val roninObsStagingRelated = rcdmPatient.rcdmObservationStagingRelated {
            id of "88"
        }
        val validation = roninStageRelated.validate(roninObsStagingRelated, null)
        validation.alertIfErrors()
        assertEquals(3, roninObsStagingRelated.identifier.size)
        val values = roninObsStagingRelated.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", roninObsStagingRelated.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", roninObsStagingRelated.subject?.reference?.value)
    }
}
