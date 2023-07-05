package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodyMassIndex
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

class RoninBodyMassIndexGeneratorTest {
    private lateinit var roninBodyMassIndex: RoninBodyMassIndex
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val bodyMassIndexCodes = possibleBodyMassIndexCodes

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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_MASS_INDEX.value)
            } returns bodyMassIndexCodes
        }
        roninBodyMassIndex = RoninBodyMassIndex(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationBodyMassIndex`() {
        // Create BodyMassIndex Obs with attributes you need, provide the tenant
        val roninObsBodyMassIndex = rcdmObservationBodyMassIndex("test") {
            status of Code("new-status")
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        code of Code("55418-8")
                        display of "Weight and Height tracking panel"
                    }
                )
                text of "Weight and Height tracking panel" // text is kept if provided otherwise only a code.coding is generated
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyMassIndexJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyMassIndex)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyMassIndexJSON)
        assertNotNull(roninObsBodyMassIndexJSON)
    }

    @Test
    fun `example use for rcdmPatient roninObservationBodyMassIndex - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsBodyMassIndex = rcdmPatient.rcdmObservationBodyMassIndex {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodyMassIndexJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodyMassIndex)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodyMassIndexJSON)
        assertNotNull(roninObsBodyMassIndexJSON)
        assertNotNull(roninObsBodyMassIndex.meta)
        assertEquals(
            roninObsBodyMassIndex.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_MASS_INDEX.value
        )
        assertNotNull(roninObsBodyMassIndex.status)
        assertEquals(1, roninObsBodyMassIndex.category.size)
        assertNotNull(roninObsBodyMassIndex.code)
        assertNotNull(roninObsBodyMassIndex.subject)
        assertNotNull(roninObsBodyMassIndex.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyMassIndex.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyMassIndex.category[0].coding[0].system)
        assertNotNull(roninObsBodyMassIndex.status)
        assertNotNull(roninObsBodyMassIndex.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBodyMassIndex.id)
        val patientFHIRId = roninObsBodyMassIndex.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninObsBodyMassIndex.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBodyMassIndex.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBodyMassIndex Observation`() {
        val roninObsBodyMassIndex = rcdmObservationBodyMassIndex("test") {}
        assertNotNull(roninObsBodyMassIndex.id)
        assertNotNull(roninObsBodyMassIndex.meta)
        assertEquals(
            roninObsBodyMassIndex.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_MASS_INDEX.value
        )
        assertNull(roninObsBodyMassIndex.implicitRules)
        assertNull(roninObsBodyMassIndex.language)
        assertNull(roninObsBodyMassIndex.text)
        assertEquals(0, roninObsBodyMassIndex.contained.size)
        assertEquals(1, roninObsBodyMassIndex.extension.size)
        assertEquals(0, roninObsBodyMassIndex.modifierExtension.size)
        assertTrue(roninObsBodyMassIndex.identifier.size >= 3)
        assertTrue(roninObsBodyMassIndex.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsBodyMassIndex.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsBodyMassIndex.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsBodyMassIndex.status)
        assertEquals(1, roninObsBodyMassIndex.category.size)
        assertNotNull(roninObsBodyMassIndex.code)
        assertNotNull(roninObsBodyMassIndex.subject)
        assertNotNull(roninObsBodyMassIndex.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodyMassIndex.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodyMassIndex.category[0].coding[0].system)
        assertNotNull(roninObsBodyMassIndex.status)
        assertNotNull(roninObsBodyMassIndex.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates rcdmObservationBodyMassIndex generator`() {
        val roninObsBodyMassIndex = rcdmObservationBodyMassIndex("test") {}
        val validation = roninBodyMassIndex.validate(roninObsBodyMassIndex, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validates rcdmObservationBodyMassIndex with provided identifier`() {
        val roninObsBodyMassIndex = rcdmObservationBodyMassIndex("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninBodyMassIndex.validate(roninObsBodyMassIndex, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(roninObsBodyMassIndex.meta)
        assertNotNull(roninObsBodyMassIndex.identifier)
        assertEquals(4, roninObsBodyMassIndex.identifier.size)
        assertNotNull(roninObsBodyMassIndex.status)
        assertNotNull(roninObsBodyMassIndex.code)
        assertNotNull(roninObsBodyMassIndex.subject)
    }

    @Test
    fun `validates rcdmObservationBodyMassIndex fails with bad status`() {
        val roninObsBodyMassIndex = rcdmObservationBodyMassIndex("test") {
            status of Code("fake-status")
        }
        val validation = roninBodyMassIndex.validate(roninObsBodyMassIndex, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals(validation.issues()[0].code, "INV_VALUE_SET")
        assertEquals(validation.issues()[0].description, "'fake-status' is outside of required value set")
        assertEquals(validation.issues()[0].location, LocationContext(element = "Observation", field = "status"))
    }
}
