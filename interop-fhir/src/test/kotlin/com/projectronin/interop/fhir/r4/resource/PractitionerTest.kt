package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Qualification
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PractitionerTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner")),
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED,
                div = "div"
            ),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","field":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = listOf(Address(country = "USA")),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            qualification = listOf(Qualification(code = CodeableConcept(text = "code"))),
            communication = listOf(CodeableConcept(text = "communication"))
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitioner)

        val expectedJson = """
            |{
            |  "resourceType" : "Practitioner",
            |  "id" : "12345",
            |  "meta" : {
            |    "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
            |  },
            |  "implicitRules" : "implicit-rules",
            |  "language" : "en-US",
            |  "text" : {
            |    "status" : "generated",
            |    "div" : "div"
            |  },
            |  "contained" : [ {"resourceType":"Banana","field":"24680"} ],
            |  "extension" : [ {
            |    "url" : "http://localhost/extension",
            |    "valueString" : "Value"
            |  } ],
            |  "modifierExtension" : [ {
            |    "url" : "http://localhost/modifier-extension",
            |    "valueString" : "Value"
            |  } ],
            |  "identifier" : [ {
            |    "value" : "id"
            |  } ],
            |  "active" : true,
            |  "name" : [ {
            |    "family" : "Doe"
            |  } ],
            |  "telecom" : [ {
            |    "value" : "8675309"
            |  } ],
            |  "address" : [ {
            |    "country" : "USA"
            |  } ],
            |  "gender" : "female",
            |  "birthDate" : "1975-07-05",
            |  "photo" : [ {
            |    "contentType" : "text",
            |    "data" : "abcd"
            |  } ],
            |  "qualification" : [ {
            |    "code" : {
            |      "text" : "code"
            |    }
            |  } ],
            |  "communication" : [ {
            |    "text" : "communication"
            |  } ]
            |}""".trimMargin()
        assertEquals(expectedJson, json)

        val deserializedPractitioner = objectMapper.readValue<Practitioner>(json)
        assertEquals(practitioner, deserializedPractitioner)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val practitioner = Practitioner(
            birthDate = Date("1936-12-25")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(practitioner)

        val expectedJson = """
            |{
            |  "resourceType" : "Practitioner",
            |  "birthDate" : "1936-12-25"
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            |{
            |  "resourceType" : "Practitioner",
            |  "active" : true
            |}""".trimMargin()
        val practitioner = objectMapper.readValue<Practitioner>(json)

        assertNull(practitioner.id)
        assertNull(practitioner.meta)
        assertNull(practitioner.implicitRules)
        assertNull(practitioner.language)
        assertNull(practitioner.text)
        assertEquals(listOf<Resource>(), practitioner.contained)
        assertEquals(listOf<Extension>(), practitioner.extension)
        assertEquals(listOf<Extension>(), practitioner.modifierExtension)
        assertEquals(listOf<Identifier>(), practitioner.identifier)
        assertEquals(true, practitioner.active)
        assertEquals(listOf<Identifier>(), practitioner.name)
        assertEquals(listOf<ContactPoint>(), practitioner.telecom)
        assertEquals(listOf<Address>(), practitioner.address)
        assertNull(practitioner.gender)
        assertNull(practitioner.birthDate)
        assertEquals(listOf<Attachment>(), practitioner.photo)
        assertEquals(listOf<Qualification>(), practitioner.qualification)
        assertEquals(listOf<CodeableConcept>(), practitioner.communication)
    }
}
