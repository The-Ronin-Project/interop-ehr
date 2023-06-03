package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.Qualification
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPractitionerTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninPractitioner = RoninPractitioner(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(roninPractitioner.qualifies(Practitioner()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            name = listOf(HumanName(family = "Doe".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Practitioner.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Practitioner.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Practitioner.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails if no name`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: name is a required element @ Practitioner.name",
            exception.message
        )
    }

    @Test
    fun `validate fails if no family name`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(given = listOf("George".asFHIR())))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: family is a required element @ Practitioner.name[0].family",
            exception.message
        )
    }

    @Test
    fun `validate fails for multiple names with no family name`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(
                HumanName(given = listOf("George").asFHIR()),
                HumanName(family = "Smith".asFHIR()),
                HumanName(given = listOf("John").asFHIR())
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: family is a required element @ Practitioner.name[0].family\n" +
                "ERROR REQ_FIELD: family is a required element @ Practitioner.name[2].family",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR()))
        )

        mockkObject(R4PractitionerValidator)
        every {
            R4PractitionerValidator.validate(
                practitioner,
                LocationContext(Practitioner::class)
            )
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Practitioner::address),
                LocationContext(Practitioner::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: address is a required element @ Practitioner.address",
            exception.message
        )

        unmockkObject(R4PractitionerValidator)
    }

    @Test
    fun `validate checks meta`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitioner.validate(practitioner).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Practitioner.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            name = listOf(HumanName(family = "Doe".asFHIR()))
        )

        roninPractitioner.validate(practitioner).alertIfErrors()
    }

    @Test
    fun `transform fails for practitioner with no ID`() {
        val practitioner = Practitioner()

        val (transformed, _) = roninPractitioner.transform(practitioner, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms practitioner with all attributes`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
            identifier = listOf(Identifier(value = "id".asFHIR())),
            active = FHIRBoolean.TRUE,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = listOf(Address(country = "USA".asFHIR())),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            qualification = listOf(Qualification(code = CodeableConcept(text = "code".asFHIR()))),
            communication = listOf(CodeableConcept(text = "communication".asFHIR()))
        )

        val (transformed, validation) = roninPractitioner.transform(practitioner, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("Practitioner", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
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
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(FHIRBoolean.TRUE, transformed.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR())), transformed.name)
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            transformed.telecom
        )
        assertEquals(listOf(Address(country = "USA".asFHIR())), transformed.address)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            transformed.photo
        )
        assertEquals(listOf(Qualification(code = CodeableConcept(text = "code".asFHIR()))), transformed.qualification)
        assertEquals(listOf(CodeableConcept(text = "communication".asFHIR())), transformed.communication)
    }

    @Test
    fun `transforms practitioner with only required attributes`() {
        val practitioner = Practitioner(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            name = listOf(HumanName(family = "Doe".asFHIR()))
        )

        val (transformed, validation) = roninPractitioner.transform(practitioner, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("Practitioner", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.active)
        assertEquals(listOf(HumanName(family = "Doe".asFHIR())), transformed.name)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<Address>(), transformed.address)
        assertNull(transformed.gender)
        assertNull(transformed.birthDate)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Qualification>(), transformed.qualification)
        assertEquals(listOf<CodeableConcept>(), transformed.communication)
    }
}
