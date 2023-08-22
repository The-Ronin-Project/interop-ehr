package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.AvailableTime
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.NotAvailable
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
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

class RoninPractitionerRoleTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninPractitionerRole = RoninPractitionerRole(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(roninPractitionerRole.qualifies(PractitionerRole()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            identifier = listOf(),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ PractitionerRole.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ PractitionerRole.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ PractitionerRole.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for no practitioner`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = null,
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: practitioner is a required element @ PractitionerRole.practitioner",
            exception.message
        )
    }

    @Test
    fun `validate fails for no telecom value`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR()),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ PractitionerRole.telecom[0].value",
            exception.message
        )
    }

    @Test
    fun `validate fails for multiple invalid telecoms`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR()),
            telecom = listOf(
                ContactPoint(use = ContactPointUse.HOME.asCode()),
                ContactPoint(system = ContactPointSystem.EMAIL.asCode(), value = "josh@projectronin.com".asFHIR()),
                ContactPoint(system = ContactPointSystem.PHONE.asCode())
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ PractitionerRole.telecom[0].value\n" +
                "ERROR REQ_FIELD: value is a required element @ PractitionerRole.telecom[2].value",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        mockkObject(R4PractitionerRoleValidator)
        every {
            R4PractitionerRoleValidator.validate(
                practitionerRole,
                LocationContext(PractitionerRole::class)
            )
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(PractitionerRole::endpoint),
                LocationContext(PractitionerRole::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: endpoint is a required element @ PractitionerRole.endpoint",
            exception.message
        )

        unmockkObject(R4PractitionerRoleValidator)
    }

    @Test
    fun `validate checks meta`() {
        val practitionerRole = PractitionerRole(
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPractitionerRole.validate(practitionerRole).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ PractitionerRole.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        roninPractitionerRole.validate(practitionerRole).alertIfErrors()
    }

    @Test
    fun `transform fails for practitioner role with no ID`() {
        val practitionerRole = PractitionerRole()

        val (transformed, _) = roninPractitionerRole.transform(practitionerRole, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms a sparse practitioner role with all required properties but no organization, location, or other optional properties`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner-role")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            code = listOf(CodeableConcept(text = "code".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
            notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
            availabilityExceptions = "exceptions".asFHIR(),
            endpoint = listOf(Reference(reference = "Endpoint/1357".asFHIR()))
        )

        val (transformed, validation) = roninPractitionerRole.transform(practitionerRole, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(0, transformed.extension.size)
        assertEquals(0, transformed.modifierExtension.size)
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
        assertEquals(Period(end = DateTime("2022")), transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertNull(transformed.organization)
        assertEquals(listOf(CodeableConcept(text = "code".asFHIR())), transformed.code)
        assertEquals(listOf(CodeableConcept(text = "specialty".asFHIR())), transformed.specialty)
        assertEquals(0, transformed.location.size)
        assertEquals(0, transformed.healthcareService.size)
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            transformed.telecom
        )
        assertEquals(listOf(AvailableTime(allDay = FHIRBoolean.FALSE)), transformed.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now".asFHIR())), transformed.notAvailable)
        assertEquals("exceptions".asFHIR(), transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/1357".asFHIR())), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with all attributes`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner-role")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
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
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR()),
            code = listOf(CodeableConcept(text = "code".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            location = listOf(Reference(reference = "Location/9012".asFHIR())),
            healthcareService = listOf(Reference(reference = "HealthcareService/3456".asFHIR())),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
            notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
            availabilityExceptions = "exceptions".asFHIR(),
            endpoint = listOf(Reference(reference = "Endpoint/1357".asFHIR()))
        )

        val (transformed, validation) = roninPractitionerRole.transform(practitionerRole, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
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
        assertEquals(Period(end = DateTime("2022")), transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf(CodeableConcept(text = "code".asFHIR())), transformed.code)
        assertEquals(listOf(CodeableConcept(text = "specialty".asFHIR())), transformed.specialty)
        assertEquals(listOf(Reference(reference = "Location/9012".asFHIR())), transformed.location)
        assertEquals(
            listOf(Reference(reference = "HealthcareService/3456".asFHIR())),
            transformed.healthcareService
        )
        assertEquals(
            listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            transformed.telecom
        )
        assertEquals(listOf(AvailableTime(allDay = FHIRBoolean.FALSE)), transformed.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now".asFHIR())), transformed.notAvailable)
        assertEquals("exceptions".asFHIR(), transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/1357".asFHIR())), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with only required attributes`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR())
        )

        val (transformed, validation) = roninPractitionerRole.transform(practitionerRole, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
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
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with all telecoms filtered`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR()),
            telecom = listOf(ContactPoint(id = "first".asFHIR()), ContactPoint(id = "second".asFHIR()))
        )

        val (transformed, validation) = roninPractitionerRole.transform(practitionerRole, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
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
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with some telecoms filtered`() {
        val practitionerRole = PractitionerRole(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
            organization = Reference(reference = "Organization/5678".asFHIR()),
            telecom = listOf(
                ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR()),
                ContactPoint(id = "second".asFHIR()),
                ContactPoint(id = "third".asFHIR()),
                ContactPoint(system = ContactPointSystem.EMAIL.asCode(), value = "doctor@hospital.org".asFHIR())
            )
        )

        val (transformed, validation) = roninPractitionerRole.transform(practitionerRole, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
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
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(
            listOf(
                ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR()),
                ContactPoint(system = ContactPointSystem.EMAIL.asCode(), value = "doctor@hospital.org".asFHIR())
            ),
            transformed.telecom
        )
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
