package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.RoninMedicationAdministration
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
        roninMedicationAdministration = RoninMedicationAdministration(normalizer, localizer)
    }

    @Test
    fun `example use for roninMedicationAdministration`() {
        // create medicationAdministration resource with attributes you need, provide the tenant
        val roninMedicationAdministration = rcdmMedicationAdministration("test") {
            // to test an attribute like status - provide the value
            status of Code("testing-this-status")
        }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninMedicationAdministrationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninMedicationAdministration)

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
    fun `Incorrect extension URI fails validation`() {
        val medicationAdministration = rcdmMedicationAdministration("test") {
            extension.plus(
                Extension(
                    url = Uri("bad uri"),
                    value = DynamicValue(
                        DynamicValueType.CODE,
                        possibleMedicationDatatypeCodes.random()
                    )
                )
            )
        }

        val roninMedicationAdministrationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(medicationAdministration)

        println(roninMedicationAdministrationJSON)

        val validation = roninMedicationAdministration.validate(medicationAdministration, null)
        assertTrue(validation.hasErrors())
        println(validation.issues())
        assertEquals(2, validation.issues().size)
        assertEquals(
            "RONIN_MEDADMIN_002",
            validation.issues()[0].code
        )
        assertEquals(
            "Medication Administration extension must contain original Medication Datatype",
            validation.issues()[0].description
        )
        assertEquals(
            "R4_INV_PRIM",
            validation.issues()[1].code

        )
        assertEquals(
            "Supplied value is not valid for a Uri",
            validation.issues()[1].description

        )
    }

    @Test
    fun `Incorrect extension type fails validation`() {
        val medicationAdministration = rcdmMedicationAdministration("test") {
            extension of listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/originalMedicationDatatype"),
                    value = DynamicValue(
                        DynamicValueType.AGE,
                        possibleMedicationDatatypeCodes.random()
                    )
                )
            )
        }

        val validation = roninMedicationAdministration.validate(medicationAdministration, null)
        assertTrue(validation.hasErrors())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "RONIN_MEDADMIN_004",
            validation.issues()[0].code
        )
        assertEquals(
            "Medication Administration extension type is invalid",
            validation.issues()[0].description
        )
    }

    @Test
    fun `Incorrect extension value fails validation`() {
        val medicationAdministration = rcdmMedicationAdministration("test") {
            extension of listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/originalMedicationDatatype"),
                    value = DynamicValue(
                        DynamicValueType.CODE,
                        Coding(
                            code = Code("invalid-code")
                        )
                    )
                )
            )
        }

        val validation = roninMedicationAdministration.validate(medicationAdministration, null)
        assertTrue(validation.hasErrors())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "RONIN_MEDADMIN_003",
            validation.issues()[0].code
        )
        assertEquals(
            "Medication Administration extension value is invalid",
            validation.issues()[0].description
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
