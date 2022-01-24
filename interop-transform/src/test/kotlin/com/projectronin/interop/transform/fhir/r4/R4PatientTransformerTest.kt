package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Communication
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
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class R4PatientTransformerTest {
    private val transformer = R4PatientTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `transforms patient with all attributes`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "meta" : {
                "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-patient" ]
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)

        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-patient"))),
            oncologyPatient.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPatient.implicitRules)
        assertEquals(Code("en-US"), oncologyPatient.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyPatient.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyPatient.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPatient.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPatient.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "MRN"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "fhirId"
                ),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPatient.identifier
        )
        assertEquals(true, oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(
            listOf(ContactPoint(value = "8675309", system = ContactPointSystem.PHONE, use = ContactPointUse.MOBILE)),
            oncologyPatient.telecom
        )
        assertEquals(AdministrativeGender.FEMALE, oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
        assertEquals(DynamicValue(type = DynamicValueType.BOOLEAN, value = false), oncologyPatient.deceased)
        assertEquals(listOf(Address(country = "USA")), oncologyPatient.address)
        assertEquals(CodeableConcept(text = "M"), oncologyPatient.maritalStatus)
        assertEquals(DynamicValue(type = DynamicValueType.INTEGER, value = 2), oncologyPatient.multipleBirth)
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            oncologyPatient.photo
        )
        assertEquals(listOf(Communication(language = CodeableConcept(text = "English"))), oncologyPatient.communication)
        assertEquals(listOf(Reference(display = "GP")), oncologyPatient.generalPractitioner)
        assertEquals(Reference(display = "organization"), oncologyPatient.managingOrganization)
        assertEquals(listOf(Link(other = Reference(), type = LinkType.REPLACES)), oncologyPatient.link)
    }

    @Test
    fun `transforms patient with only required attributes`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)

        oncologyPatient!! // Force it to be treated as non-null
        assertEquals("Patient", oncologyPatient.resourceType)
        assertEquals(Id("test-12345"), oncologyPatient.id)
        assertNull(oncologyPatient.meta)
        assertNull(oncologyPatient.implicitRules)
        assertNull(oncologyPatient.language)
        assertNull(oncologyPatient.text)
        assertEquals(listOf<ContainedResource>(), oncologyPatient.contained)
        assertEquals(listOf<Extension>(), oncologyPatient.extension)
        assertEquals(listOf<Extension>(), oncologyPatient.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "MRN"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "fhirId"
                ),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPatient.identifier
        )
        assertNull(oncologyPatient.active)
        assertEquals(listOf(HumanName(family = "Doe")), oncologyPatient.name)
        assertEquals(
            listOf(ContactPoint(value = "8675309", system = ContactPointSystem.PHONE, use = ContactPointUse.MOBILE)),
            oncologyPatient.telecom
        )
        assertEquals(AdministrativeGender.FEMALE, oncologyPatient.gender)
        assertEquals(Date("1975-07-05"), oncologyPatient.birthDate)
        assertNull(oncologyPatient.deceased)
        assertEquals(listOf(Address(country = "USA")), oncologyPatient.address)
        assertEquals(CodeableConcept(text = "M"), oncologyPatient.maritalStatus)
        assertNull(oncologyPatient.multipleBirth)
        assertEquals(listOf<Attachment>(), oncologyPatient.photo)
        assertEquals(listOf<Communication>(), oncologyPatient.communication)
        assertEquals(listOf<Reference>(), oncologyPatient.generalPractitioner)
        assertNull(oncologyPatient.managingOrganization)
        assertEquals(listOf<Link>(), oncologyPatient.link)
    }

    @Test
    fun `non R4 patient`() {
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPatient(patient, tenant)
        }

        assertEquals("Patient is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for patient with missing id`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "identifier" : [ {
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing mrn`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for bad mrn CodeableConcept`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
                "type" : {
                  "coding" : [ {
                    "system" : "ABCD",
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing mrn value`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
                "type" : {
                  "coding" : [ {
                    "system" : "http://projectronin.com/id/mrn",
                    "code" : "MR",
                    "display" : "Medical Record Number"
                  } ],
                  "text" : "MRN"
                },
                "system" : "http://projectronin.com/id/mrn"
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing fhir stu3 id`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing CodeableConcept in fhir stu3 id`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
                "system" : "BADURL",
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing value for fhir stu3 id`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
                "system" : "http://projectronin.com/id/fhir"
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for no name`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              "name" : [ ],
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for no telecom`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for telecom with missing details`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
                "value" : "8675309"
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
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing address`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              "address" : [ ],
              "maritalStatus" : {
                "text" : "M"
              }
            }
        """.trimIndent()
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for contact with missing details`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              },
              "contact" : [ { } ]
            }
        """.trimIndent()
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing gender`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              "birthDate" : "1975-07-05",
              "address" : [ {
                "country" : "USA"
              } ],
              "maritalStatus" : {
                "text" : "M"
              }
            }
        """.trimIndent()
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing birthDate`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              "address" : [ {
                "country" : "USA"
              } ],
              "maritalStatus" : {
                "text" : "M"
              }
            }
        """.trimIndent()
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `fails for missing maritalStatus`() {
        val patientJson = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
              } ]
            }
        """.trimIndent()
        val patient = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patientJson
        }

        val oncologyPatient = transformer.transformPatient(patient, tenant)
        assertNull(oncologyPatient)
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPatients(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }
        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(0, oncologyPatients.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }
        val patient2Json = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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

        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patient2Json
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(1, oncologyPatients.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val patient1Json = """
            {
              "resourceType" : "Patient",
              "id" : "12345",
              "identifier" : [ {
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
        val patient1 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patient1Json
        }

        val patient2Json = """
            {
              "resourceType" : "Patient",
              "id" : "67890",
              "identifier" : [ {
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

        val patient2 = mockk<Patient> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns patient2Json
        }

        val bundle = mockk<Bundle<Patient>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(patient1, patient2)
        }

        val oncologyPatients = transformer.transformPatients(bundle, tenant)
        assertEquals(2, oncologyPatients.size)
    }
}
