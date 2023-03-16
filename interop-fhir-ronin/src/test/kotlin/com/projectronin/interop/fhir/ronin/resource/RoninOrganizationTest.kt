package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.OrganizationContact
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninOrganizationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninOrganization = RoninOrganization(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(roninOrganization.qualifies(Organization()))
    }

    @Test
    fun `validate fails without ronin identifiers`() {
        val organization = Organization(
            id = Id("12345"),
            name = "Organization Name".asFHIR(),
            active = true.asFHIR()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninOrganization.validate(organization, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Organization.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Organization.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails with no organization name provided`() {
        val organization = Organization(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            active = true.asFHIR()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninOrganization.validate(organization, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: name is a required element @ Organization.name",
            exception.message
        )
    }

    @Test
    fun `validate fails with no organization active provided`() {
        val organization = Organization(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "Organization name".asFHIR()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninOrganization.validate(organization, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: active is a required element @ Organization.active",
            exception.message
        )
    }

    @Test
    fun `validate against R4 profile for organization`() {
        val organization = Organization(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "Organization name".asFHIR(),
            active = true.asFHIR()
        )

        mockkObject(R4OrganizationValidator)
        every {
            R4OrganizationValidator.validate(
                organization,
                LocationContext(Organization::class)
            )
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Organization::name),
                LocationContext(Organization::class)
            )
            checkNotNull(
                null,
                RequiredFieldError(Organization::active),
                LocationContext(Organization::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninOrganization.validate(organization, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: name is a required element @ Organization.name\n" +
                "ERROR REQ_FIELD: active is a required element @ Organization.active",
            exception.message
        )

        unmockkObject(R4OrganizationValidator)
    }

    @Test
    fun `validate is successful with name and active`() {
        val organization = Organization(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "Organization name".asFHIR(),
            active = true.asFHIR()
        )

        roninOrganization.validate(organization, null).alertIfErrors()
    }

    @Test
    fun `transform organization with all attributes`() {
        val organization = Organization(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/organization.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            active = true.asFHIR(),
            type = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/organization-type"),
                            code = Code("prov"),
                            display = "Healthcare Provider".asFHIR()
                        )
                    )
                )
            ),
            name = "Organization Name".asFHIR(),
            alias = listOf(
                "Other Organization Name".asFHIR(),
                "Organization also known as...".asFHIR()
            ),
            telecom = listOf(
                ContactPoint(
                    id = "FAKEID".asFHIR(),
                    system = Code("phone"),
                    value = "555-555-5555".asFHIR(),
                    use = Code("work")
                )
            ),
            address = listOf(
                Address(
                    country = "USA".asFHIR()
                )
            ),
            partOf = Reference(reference = "Organization/super".asFHIR()),
            contact = listOf(
                OrganizationContact(
                    purpose = CodeableConcept(
                        coding = listOf(
                            Coding(
                                code = Code("fake")
                            )
                        )
                    ),
                    name = HumanName(
                        given = listOf(
                            "FakeName".asFHIR()
                        )
                    ),
                    telecom = listOf(
                        ContactPoint(
                            system = Code("phone"),
                            value = "555-555-5555".asFHIR()
                        )
                    ),
                    address = Address(
                        country = "USA".asFHIR()
                    )
                )
            ),
            endpoint = listOf(
                Reference(
                    reference = "Endpoint/1357".asFHIR()
                )
            )
        )

        val (transformed, validation) = roninOrganization.transform(organization, tenant)
        validation.alertIfErrors()

        transformed!!

        assertEquals("Organization", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNotNull(transformed.active)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/organization-type"),
                            code = Code("prov"),
                            display = "Healthcare Provider".asFHIR()
                        )
                    )
                )
            ),
            transformed.type
        )
        assertNotNull(transformed.name)
        assertEquals(
            listOf(
                "Other Organization Name".asFHIR(),
                "Organization also known as...".asFHIR()
            ),
            transformed.alias
        )
        assertEquals(
            listOf(
                ContactPoint(
                    id = "FAKEID".asFHIR(),
                    system = Code("phone"),
                    value = "555-555-5555".asFHIR(),
                    use = Code("work")
                )
            ),
            transformed.telecom
        )
        assertEquals(
            listOf(
                Address(
                    country = "USA".asFHIR()
                )
            ),
            transformed.address
        )
        assertEquals(Reference(reference = "Organization/super".asFHIR()), transformed.partOf)
        assertEquals(
            listOf(
                OrganizationContact(
                    purpose = CodeableConcept(
                        coding = listOf(
                            Coding(
                                code = Code("fake")
                            )
                        )
                    ),
                    name = HumanName(
                        given = listOf(
                            "FakeName".asFHIR()
                        )
                    ),
                    telecom = listOf(
                        ContactPoint(
                            system = Code("phone"),
                            value = "555-555-5555".asFHIR()
                        )
                    ),
                    address = Address(
                        country = "USA".asFHIR()
                    )
                )
            ),
            transformed.contact
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "Endpoint/1357".asFHIR()
                )
            ),
            transformed.endpoint
        )
    }

    @Test
    fun `transform organization with only required attributes`() {
        val organization = Organization(
            id = Id("12345"),
            name = "Organization name".asFHIR(),
            active = true.asFHIR()
        )

        val (transformed, validation) = roninOrganization.transform(organization, tenant)
        validation.alertIfErrors()

        transformed!!

        assertEquals("Organization", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value))),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNotNull(transformed.active)
        assertEquals(listOf<CodeableConcept>(), transformed.type)
        assertNotNull(transformed.name)
        assertEquals(listOf<String>(), transformed.alias)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<Address>(), transformed.address)
        assertNull(transformed.partOf)
        assertEquals(listOf<OrganizationContact>(), transformed.contact)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
