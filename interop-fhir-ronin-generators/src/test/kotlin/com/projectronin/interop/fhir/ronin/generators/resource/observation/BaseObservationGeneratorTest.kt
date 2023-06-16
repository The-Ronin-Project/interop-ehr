package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
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
        roninObs = RoninObservation(normalizer, localizer)
    }

    @Test
    fun `example use for roninObservation`() {
        // Create ronin Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObservation = rcdmObservation("fake-tenant") {
            // if you want to test for a specific status, generated if not provided
            status of Code("registered-different")
            // test for a new or different code, generated if not provided
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
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910, generated if not provided
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

        // Uncomment to take a peek at the JSON
        // println(roninObservationJSON)
        assertNotNull(roninObservationJSON)
    }

    @Test
    fun `example use for roninObservation - missing required fields generated`() {
        // Create ronin Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObservation = rcdmObservation("fake-tenant") {
            // status, code and category required and will be generated
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910, generated if not provided
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

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
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid base roninObservation`() {
        val roninObservation = rcdmObservation("fake-tenant") {}
        assertNull(roninObservation.id)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(0, roninObservation.extension.size)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "fake-tenant".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates base roninObservation with params`() {
        val roninObservation = rcdmObservation("fake-tenant") {
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
            subject of rcdmReference("Practitioner", "678910")
        }
        assertEquals("Id-Id", roninObservation.id?.value)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(0, roninObservation.extension.size)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "fake-tenant".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject)
        assertEquals("Practitioner/678910", roninObservation.subject?.reference?.value)
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
        assertNotNull(baseObs.subject)
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
        assertNotNull(baseObs.subject)
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
            subject of rcdmReference("Practitioner", "678910")
        }
        val validation = roninObs.validate(baseObs, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(validation.issues()[0].description, "The referenced resource type was not one of Patient, Location")
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "subject"))
        assertEquals(validation.issues()[1].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[1].description, "'fake-status' is outside of required value set")
        assertEquals(validation.issues()[1].location, LocationContext(element = "Observation", field = "status"))
    }
}
