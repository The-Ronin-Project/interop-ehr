package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DosageTest {
    @Test
    fun `fails for unsupported dose dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> { DoseAndRate(dose = DynamicValue(DynamicValueType.INTEGER, 1)) }
        assertEquals("dose can only be one of the following: Range, Quantity", exception.message)
    }

    @Test
    fun `fails for unsupported rate dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> { DoseAndRate(rate = DynamicValue(DynamicValueType.INTEGER, 1)) }
        assertEquals("rate can only be one of the following: Ratio, Range, Quantity", exception.message)
    }

    @Test
    fun `fails for unsupported as needed dynamic value type`() {
        val exception =
            assertThrows<IllegalArgumentException> { Dosage(asNeeded = DynamicValue(DynamicValueType.INTEGER, 1)) }
        assertEquals("asNeeded can only be one of the following: Boolean, CodeableConcept", exception.message)
    }

    @Test
    fun `can serialize and deserialize JSON`() {
        val dosage = Dosage(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.INTEGER, 1)
                )
            ),
            sequence = 2,
            text = "Dosage 2",
            additionalInstruction = listOf(CodeableConcept(text = "additional instruction")),
            patientInstruction = "Take BID",
            timing = Timing(event = listOf(DateTime("2021-12-25"))),
            asNeeded = DynamicValue(DynamicValueType.BOOLEAN, false),
            site = CodeableConcept(text = "dosage site"),
            route = CodeableConcept(text = "dosage route"),
            method = CodeableConcept(text = "dosage method"),
            doseAndRate = listOf(
                DoseAndRate(
                    id = "6789",
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value")
                        )
                    ),
                    type = CodeableConcept(text = "dose and rate type"),
                    dose = DynamicValue(DynamicValueType.QUANTITY, Quantity(value = 3.0)),
                    rate = DynamicValue(DynamicValueType.QUANTITY, Quantity(value = 2.0))
                )
            ),
            maxDosePerPeriod = Ratio(numerator = Quantity(value = 2.0), denominator = Quantity(value = 5.0)),
            maxDosePerAdministration = SimpleQuantity(value = 20.0),
            maxDosePerLifetime = SimpleQuantity(value = 120.0)
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dosage)

        val expectedJson = """
            |{
            |  "id" : "12345",
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueInteger" : 1
            |  } ],
            |  "sequence" : 2,
            |  "text" : "Dosage 2",
            |  "additionalInstruction" : [ {
            |    "text" : "additional instruction"
            |  } ],
            |  "patientInstruction" : "Take BID",
            |  "timing" : {
            |    "event" : [ "2021-12-25" ]
            |  },
            |  "asNeededBoolean" : false,
            |  "site" : {
            |    "text" : "dosage site"
            |  },
            |  "route" : {
            |    "text" : "dosage route"
            |  },
            |  "method" : {
            |    "text" : "dosage method"
            |  },
            |  "doseAndRate" : [ {
            |    "id" : "6789",
            |    "extension" : [ {
            |      "url" : "http://localhost/extension",
            |      "valueString" : "Value"
            |    } ],
            |    "type" : {
            |      "text" : "dose and rate type"
            |    },
            |    "doseQuantity" : {
            |      "value" : 3.0
            |    },
            |    "rateQuantity" : {
            |      "value" : 2.0
            |    }
            |  } ],
            |  "maxDosePerPeriod" : {
            |    "numerator" : {
            |      "value" : 2.0
            |    },
            |    "denominator" : {
            |      "value" : 5.0
            |    }
            |  },
            |  "maxDosePerAdministration" : {
            |    "value" : 20.0
            |  },
            |  "maxDosePerLifetime" : {
            |    "value" : 120.0
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedDosage = objectMapper.readValue<Dosage>(json)
        assertEquals(dosage, deserializedDosage)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val dosage = Dosage(
            doseAndRate = listOf(
                DoseAndRate(
                    type = CodeableConcept(text = "dose and rate type")
                )
            )
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dosage)

        val expectedJson = """
            |{
            |  "doseAndRate" : [ {
            |    "type" : {
            |      "text" : "dose and rate type"
            |    }
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "asNeededBoolean" : true
            |}""".trimMargin()
        val dosage = objectMapper.readValue<Dosage>(json)

        assertNull(dosage.id)
        assertEquals(listOf<Extension>(), dosage.extension)
        assertEquals(listOf<Extension>(), dosage.modifierExtension)
        assertNull(dosage.sequence)
        assertNull(dosage.text)
        assertEquals(listOf<CodeableConcept>(), dosage.additionalInstruction)
        assertNull(dosage.patientInstruction)
        assertNull(dosage.timing)
        assertEquals(DynamicValue(DynamicValueType.BOOLEAN, true), dosage.asNeeded)
        assertNull(dosage.site)
        assertNull(dosage.route)
        assertNull(dosage.method)
        assertEquals(listOf<DoseAndRate>(), dosage.doseAndRate)
        assertNull(dosage.maxDosePerPeriod)
        assertNull(dosage.maxDosePerAdministration)
        assertNull(dosage.maxDosePerLifetime)
    }
}
