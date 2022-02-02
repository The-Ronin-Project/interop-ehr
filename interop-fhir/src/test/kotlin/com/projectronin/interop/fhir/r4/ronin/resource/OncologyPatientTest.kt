package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OncologyPatientTest {
    @Test
    fun `can serialize and deserialize JSON`() {
        val oncologyPatient = OncologyPatient(
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
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
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
            link = listOf(PatientLink(other = Reference(), type = LinkType.REPLACES))
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(oncologyPatient)

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
              "contained" : [ {"resourceType":"Banana","field":"24680"} ],
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "identifier" : [ {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/tenantId",
                    "code" : "TID",
                    "display" : "Ronin-specified Tenant Identifier"
                  } ],
                  "text" : "Tenant ID"
                },
                "system" : "http://projectronin.com/id/tenantId",
                "value" : "tenantId"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/mrn",
                    "code" : "MR",
                    "display" : "Medical Record Number"
                  } ],
                  "text" : "MRN"
                },
                "system" : "http://projectronin.com/id/mrn",
                "value" : "MRN"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/fhir",
                    "code" : "STU3",
                    "display" : "FHIR STU3 ID"
                  } ],
                  "text" : "FHIR STU3"
                },
                "system" : "http://projectronin.com/id/fhir",
                "value" : "fhirId"
              } ],
              "active" : true,
              "name" : [ {
                "family" : "Doe"
              } ],
              "telecom" : [ {
                "system" : "phone",
                "value" : "8675309",
                "use" : "mobile"
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

        val deserializedOncologyPatient = JacksonManager.objectMapper.readValue<OncologyPatient>(json)
        assertEquals(oncologyPatient, deserializedOncologyPatient)
    }

    @Test
    fun `serialized JSON ignores null and empty fields`() {
        val oncologyPatient = OncologyPatient(
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M")
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(oncologyPatient)

        val expectedJson = """
            {
              "resourceType" : "Patient",
              "identifier" : [ {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/tenantId",
                    "code" : "TID",
                    "display" : "Ronin-specified Tenant Identifier"
                  } ],
                  "text" : "Tenant ID"
                },
                "system" : "http://projectronin.com/id/tenantId",
                "value" : "tenantId"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/mrn",
                    "code" : "MR",
                    "display" : "Medical Record Number"
                  } ],
                  "text" : "MRN"
                },
                "system" : "http://projectronin.com/id/mrn",
                "value" : "MRN"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/fhir",
                    "code" : "STU3",
                    "display" : "FHIR STU3 ID"
                  } ],
                  "text" : "FHIR STU3"
                },
                "system" : "http://projectronin.com/id/fhir",
                "value" : "fhirId"
              } ],
              "name" : [ {
                "family" : "Doe"
              } ],
              "telecom" : [ {
                "system" : "phone",
                "value" : "8675309",
                "use" : "mobile"
              } ],
              "gender" : "female",
              "birthDate" : "1975-07-05",
              "address" : [ {
                "country" : "USA"
              } ],
              "maritalStatus" : {
                "text" : "M"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can deserialize from JSON with nullable and empty fields`() {
        val json = """
            {
              "resourceType" : "Patient",
              "identifier" : [ {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/tenantId",
                    "code" : "TID",
                    "display" : "Ronin-specified Tenant Identifier"
                  } ],
                  "text" : "Tenant ID"
                },
                "system" : "http://projectronin.com/id/tenantId",
                "value" : "tenantId"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/mrn",
                    "code" : "MR",
                    "display" : "Medical Record Number"
                  } ],
                  "text" : "MRN"
                },
                "system" : "http://projectronin.com/id/mrn",
                "value" : "MRN"
              }, {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/fhir",
                    "code" : "STU3",
                    "display" : "FHIR STU3 ID"
                  } ],
                  "text" : "FHIR STU3"
                },
                "system" : "http://projectronin.com/id/fhir",
                "value" : "fhirId"
              } ],
              "name" : [ {
                "family" : "Doe"
              } ],
              "telecom" : [ {
                "system" : "phone",
                "value" : "8675309",
                "use" : "mobile"
              } ],
              "gender" : "female",
              "birthDate" : "1975-07-05",
              "address" : [ {
                "country" : "USA"
              } ],
              "maritalStatus" : {
                "text" : "M"
              }
            }
        """.trimIndent()
        val oncologyPatient = JacksonManager.objectMapper.readValue<OncologyPatient>(json)

        assertNull(oncologyPatient.id)
        assertNull(oncologyPatient.meta)
        assertNull(oncologyPatient.implicitRules)
        assertNull(oncologyPatient.language)
        assertNull(oncologyPatient.text)
        assertEquals(listOf<RoninResource>(), oncologyPatient.contained)
        assertEquals(listOf<Extension>(), oncologyPatient.extension)
        assertEquals(listOf<Extension>(), oncologyPatient.modifierExtension)
        assertNull(oncologyPatient.active)
        assertNull(oncologyPatient.deceased)
        assertNull(oncologyPatient.multipleBirth)
        assertEquals(listOf<Attachment>(), oncologyPatient.photo)
        assertEquals(listOf<Contact>(), oncologyPatient.contact)
        assertEquals(listOf<Communication>(), oncologyPatient.communication)
        assertEquals(listOf<Reference>(), oncologyPatient.generalPractitioner)
        assertNull(oncologyPatient.managingOrganization)
        assertEquals(listOf<PatientLink>(), oncologyPatient.link)
    }

    @Test
    fun `fails if no tenant identifier provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `fails if tenant does not have tenant codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.MRN,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("Tenant identifier provided without proper CodeableConcept defined", exception.message)
    }

    @Test
    fun `fails if tenant does not have value`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("tenant value is required", exception.message)
    }

    @Test
    fun `cannot create dynamic values with bad types`() {
        // deceased must be Boolean or Date Time
        val deceasedException = assertThrows<java.lang.IllegalArgumentException> {
            OncologyPatient(
                deceased = DynamicValue(type = DynamicValueType.BASE_64_BINARY, value = false),
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        }
        assertEquals("Bad dynamic value indicating if the patient is deceased", deceasedException.message)

        // multipleBirth must be Boolean or Integer
        val multipleBirthException = assertThrows<java.lang.IllegalArgumentException> {
            OncologyPatient(
                multipleBirth = DynamicValue(type = DynamicValueType.BASE_64_BINARY, value = 2),
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        type = CodeableConcepts.RONIN_TENANT,
                        value = "tenantId"
                    ),
                    Identifier(
                        system = CodeSystem.FHIR_STU3_ID.uri,
                        type = CodeableConcepts.FHIR_STU3_ID,
                        value = "fhirId"
                    )
                ),
                name = listOf(HumanName(family = "Doe")),
                telecom = listOf(
                    ContactPoint(
                        system = ContactPointSystem.PHONE,
                        value = "8675309",
                        use = ContactPointUse.MOBILE
                    )
                ),
                gender = AdministrativeGender.FEMALE,
                birthDate = Date("1975-07-05"),
                address = listOf(Address(country = "USA")),
                maritalStatus = CodeableConcept(text = "M")
            )
        }
        assertEquals(
            "Bad dynamic value indicating whether the patient was part of a multiple birth",
            multipleBirthException.message
        )
    }

    @Test
    fun `fails if no mrn identifier provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("mrn identifier is required", exception.message)
    }

    @Test
    fun `fails if mrn has a bad codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("mrn identifier type defined without proper CodeableConcept", exception.message)
    }

    @Test
    fun `fails if mrn does not have value`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("mrn value is required", exception.message)
    }

    @Test
    fun `fails if no fhir_stu3_id identifier provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("fhir_stu3_id identifier is required", exception.message)
    }

    @Test
    fun `fails if fhir_stu3_id has a bad codeable concept`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("fhir_stu3_id identifier type defined without proper CodeableConcept", exception.message)
    }

    @Test
    fun `fails if fhir_stu3_id does not have value`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("fhir_stu3_id value is required", exception.message)
    }

    @Test
    fun `fails if no name provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("At least one name must be provided", exception.message)
    }

    @Test
    fun `fails if no telecom provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("At least one telecom must be provided", exception.message)
    }

    @Test
    fun `fails if telecoms do not have use`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309"
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("Telecoms must have a system, value and use", exception.message)
    }

    @Test
    fun `fails if telecoms do not have system`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            use = ContactPointUse.MOBILE,
                            value = "8675309"
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("Telecoms must have a system, value and use", exception.message)
    }

    @Test
    fun `fails if telecoms do not have value`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            use = ContactPointUse.MOBILE,
                            system = ContactPointSystem.PHONE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("Telecoms must have a system, value and use", exception.message)
    }

    @Test
    fun `fails if no address provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(),
                    maritalStatus = CodeableConcept(text = "M")
                )
            }
        assertEquals("At least one address must be provided", exception.message)
    }

    @Test
    fun `fails if contacts do not have details`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                OncologyPatient(
                    identifier = listOf(
                        Identifier(
                            system = CodeSystem.RONIN_TENANT.uri,
                            type = CodeableConcepts.RONIN_TENANT,
                            value = "tenantId"
                        ),
                        Identifier(
                            system = CodeSystem.MRN.uri,
                            type = CodeableConcepts.MRN,
                            value = "MRN"
                        ),
                        Identifier(
                            system = CodeSystem.FHIR_STU3_ID.uri,
                            type = CodeableConcepts.FHIR_STU3_ID,
                            value = "fhirId"
                        )
                    ),
                    name = listOf(HumanName(family = "Doe")),
                    telecom = listOf(
                        ContactPoint(
                            system = ContactPointSystem.PHONE,
                            value = "8675309",
                            use = ContactPointUse.MOBILE
                        )
                    ),
                    gender = AdministrativeGender.FEMALE,
                    birthDate = Date("1975-07-05"),
                    address = listOf(Address(country = "USA")),
                    maritalStatus = CodeableConcept(text = "M"),
                    contact = listOf(Contact()),
                )
            }
        assertEquals(
            "[pat-1](https://www.hl7.org/fhir/R4/patient.html#invs): contact SHALL at least contain a contact's details or a reference to an organization",
            exception.message
        )
    }
}
