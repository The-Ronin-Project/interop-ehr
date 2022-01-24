package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class R4PractitionerRoleTransformerTest {
    private val transformer = R4PractitionerRoleTransformer()
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `non R4 practitioner role`() {
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPractitionerRole(practitionerRole, tenant)
        }

        assertEquals("PractitionerRole is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for practitioner role with no ID`() {
        val practitionerRoleJson = "{}"
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)
        assertNull(oncologyPractitionerRole)
    }

    @Test
    fun `fails for practitioner role with no practitioner`() {
        val practitionerRoleJson = """{
            |  "id" : "1234",
            |  "organization" : {
            |    "reference" : "Organization/1234"
            |  }
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)
        assertNull(oncologyPractitionerRole)
    }

    @Test
    fun `fails for practitioner role with no organization`() {
        val practitionerRoleJson = """{
            |  "id" : "1234",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  }
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)
        assertNull(oncologyPractitionerRole)
    }

    @Test
    fun `transforms practitioner role with all attributes`() {
        val practitionerRoleJson = """{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "meta" : {
            |    "profile" : [ "http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole" ]
            |  },
            |  "implicitRules" : "implicit-rules",
            |  "language" : "en-US",
            |  "text" : {
            |    "status" : "generated",
            |    "div" : "div"
            |  },
            |  "contained" : [ {"resourceType":"Banana","id":"24680"} ],
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
            |  "period" : {
            |    "end" : "2022"
            |  },
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  },
            |  "code" : [ {
            |    "text" : "code"
            |  } ],
            |  "specialty" : [ {
            |    "text" : "specialty"
            |  } ],
            |  "location" : [ {
            |    "reference" : "Location/9012"
            |  } ],
            |  "healthcareService" : [ {
            |    "reference" : "HealthcareService/3456"
            |  } ],
            |  "telecom" : [ {
            |    "system" : "phone",
            |    "value" : "8675309"
            |  } ],
            |  "availableTime" : [ {
            |    "allDay" : false
            |  } ],
            |  "notAvailable" : [ {
            |    "description" : "Not available now"
            |  } ],
            |  "availabilityExceptions" : "exceptions",
            |  "endpoint" : [ {
            |    "reference" : "Endpoint/1357"
            |  } ]
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)

        oncologyPractitionerRole!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", oncologyPractitionerRole.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitionerRole.id)
        assertEquals(
            Meta(profile = listOf(Canonical("http://projectronin.com/fhir/us/ronin/StructureDefinition/oncology-practitionerrole"))),
            oncologyPractitionerRole.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyPractitionerRole.implicitRules)
        assertEquals(Code("en-US"), oncologyPractitionerRole.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyPractitionerRole.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyPractitionerRole.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPractitionerRole.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyPractitionerRole.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitionerRole.identifier
        )
        assertEquals(true, oncologyPractitionerRole.active)
        assertEquals(Period(end = DateTime("2022")), oncologyPractitionerRole.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), oncologyPractitionerRole.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), oncologyPractitionerRole.organization)
        assertEquals(listOf(CodeableConcept(text = "code")), oncologyPractitionerRole.code)
        assertEquals(listOf(CodeableConcept(text = "specialty")), oncologyPractitionerRole.specialty)
        assertEquals(listOf(Reference(reference = "Location/test-9012")), oncologyPractitionerRole.location)
        assertEquals(
            listOf(Reference(reference = "HealthcareService/test-3456")),
            oncologyPractitionerRole.healthcareService
        )
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
            oncologyPractitionerRole.telecom
        )
        assertEquals(listOf(AvailableTime(allDay = false)), oncologyPractitionerRole.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now")), oncologyPractitionerRole.notAvailable)
        assertEquals("exceptions", oncologyPractitionerRole.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/test-1357")), oncologyPractitionerRole.endpoint)
    }

    @Test
    fun `transforms practitioner role with only required attributes`() {
        val practitionerRoleJson = """{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  }
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)

        oncologyPractitionerRole!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", oncologyPractitionerRole.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitionerRole.id)
        assertNull(oncologyPractitionerRole.meta)
        assertNull(oncologyPractitionerRole.implicitRules)
        assertNull(oncologyPractitionerRole.language)
        assertNull(oncologyPractitionerRole.text)
        assertEquals(listOf<ContainedResource>(), oncologyPractitionerRole.contained)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.extension)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitionerRole.identifier
        )
        assertNull(oncologyPractitionerRole.active)
        assertNull(oncologyPractitionerRole.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), oncologyPractitionerRole.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), oncologyPractitionerRole.organization)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.code)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.specialty)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.location)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.healthcareService)
        assertEquals(listOf<ContactPoint>(), oncologyPractitionerRole.telecom)
        assertEquals(listOf<AvailableTime>(), oncologyPractitionerRole.availableTime)
        assertEquals(listOf<NotAvailable>(), oncologyPractitionerRole.notAvailable)
        assertNull(oncologyPractitionerRole.availabilityExceptions)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.endpoint)
    }

    @Test
    fun `transforms practitioner role with all telecoms filtered`() {
        val practitionerRoleJson = """{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  },
            |  "telecom" : [ {
            |    "value" : "8675309"
            |  }, {
            |    "system" : "email"
            |  } ]
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)

        oncologyPractitionerRole!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", oncologyPractitionerRole.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitionerRole.id)
        assertNull(oncologyPractitionerRole.meta)
        assertNull(oncologyPractitionerRole.implicitRules)
        assertNull(oncologyPractitionerRole.language)
        assertNull(oncologyPractitionerRole.text)
        assertEquals(listOf<ContainedResource>(), oncologyPractitionerRole.contained)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.extension)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitionerRole.identifier
        )
        assertNull(oncologyPractitionerRole.active)
        assertNull(oncologyPractitionerRole.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), oncologyPractitionerRole.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), oncologyPractitionerRole.organization)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.code)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.specialty)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.location)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.healthcareService)
        assertEquals(listOf<ContactPoint>(), oncologyPractitionerRole.telecom)
        assertEquals(listOf<AvailableTime>(), oncologyPractitionerRole.availableTime)
        assertEquals(listOf<NotAvailable>(), oncologyPractitionerRole.notAvailable)
        assertNull(oncologyPractitionerRole.availabilityExceptions)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.endpoint)
    }

    @Test
    fun `transforms practitioner role with some telecoms filtered`() {
        val practitionerRoleJson = """{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  },
            |  "telecom" : [ {
            |    "value" : "8675309"
            |  }, {
            |    "system" : "email",
            |    "value" : "doctor@hospital.org"
            |  } ]
            |}""".trimMargin()
        val practitionerRole = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRoleJson
        }

        val oncologyPractitionerRole = transformer.transformPractitionerRole(practitionerRole, tenant)

        oncologyPractitionerRole!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", oncologyPractitionerRole.resourceType)
        assertEquals(Id("test-12345"), oncologyPractitionerRole.id)
        assertNull(oncologyPractitionerRole.meta)
        assertNull(oncologyPractitionerRole.implicitRules)
        assertNull(oncologyPractitionerRole.language)
        assertNull(oncologyPractitionerRole.text)
        assertEquals(listOf<ContainedResource>(), oncologyPractitionerRole.contained)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.extension)
        assertEquals(listOf<Extension>(), oncologyPractitionerRole.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyPractitionerRole.identifier
        )
        assertNull(oncologyPractitionerRole.active)
        assertNull(oncologyPractitionerRole.period)
        assertEquals(Reference(reference = "Practitioner/test-1234"), oncologyPractitionerRole.practitioner)
        assertEquals(Reference(reference = "Organization/test-5678"), oncologyPractitionerRole.organization)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.code)
        assertEquals(listOf<CodeableConcept>(), oncologyPractitionerRole.specialty)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.location)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.healthcareService)
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.EMAIL, value = "doctor@hospital.org")),
            oncologyPractitionerRole.telecom
        )
        assertEquals(listOf<AvailableTime>(), oncologyPractitionerRole.availableTime)
        assertEquals(listOf<NotAvailable>(), oncologyPractitionerRole.notAvailable)
        assertNull(oncologyPractitionerRole.availabilityExceptions)
        assertEquals(listOf<Reference>(), oncologyPractitionerRole.endpoint)
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<PractitionerRole>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformPractitionerRoles(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val practitionerRole1 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }
        val practitionerRole2 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val bundle = mockk<Bundle<PractitionerRole>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitionerRole1, practitionerRole2)
        }

        val oncologyPractitionerRoles = transformer.transformPractitionerRoles(bundle, tenant)
        assertEquals(0, oncologyPractitionerRoles.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val practitionerRole1 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns "{}"
        }

        val practitionerRole2Json = """
            |{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  }
            |}""".trimMargin()
        val practitionerRole2 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRole2Json
        }

        val bundle = mockk<Bundle<PractitionerRole>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitionerRole1, practitionerRole2)
        }

        val oncologyPractitionerRoles = transformer.transformPractitionerRoles(bundle, tenant)
        assertEquals(1, oncologyPractitionerRoles.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val practitionerRole1Json = """
            |{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "67890",
            |  "practitioner" : {
            |    "reference" : "Practitioner/5678"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/1234"
            |  }
            |}""".trimMargin()
        val practitionerRole1 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRole1Json
        }

        val practitionerRole2Json = """
            |{
            |  "resourceType" : "PractitionerRole",
            |  "id" : "12345",
            |  "practitioner" : {
            |    "reference" : "Practitioner/1234"
            |  },
            |  "organization" : {
            |    "reference" : "Organization/5678"
            |  }
            |}""".trimMargin()
        val practitionerRole2 = mockk<PractitionerRole> {
            every { dataSource } returns DataSource.FHIR_R4
            every { raw } returns practitionerRole2Json
        }

        val bundle = mockk<Bundle<PractitionerRole>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(practitionerRole1, practitionerRole2)
        }

        val oncologyPractitionerRoles = transformer.transformPractitionerRoles(bundle, tenant)
        assertEquals(2, oncologyPractitionerRoles.size)
    }
}
