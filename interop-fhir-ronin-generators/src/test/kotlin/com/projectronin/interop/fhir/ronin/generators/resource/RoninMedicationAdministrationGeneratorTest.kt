package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.resource.RoninMedicationAdministration
import com.projectronin.interop.fhir.ronin.resource.extractor.MedicationExtractor
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninMedicationAdministrationGeneratorTest {
    private lateinit var roninMedicationAdministration: RoninMedicationAdministration
    private lateinit var registry: NormalizationRegistryClient
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        registry = mockk()
        val normalizer: Normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        val localizer: Localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        val medicationExtractor = mockk<MedicationExtractor> {
            every { extractMedication(any(), any(), any()) } returns null
        }
        roninMedicationAdministration =
            RoninMedicationAdministration(normalizer, localizer, medicationExtractor, registry)
    }

    @Test
    fun `example use for roninMedicationAdministration`() {
        // create medicationAdministration resource with attributes you need, provide the tenant
        val roninMedicationAdministration = rcdmMedicationAdministration("test") {
            // to test an attribute like status - provide the value
            status of Code("testing-this-status")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninMedicationAdministrationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(roninMedicationAdministration)

        // Uncomment to take a peek at the JSON
        // println(roninMedicationAdministrationJSON)
        assertNotNull(roninMedicationAdministrationJSON)
    }

    @Test
    fun `validates rcdm medication administration`() {
        val medication = rcdmMedicationAdministration("test") {}
        val validation = roninMedicationAdministration.validate(medication, null).hasErrors()
        assertFalse(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val medicationAdministration = rcdmMedicationAdministration("test") {
            identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
        }
        val validation = roninMedicationAdministration.validate(medicationAdministration, null).hasErrors()
        assertEquals(false, validation)
        assertEquals(4, medicationAdministration.identifier.size)
        val ids = medicationAdministration.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `generates rcdmMedicationAdministration with given status but fails validation because status is bad`() {
        val medicationAdministration = rcdmMedicationAdministration("test") {
            status of Code("this is a bad status")
        }

        assertEquals(Code("this is a bad status"), medicationAdministration.status)

        // validate should fail
        val validation = roninMedicationAdministration.validate(medicationAdministration, null)
        assertTrue(validation.hasErrors())
        assertEquals("INV_VALUE_SET", validation.issues()[0].code)
        assertEquals(
            "'this is a bad status' is outside of required value set",
            validation.issues()[0].description
        )
        assertEquals(
            LocationContext(element = "MedicationAdministration", field = "status"),
            validation.issues()[0].location
        )
    }

    @Test
    fun `rcdmPatient rcdmMedicationAdministration validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val medicationAdministration = rcdmPatient.rcdmMedicationAdministration {}
        assertEquals("Patient/${rcdmPatient.id?.value}", medicationAdministration.subject?.reference?.value)
        val validation = roninMedicationAdministration.validate(medicationAdministration, null)
        assertEquals(false, validation.hasErrors())
    }
}
