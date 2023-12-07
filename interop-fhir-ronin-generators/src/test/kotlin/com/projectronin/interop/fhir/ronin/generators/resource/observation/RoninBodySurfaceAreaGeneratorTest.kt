package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
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
import com.projectronin.interop.fhir.ronin.resource.observation.RoninBodySurfaceArea
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninBodySurfaceAreaGeneratorTest {
    private lateinit var roninBodySurfaceArea: RoninBodySurfaceArea
    private lateinit var registry: NormalizationRegistryClient
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
        registry =
            mockk<NormalizationRegistryClient> {
                every {
                    getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_SURFACE_AREA.value)
                } returns possibleBodySurfaceAreaCodes
            }
        roninBodySurfaceArea = RoninBodySurfaceArea(normalizer, localizer, registry)
    }

    @Test
    fun `example use for roninObservationBodySurfaceArea`() {
        // Create BodySurfaceArea Obs with attributes you need, provide the tenant
        val roninObsBodySurfaceArea =
            rcdmObservationBodySurfaceArea("test") {
                // if you want to test for a specific status
                status of Code("registered")
                // test for a new or different code
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "http://loinc.org"
                                    version of "0.01"
                                    code of Code("123456")
                                    display of "New Body Surface code"
                                },
                            )
                        text of "New Body Surface code" // text is kept if provided otherwise only a code.coding is generated
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodySurfaceAreaJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodySurfaceArea)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodySurfaceAreaJSON)
        assertNotNull(roninObsBodySurfaceAreaJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBodySurfaceArea - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient("test") {}
        val roninObsBodySurfaceArea = rcdmPatient.rcdmObservationBodySurfaceArea {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBodySurfaceAreaJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBodySurfaceArea)

        // Uncomment to take a peek at the JSON
        // println(roninObsBodySurfaceAreaJSON)
        assertNotNull(roninObsBodySurfaceAreaJSON)
        assertNotNull(roninObsBodySurfaceArea.meta)
        assertEquals(
            roninObsBodySurfaceArea.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_SURFACE_AREA.value,
        )
        assertNotNull(roninObsBodySurfaceArea.status)
        assertEquals(1, roninObsBodySurfaceArea.category.size)
        assertNotNull(roninObsBodySurfaceArea.code)
        assertNotNull(roninObsBodySurfaceArea.subject)
        assertNotNull(roninObsBodySurfaceArea.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBodySurfaceArea.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBodySurfaceArea.category[0].coding[0].system)
        assertNotNull(roninObsBodySurfaceArea.status)
        assertNotNull(roninObsBodySurfaceArea.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBodySurfaceArea.id)
        val patientFHIRId =
            roninObsBodySurfaceArea.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsBodySurfaceArea.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBodySurfaceArea.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBodySurfaceArea Observation`() {
        val roninObsSurfaceArea = rcdmObservationBodySurfaceArea("test") {}
        assertNotNull(roninObsSurfaceArea.id)
        assertNotNull(roninObsSurfaceArea.meta)
        assertEquals(
            roninObsSurfaceArea.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BODY_SURFACE_AREA.value,
        )
        assertNull(roninObsSurfaceArea.implicitRules)
        assertNull(roninObsSurfaceArea.language)
        assertNull(roninObsSurfaceArea.text)
        assertEquals(0, roninObsSurfaceArea.contained.size)
        assertEquals(1, roninObsSurfaceArea.extension.size)
        assertEquals(0, roninObsSurfaceArea.modifierExtension.size)
        assertTrue(roninObsSurfaceArea.identifier.size >= 3)
        assertTrue(roninObsSurfaceArea.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsSurfaceArea.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsSurfaceArea.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsSurfaceArea.status)
        assertEquals(1, roninObsSurfaceArea.category.size)
        assertNotNull(roninObsSurfaceArea.code)
        assertNotNull(roninObsSurfaceArea.subject)
        assertNotNull(roninObsSurfaceArea.subject?.type?.extension)
        assertEquals("vital-signs", roninObsSurfaceArea.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsSurfaceArea.category[0].coding[0].system)
        assertNotNull(roninObsSurfaceArea.status)
        assertNotNull(roninObsSurfaceArea.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for rcdmObservationBodySurfaceArea`() {
        val bodySurfaceArea = rcdmObservationBodySurfaceArea("test") {}
        val validation = roninBodySurfaceArea.validate(bodySurfaceArea, null)
        validation.alertIfErrors()
    }

    @Test
    fun `validation for rcdmObservationBodySurfaceArea with existing identifier`() {
        val bodySurfaceArea =
            rcdmObservationBodySurfaceArea("test") {
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
            }
        val validation = roninBodySurfaceArea.validate(bodySurfaceArea, null)
        validation.alertIfErrors()
        assertNotNull(bodySurfaceArea.meta)
        assertNotNull(bodySurfaceArea.identifier)
        assertEquals(4, bodySurfaceArea.identifier.size)
        assertNotNull(bodySurfaceArea.status)
        assertNotNull(bodySurfaceArea.code)
        assertNotNull(bodySurfaceArea.subject)
    }

    @Test
    fun `validation fails for rcdmObservationBodySurfaceArea with bad code`() {
        val bodySurfaceArea =
            rcdmObservationBodySurfaceArea("test") {
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "something here"
                                    version of "1000000"
                                    code of Code("some code here")
                                },
                            )
                    }
            }
        val validation = roninBodySurfaceArea.validate(bodySurfaceArea, null)
        assertTrue(validation.hasErrors())

        val issueCodes = validation.issues().map { it.code }.toSet()
        assertEquals(setOf("RONIN_OBS_003", "R4_INV_PRIM"), issueCodes)
    }
}
