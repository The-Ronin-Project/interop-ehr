package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.resource.RoninMedication
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninMedicationGeneratorTest {
    private lateinit var roninMedication: RoninMedication
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
        roninMedication = RoninMedication(normalizer, localizer)
    }

    @Test
    fun `example use for roninMedication`() {
        // create appointment resource with attributes you need, provide the tenant
        val roninMedication =
            rcdmMedication("test") {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninMedicationJSON = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninMedication)

        // Uncomment to take a peek at the JSON
        // println(roninMedicationJSON)
        assertNotNull(roninMedicationJSON)
    }

    @Test
    fun `validates rcdm medication`() {
        val medication = rcdmMedication("test") {}
        val validation = roninMedication.validate(medication, null).hasErrors()
        assertFalse(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val medication =
            rcdmMedication("test") {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = roninMedication.validate(medication, null).hasErrors()
        Assertions.assertEquals(validation, false)
        Assertions.assertEquals(4, medication.identifier.size)
        val ids = medication.identifier.map { it.id }.toSet()
        Assertions.assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `generates rcdmMedication with given status but fails validation because status is bad`() {
        val medication =
            rcdmMedication("test") {
                status of Code("this is a bad status")
            }
        Assertions.assertEquals(medication.status, Code("this is a bad status"))

        // validate should fail
        val validation = roninMedication.validate(medication, null)
        Assertions.assertTrue(validation.hasErrors())
        Assertions.assertEquals(validation.issues()[0].code, "INV_VALUE_SET")
        Assertions.assertEquals(
            validation.issues()[0].description,
            "'this is a bad status' is outside of required value set",
        )
        Assertions.assertEquals(
            validation.issues()[0].location,
            LocationContext(element = "Medication", field = "status"),
        )
    }

    @Test
    fun `Incorrect extension URI fails validation`() {
        val medication =
            rcdmMedication("test") {
                extension.plus(
                    Extension(
                        url = Uri("bad uri"),
                        value =
                            DynamicValue(
                                DynamicValueType.CODEABLE_CONCEPT,
                                CodeableConcept(),
                            ),
                    ),
                )
            }

        val validation = roninMedication.validate(medication, null)
        Assertions.assertTrue(validation.hasErrors())
        Assertions.assertEquals(validation.issues().size, 1)
        Assertions.assertEquals(
            validation.issues()[0].code,
            "R4_INV_PRIM",
        )
        Assertions.assertEquals(
            validation.issues()[0].description,
            "Supplied value is not valid for a Uri",
        )
        Assertions.assertEquals(
            validation.issues()[0].location,
            LocationContext(
                element = "Medication",
                field = "extension[0].url",
            ),
        )
    }

    @Test
    fun `Incorrect extension value type fails validation`() {
        val medication =
            rcdmMedication("test") {
                extension of
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.AGE,
                                    Age(),
                                ),
                        ),
                    )
            }

        val validation = roninMedication.validate(medication, null)
        Assertions.assertTrue(validation.hasErrors())
        Assertions.assertEquals(validation.issues().size, 1)
        Assertions.assertEquals(
            validation.issues()[0].code,
            "RONIN_MED_001",
        )
        Assertions.assertEquals(
            validation.issues()[0].description,
            "Tenant source medication code extension is missing or invalid",
        )
        Assertions.assertEquals(
            validation.issues()[0].location,
            LocationContext(
                element = "Medication",
                field = "extension",
            ),
        )
    }
}
