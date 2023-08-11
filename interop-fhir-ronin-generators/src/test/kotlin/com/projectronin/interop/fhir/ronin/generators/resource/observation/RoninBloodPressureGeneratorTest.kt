package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBloodPressure
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
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
    private val systolicCoding = ValueSetList(
        listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6"))),
        ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "systolicbloodpressure",
            valueSetUuid = "800f4e2b-d716-44e8-9183-87d3ed8cba9b",
            version = "1"
        )
    )
    private val diastolicCoding = ValueSetList(
        listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4"))),
        ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "diastolicbloodpressure",
            valueSetUuid = "0718bdad-3386-4193-8b47-1cf9220b4bb3",
            version = "1"
        )
    )

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
        // Create Blood Pressure Obs with attributes you need, provide the tenant
        val roninObsBloodPressure = rcdmObservationBloodPressure("test") {
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
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBloodPressureJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBloodPressure)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressureJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBloodPressure - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsBloodPressure = rcdmPatient.rcdmObservationBloodPressure {}
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
        assertNotNull(roninObsBloodPressure.id)
        val patientFHIRId = roninObsBloodPressure.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninObsBloodPressure.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBloodPressure.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBloodPressure Observation`() {
        val roninObsBloodPressure = rcdmObservationBloodPressure("test") {}
        assertNotNull(roninObsBloodPressure.id)
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
        assertTrue(roninObsBloodPressure.identifier.any { it.value == "test".asFHIR() })
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
        val roninObsBloodPressure = rcdmObservationBloodPressure("test") {
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
        assertEquals("test-Id-Id", roninObsBloodPressure.id?.value)
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
        assertTrue(roninObsBloodPressure.subject?.reference?.value?.startsWith("Patient/test-") == true)
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
            performer of listOf(
                reference("Location", "789")
            )
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(validation.issues()[0].description, "The referenced resource type was not one of CareTeam, Organization, Patient, Practitioner, PractitionerRole")
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "performer[0]"))
        assertEquals(validation.issues()[1].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[1].description, "'fake-status' is outside of required value set")
        assertEquals(validation.issues()[1].location, LocationContext(element = "Observation", field = "status"))
    }

    @Test
    fun `invalid subject input - fails validation`() {
        val bpObs = rcdmObservationBloodPressure("test") {
            subject of rcdmReference("Location", "456")
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "RONIN_INV_REF_TYPE")
        assertEquals(validation.issues()[0].description, "The referenced resource type was not Patient")
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "subject"))
    }

    @Test
    fun `valid subject input - validation succeeds`() {
        val bpObs = rcdmObservationBloodPressure("test") {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
        assertTrue(bpObs.subject?.reference?.value == "Patient/456")
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val bpObs = rcdmPatient.rcdmObservationBloodPressure {}
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(bpObs.meta)
        assertNotNull(bpObs.identifier)
        assertTrue(bpObs.identifier.size >= 3)
        assertNotNull(bpObs.status)
        assertNotNull(bpObs.code)
        assertNotNull(bpObs.subject?.type?.extension)
        assertEquals("Patient", bpObs.subject?.decomposedType())
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val bpObs = rcdmPatient.rcdmObservationBloodPressure {
            subject of rcdmReference("Patient", "456")
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/456", bpObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") {}
        val bpObs = rcdmPatient.rcdmObservationBloodPressure {
            subject of reference("Patient", "456")
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals("Patient/${rcdmPatient.id?.value}", bpObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient("test") { id of "99" }
        val bpObs = rcdmPatient.rcdmObservationBloodPressure {
            id of "88"
        }
        val validation = roninBloodPressure.validate(bpObs, null)
        assertEquals(validation.hasErrors(), false)
        assertEquals(3, bpObs.identifier.size)
        val values = bpObs.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains("test".asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", bpObs.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", bpObs.subject?.reference?.value)
    }
}
