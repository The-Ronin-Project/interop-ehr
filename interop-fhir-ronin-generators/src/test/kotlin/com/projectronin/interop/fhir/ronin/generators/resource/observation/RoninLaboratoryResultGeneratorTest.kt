package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.observation.RoninLaboratoryResult
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninLaboratoryResultGeneratorTest {
    private lateinit var roninLabResult: RoninLaboratoryResult
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
        val registry = mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_LABORATORY_RESULT.value)
            } returns possibleLaboratoryResultCodes
        }
        roninLabResult = RoninLaboratoryResult(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationLaboratoryResult`() {
        // Create LaboratoryResult Obs with attributes you need, provide the tenant
        val roninObsLaboratoryResult = rcdmObservationLaboratoryResult("test") {
            // if you want to test for a specific status
            status of Code("corrected")
            // test for a new or different code
            code of codeableConcept {
                coding of listOf(
                    coding {
                        system of "http://loinc.org"
                        code of Code("89263-8")
                        display of "Special circumstances associated observations panel"
                    }
                )
            }
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsLaboratoryResultJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsLaboratoryResult)

        // Uncomment to take a peek at the JSON
        // println(roninObsLaboratoryResultJSON)
        assertNotNull(roninObsLaboratoryResultJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationLaboratoryResult - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsLaboratoryResult = rcdmPatient.rcdmObservationLaboratoryResult {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsLaboratoryResultJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsLaboratoryResult)

        // Uncomment to take a peek at the JSON
        // println(roninObsLaboratoryResultJSON)
        assertNotNull(roninObsLaboratoryResultJSON)
        assertNotNull(roninObsLaboratoryResult.meta)
        assertEquals(
            roninObsLaboratoryResult.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_LABORATORY_RESULT.value
        )
        assertNotNull(roninObsLaboratoryResult.status)
        assertEquals(1, roninObsLaboratoryResult.category.size)
        assertNotNull(roninObsLaboratoryResult.code)
        assertNotNull(roninObsLaboratoryResult.subject)
        assertNotNull(roninObsLaboratoryResult.subject?.type?.extension)
        assertEquals("laboratory", roninObsLaboratoryResult.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsLaboratoryResult.category[0].coding[0].system)
        assertNotNull(roninObsLaboratoryResult.status)
        assertNotNull(roninObsLaboratoryResult.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsLaboratoryResult.id)
        val patientFHIRId =
            roninObsLaboratoryResult.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsLaboratoryResult.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsLaboratoryResult.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationLaboratoryResult Observation`() {
        val roninObsLabResult = rcdmObservationLaboratoryResult("test") {}
        assertNotNull(roninObsLabResult.id)
        assertNotNull(roninObsLabResult.meta)
        assertEquals(
            roninObsLabResult.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_LABORATORY_RESULT.value
        )
        assertNull(roninObsLabResult.implicitRules)
        assertNull(roninObsLabResult.language)
        assertNull(roninObsLabResult.text)
        assertEquals(0, roninObsLabResult.contained.size)
        assertEquals(1, roninObsLabResult.extension.size)
        assertEquals(0, roninObsLabResult.modifierExtension.size)
        assertTrue(roninObsLabResult.identifier.size >= 3)
        assertTrue(roninObsLabResult.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsLabResult.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsLabResult.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsLabResult.status)
        assertEquals(1, roninObsLabResult.category.size)
        assertNotNull(roninObsLabResult.code)
        assertNotNull(roninObsLabResult.subject)
        assertNotNull(roninObsLabResult.subject?.type?.extension)
        assertEquals("laboratory", roninObsLabResult.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsLabResult.category[0].coding[0].system)
        assertNotNull(roninObsLabResult.status)
        assertNotNull(roninObsLabResult.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationLaboratoryResult`() {
        val roninLab = rcdmObservationLaboratoryResult("test") {}
        val validation = roninLabResult.validate(roninLab, null)
        assertEquals(validation.hasErrors(), false)
    }

    @Test
    fun `validation for roninObservationLaboratoryResult with existing identifier`() {
        val roninLab = rcdmObservationLaboratoryResult("test") {
            identifier of listOf(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR()
                )
            )
        }
        val validation = roninLabResult.validate(roninLab, null)
        assertEquals(validation.hasErrors(), false)
        assertNotNull(roninLab.meta)
        assertNotNull(roninLab.identifier)
        assertEquals(4, roninLab.identifier.size)
        assertNotNull(roninLab.status)
        assertNotNull(roninLab.code)
        assertNotNull(roninLab.subject)
    }

    @Test
    fun `rcdmObservationLaboratoryResult with custom value`() {
        val valueQuantity = DynamicValue(
            DynamicValueType.QUANTITY,
            Quantity(
                value = Decimal(100.toBigDecimal()),
                unit = "mg".asFHIR(),
                system = CodeSystem.UCUM.uri,
                code = Code("mg")
            )
        )

        val roninObsLaboratoryResult = rcdmObservationLaboratoryResult("test") {
            value of valueQuantity
        }

        assertEquals(valueQuantity, roninObsLaboratoryResult.value)
    }
}
