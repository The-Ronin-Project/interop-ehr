package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.DynamicValues
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.observationComponent
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.generators.util.tenantSourceConditionExtension
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
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

class BaseObservationGeneratorTest {
    private lateinit var roninObs: RoninObservation
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
        roninObs = RoninObservation(normalizer, localizer, mockk())
    }

    @Test
    fun `example use for roninObservation`() {
        // Create roninObs with attributes you need, provide the tenant
        val roninObservation = rcdmObservation("test") {
            // to test an attribute like status - provide the value
            status of Code("registered-different")
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        version of "0.01"
                        code of Code("94499-1")
                        display of "Respiratory viral pathogens DNA and RNA panel - Respiratory specimen Qualitative by NAA with probe detection"
                    }
                )
                text of "Respiratory viral pathogens DNA and RNA panel" // text is kept if provided otherwise only a code.coding is generated
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

        // Uncomment to take a peek at the JSON
        // println(roninObservationJSON)
        assertNotNull(roninObservationJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservation - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObservation = rcdmPatient.rcdmObservation {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

        // Uncomment to take a peek at the JSON
        // println(roninObservationJSON)
        assertNotNull(roninObservationJSON)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value
        )
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid base roninObservation`() {
        val roninObservation = rcdmObservation("test") {}
        assertNotNull(roninObservation.id)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(listOf(tenantSourceObservationCodeExtension), roninObservation.extension)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        val patientFHIRId =
            roninObservation.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObservation.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObservation.id?.value.toString())
        assertEquals("test", tenant)
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates base roninObservation with params`() {
        val roninObservation = rcdmObservation("test") {
            id of Id("Id-Id")
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
            status of Code("fake-status")
            category of listOf(
                codeableConcept {
                    coding of listOf(
                        coding {
                            system of "fake-system"
                        }
                    )
                }
            )
            code of codeableConcept {
                text of "fake text goes here"
            }
        }
        assertEquals("test-Id-Id", roninObservation.id?.value)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(listOf(tenantSourceObservationCodeExtension), roninObservation.extension)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for base observation`() {
        val baseObs = rcdmObservation("test") {}
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertTrue(baseObs.identifier.size >= 3)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `validation for base observation with existing identifier`() {
        val baseObs = rcdmObservation("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertEquals(4, baseObs.identifier.size)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `validation fails base observation with incorrect params`() {
        val baseObs = rcdmObservation("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
            status of Code("fake-status")
            category of listOf(
                codeableConcept {
                    coding of listOf(
                        coding {
                            system of "fake-system"
                        }
                    )
                }
            )
            code of codeableConcept {
                text of "fake text goes here"
            }
            performer of listOf(
                reference("Location", "789")
            )
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(
            validation.issues()[0].description,
            "The referenced resource type was not one of CareTeam, Organization, Patient, Practitioner, PractitionerRole"
        )
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "performer[0]"))
        assertEquals(validation.issues()[1].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[1].description, "'fake-status' is outside of required value set")
        assertEquals(validation.issues()[1].location, LocationContext(element = "Observation", field = "status"))
    }

    @Test
    fun `valid subject input - validate succeeds`() {
        val roninObservation = rcdmObservation("test") {
            subject of rcdmReference("Location", "456")
        }
        val validation = roninObs.validate(roninObservation, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Location/456", roninObservation.subject?.reference?.value)
    }

    @Test
    fun `invalid subject input - validate fails`() {
        val roninObservation = rcdmObservation("test") {
            subject of rcdmReference("Practitioner", "789")
        }
        val validation = roninObs.validate(roninObservation, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(
            validation.issues()[0].description,
            "The referenced resource type was not one of Patient, Location"
        )
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "subject"))
    }

    @Test
    fun `rcdmPatient rcdmObservation validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val baseObs = rcdmPatient.rcdmObservation {}
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertTrue(baseObs.identifier.size >= 3)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `rcdmPatient rcdmObservation - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val baseObs = rcdmPatient.rcdmObservation {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", baseObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservation - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val baseObs = rcdmPatient.rcdmObservation {
            subject of reference("Patient", "456")
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/${rcdmPatient.id?.value}", baseObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservation - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val baseObs = rcdmPatient.rcdmObservation {
            id of "88"
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(3, baseObs.identifier.size)
        val values = baseObs.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", baseObs.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", baseObs.subject?.reference?.value)
    }

    @Test
    fun `generates code extension when extensions are provided`() {
        val observation = rcdmBaseObservation("test") {
            extension of tenantSourceConditionExtension
        }

        assertEquals(
            tenantSourceConditionExtension + tenantSourceObservationCodeExtension,
            observation.extension
        )
    }

    @Test
    fun `generates value extension when codeable concept value is provided`() {
        val observation = rcdmBaseObservation("test") {
            value of DynamicValues.codeableConcept(codeableConcept { })
        }

        assertEquals(
            listOf(tenantSourceObservationCodeExtension, tenantSourceObservationValueExtension),
            observation.extension
        )
    }

    @Test
    fun `does not generate value extension when other value type is provided`() {
        val observation = rcdmBaseObservation("test") {
            value of DynamicValues.string("value")
        }

        assertEquals(
            listOf(tenantSourceObservationCodeExtension),
            observation.extension
        )
    }

    @Test
    fun `generates component code extension when component with code is provided`() {
        val observation = rcdmBaseObservation("test") {
            component of listOf(
                observationComponent {
                    code of codeableConcept { }
                }
            )
        }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension
        )
    }

    @Test
    fun `generates component value extension when component with codeable concept value is provided`() {
        val observation = rcdmBaseObservation("test") {
            component of listOf(
                observationComponent {
                    code of codeableConcept { }
                    value of DynamicValues.codeableConcept(codeableConcept { })
                }
            )
        }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension, tenantSourceObservationComponentValueExtension),
            observation.component.first().extension
        )
    }

    @Test
    fun `does not generate component value extension when component with other value type is provided`() {
        val observation = rcdmBaseObservation("test") {
            component of listOf(
                observationComponent {
                    code of codeableConcept { }
                    value of DynamicValues.string("value")
                }
            )
        }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension
        )
    }

    @Test
    fun `does not generate component value extension when component is provided without a value`() {
        val observation = rcdmBaseObservation("test") {
            component of listOf(
                observationComponent {
                    code of codeableConcept { }
                    value of null
                }
            )
        }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension
        )
    }
}
