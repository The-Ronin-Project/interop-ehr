package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Link
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PatientTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val patient = Patient(
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
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
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
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M"),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe"))),
            communication = listOf(Communication(language = CodeableConcept(text = "English"))),
            generalPractitioner = listOf(Reference(display = "GP")),
            managingOrganization = Reference(display = "organization"),
            link = listOf(Link(other = Reference(), type = LinkType.REPLACES))
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patient)

        val expectedJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "meta" : {
                "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitioner" ]
              },
              "implicitRules" : "implicit-rules",
              "language" : "en-US",
              "text" : {
                "status" : "generated",
                "div" : "div"
              },
              "contained" : [ {"resourceType":"Banana","id":"24680"} ],
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "identifier" : [ {
                "value" : "id"
              } ],
              "active" : true,
              "name" : [ {
                "family" : "Doe"
              } ],
              "telecom" : [ {
                "value" : "8675309"
              } ],
              "gender" : "female",
              "birthDate" : "1975-07-05",
              "deceasedBoolean" : false,
              "address" : [ {
                "country" : "USA"
              } ],
              "maritalStatus" : {
                "text" : "M"
              },
              "multipleBirthInteger" : 2,
              "photo" : [ {
                "contentType" : "text",
                "data" : "abcd"
              } ],
              "contact" : [ {
                "name" : {
                  "text" : "Jane Doe"
                }
              } ],
              "communication" : [ {
                "language" : {
                  "text" : "English"
                }
              } ],
              "generalPractitioner" : [ {
                "display" : "GP"
              } ],
              "managingOrganization" : {
                "display" : "organization"
              },
              "link" : [ {
                "other" : { },
                "type" : "replaces"
              } ]
            }
        """.trimIndent()
        assertEquals(expectedJson, json)

        val deserializedPatient = JacksonManager.objectMapper.readValue<Patient>(json)
        assertEquals(patient, deserializedPatient)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val patient = Patient()
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(patient)

        val expectedJson = """
            {
              "resourceType" : "Patient"
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "resourceType" : "Patient"
            }
        """.trimIndent()
        val patient = JacksonManager.objectMapper.readValue<Patient>(json)

        assertNull(patient.id)
        assertNull(patient.meta)
        assertNull(patient.implicitRules)
        assertNull(patient.language)
        assertNull(patient.text)
        assertEquals(listOf<ContainedResource>(), patient.contained)
        assertEquals(listOf<Extension>(), patient.extension)
        assertEquals(listOf<Extension>(), patient.modifierExtension)
        assertEquals(listOf<Identifier>(), patient.identifier)
        assertNull(patient.active)
        assertEquals(listOf<HumanName>(), patient.name)
        assertEquals(listOf<ContactPoint>(), patient.telecom)
        assertNull(patient.gender)
        assertNull(patient.birthDate)
        assertNull(patient.deceased)
        assertEquals(listOf<Address>(), patient.address)
        assertNull(patient.maritalStatus)
        assertNull(patient.multipleBirth)
        assertEquals(listOf<Attachment>(), patient.photo)
        assertEquals(listOf<Contact>(), patient.contact)
        assertEquals(listOf<Communication>(), patient.communication)
        assertEquals(listOf<Reference>(), patient.generalPractitioner)
        assertNull(patient.managingOrganization)
        assertEquals(listOf<Link>(), patient.link)
    }

    @Test
    fun `cannot create dynamic values with bad types`() {
        // deceased must be Boolean or Date Time
        val deceasedException = assertThrows<IllegalArgumentException> {
            Patient(
                deceased = DynamicValue(type = DynamicValueType.BASE_64_BINARY, value = false)
            )
        }
        assertEquals("Bad dynamic value indicating if the patient is deceased", deceasedException.message)

        // multipleBirth must be Boolean or Integer
        val multipleBirthException = assertThrows<IllegalArgumentException> {
            Patient(
                multipleBirth = DynamicValue(type = DynamicValueType.BASE_64_BINARY, value = 2)
            )
        }
        assertEquals(
            "Bad dynamic value indicating whether the patient was part of a multiple birth",
            multipleBirthException.message
        )
    }

    @Test
    fun `cannot create a contact without details`() {
        // Either the name, telecom, address, or organization must be present
        val exception = assertThrows<IllegalArgumentException> {
            Patient(
                contact = listOf(Contact())
            )
        }
        assertEquals(
            "[pat-1](https://www.hl7.org/fhir/R4/patient.html#invs): contact SHALL at least contain a contact's details or a reference to an organization.",
            exception.message
        )
    }
}
