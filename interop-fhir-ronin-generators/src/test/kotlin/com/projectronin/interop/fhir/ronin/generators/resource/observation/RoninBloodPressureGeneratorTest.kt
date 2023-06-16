package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
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

class RoninBloodPressureGeneratorTest {
    private lateinit var roninBloodPressure: RoninBloodPressure
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val systolicCoding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6")))
    private val diastolicCoding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4")))

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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
            } returns possibleBloodPressureCodes
            every {
                getRequiredValueSet("Observation.component:systolic.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
            } returns systolicCoding
            every {
                getRequiredValueSet("Observation.component:diastolic.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
            } returns diastolicCoding
        }
        roninBloodPressure = RoninBloodPressure(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationBloodPressure`() {
        // Create Blood Pressure Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsBloodPressure = rcdmObservationBloodPressure("fake-tenant") {
            // if you want to test for a specific status
            status of Code("registered-different")
            // test for a new or different code
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
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBloodPressureJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBloodPressure)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressureJSON)
    }

    @Test
    fun `example use for roninObservationBloodPressure - missing required fields generated`() {
        // Create Blood Pressure Obs with attributes you need, provide the tenant(mda), here "fake-tenant"
        val roninObsBloodPressure = rcdmObservationBloodPressure("fake-tenant") {
            // status, code and category required and will be generated
            // test for a specific subject / patient - here you pass 'type' of PATIENT and 'id' of 678910
            subject of rcdmReference("Patient", "678910")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBloodPressureJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBloodPressure)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressure.meta)
        assertEquals(
            roninObsBloodPressure.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BLOOD_PRESSURE.value
        )
        assertNotNull(roninObsBloodPressure.status)
        assertEquals(1, roninObsBloodPressure.category.size)
        assertNotNull(roninObsBloodPressure.code)
        assertNotNull(roninObsBloodPressure.subject)
        assertNotNull(roninObsBloodPressure.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBloodPressure.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBloodPressure.category[0].coding[0].system)
        assertNotNull(roninObsBloodPressure.status)
        assertNotNull(roninObsBloodPressure.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid roninObservationBloodPressure Observation`() {
        val roninObsBloodPressure = rcdmObservationBloodPressure("fake-tenant") {}
        assertNull(roninObsBloodPressure.id)
        assertNotNull(roninObsBloodPressure.meta)
        assertEquals(
            roninObsBloodPressure.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BLOOD_PRESSURE.value
        )
        assertNull(roninObsBloodPressure.implicitRules)
        assertNull(roninObsBloodPressure.language)
        assertNull(roninObsBloodPressure.text)
        assertEquals(0, roninObsBloodPressure.contained.size)
        assertEquals(1, roninObsBloodPressure.extension.size)
        assertEquals(0, roninObsBloodPressure.modifierExtension.size)
        assertTrue(roninObsBloodPressure.identifier.size >= 3)
        assertTrue(roninObsBloodPressure.identifier.any { it.value == "fake-tenant".asFHIR() })
        assertTrue(roninObsBloodPressure.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsBloodPressure.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsBloodPressure.status)
        assertEquals(1, roninObsBloodPressure.category.size)
        assertNotNull(roninObsBloodPressure.code)
        assertNotNull(roninObsBloodPressure.subject)
        assertNotNull(roninObsBloodPressure.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBloodPressure.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBloodPressure.category[0].coding[0].system)
        assertNotNull(roninObsBloodPressure.status)
        assertNotNull(roninObsBloodPressure.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `roninObservationBloodPressure with params`() {
        val roninObsBloodPressure = rcdmObservationBloodPressure("fake-tenant") {
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
            subject of rcdmReference("Patient", "678910")
        }
        assertEquals("Id-Id", roninObsBloodPressure.id?.value)
        assertEquals(4, roninObsBloodPressure.identifier.size)
        assertTrue(
            roninObsBloodPressure.identifier.contains(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        )
        assertEquals("fake-status", roninObsBloodPressure.status?.value)
        assertNotNull(roninObsBloodPressure.category[0].coding[0].system)
        assertEquals("fake text goes here", roninObsBloodPressure.code?.text?.value)
        assertEquals("Patient/678910", roninObsBloodPressure.subject?.reference?.value)
    }

    @Test
    fun `validates for rcdmObservationBloodPressure`() {
        val bpObs = rcdmObservationBloodPressure("test") {}
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for rcdmObservationBloodPressure with existing identifier`() {
        val bpObs = rcdmObservationBloodPressure("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninBloodPressure.validate(bpObs, null).hasErrors()
        assertEquals(validation, false)
        assertNotNull(bpObs.meta)
        assertNotNull(bpObs.identifier)
        assertEquals(4, bpObs.identifier.size)
        assertNotNull(bpObs.status)
        assertNotNull(bpObs.code)
        assertNotNull(bpObs.subject)
    }

    @Test
    fun `validation fails rcdmObservationBloodPressure with incorrect params`() {
        val bpObs = rcdmObservationBloodPressure("test") {
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
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(validation.issues()[0].description, "The referenced resource type was not Patient")
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "subject"))
        assertEquals(validation.issues()[1].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[1].description, "'fake-status' is outside of required value set")
        assertEquals(validation.issues()[1].location, LocationContext(element = "Observation", field = "status"))
    }
}
