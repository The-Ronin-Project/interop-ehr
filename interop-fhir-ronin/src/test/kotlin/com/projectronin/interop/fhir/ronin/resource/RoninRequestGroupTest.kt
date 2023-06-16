package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.resource.RequestGroupAction
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.RequestGroupIntent
import com.projectronin.interop.fhir.r4.valueset.RequestGroupStatus
import com.projectronin.interop.fhir.r4.valueset.RequestPriority
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninRequestGroupTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninRequestGroup = RoninRequestGroup(normalizer, localizer)

    @Test
    fun `validation fails without subject`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ RequestGroup.subject",
            exception.message
        )
    }

    @Test
    fun `validation fails with subject but no type`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(reference = "Patient/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ RequestGroup.subject.type",
            exception.message
        )
    }

    @Test
    fun `validation fails with subject and type but no data-authority reference extension`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient"))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ RequestGroup.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validation fails without meta`() {
        val requestGroup = RequestGroup(
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ RequestGroup.meta",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if not required status value-set`() {
        val requestGroup = RequestGroup(
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = Code("fake-status"),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'fake-status' is outside of required value set @ RequestGroup.status",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if not required intent value-set`() {
        val requestGroup = RequestGroup(
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = Code("fake-intent"),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'fake-intent' is outside of required value set @ RequestGroup.status",
            exception.message
        )
    }

    @Test
    fun `validate - checks R4 profile - fails if not required priority value-set`() {
        val requestGroup = RequestGroup(
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            priority = Code("fake-priority"),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )

        )

        val exception = assertThrows<IllegalArgumentException> {
            roninRequestGroup.validate(requestGroup).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'fake-priority' is outside of required value set @ RequestGroup.priority",
            exception.message
        )
    }

    @Test
    fun `validate profile - succeeds`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
                    value = "Data Authority Identifier.asFHIR".asFHIR()
                )
            ),
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )
        roninRequestGroup.validate(requestGroup).alertIfErrors()
    }

    @Test
    fun `transforms with only required attributes`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )
        val (transformed, validation) = roninRequestGroup.transform(requestGroup, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("RequestGroup", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
            transformed.meta
        )
        Assertions.assertNull(transformed.implicitRules)
        Assertions.assertNull(transformed.language)
        Assertions.assertNull(transformed.text)
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(RequestGroupStatus.DRAFT.asCode(), transformed.status)
        assertEquals(RequestGroupIntent.OPTION.asCode(), transformed.intent)
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
    }

    @Test
    fun `transform requestGroup with all attributes`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)),
                source = Uri("source")
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
            identifier = listOf(Identifier(value = "id".asFHIR(), system = Uri("Id-Id"))),
            instantiatesCanonical = listOf(Canonical(value = "canonical")),
            instantiatesUri = listOf(Uri("uri")),
            basedOn = listOf(Reference(reference = "reference".asFHIR())),
            replaces = listOf(Reference(reference = "reference".asFHIR())),
            groupIdentifier = Identifier(value = "group".asFHIR()),
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            priority = RequestPriority.ASAP.asCode(),
            code = CodeableConcept(text = "request-group".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            encounter = Reference(reference = "reference".asFHIR()),
            authoredOn = null,
            author = Reference(reference = "reference".asFHIR()),
            reasonCode = listOf(
                CodeableConcept(text = "request-group reason code".asFHIR())
            ),
            reasonReference = listOf(Reference(reference = "reference".asFHIR())),
            note = listOf(Annotation(text = Markdown("annotation"))),
            action = listOf(
                RequestGroupAction(
                    title = "request-group-action".asFHIR(),
                    description = "request-group-description".asFHIR(),
                    resource = Reference(reference = "reference".asFHIR())
                )
            )
        )
        val (transformed, validation) = roninRequestGroup.transform(requestGroup, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("RequestGroup", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
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
                Identifier(system = Uri("Id-Id"), value = "id".asFHIR()),
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
        assertEquals(
            listOf(
                Canonical(value = "canonical")
            ),
            transformed.instantiatesCanonical
        )
        assertEquals(
            listOf(
                Uri("uri")
            ),
            transformed.instantiatesUri
        )
        assertEquals(
            listOf(
                Reference(reference = "reference".asFHIR())
            ),
            transformed.basedOn
        )
        assertEquals(
            listOf(
                Reference(reference = "reference".asFHIR())
            ),
            transformed.replaces
        )
        assertEquals(
            Identifier(value = "group".asFHIR()),
            transformed.groupIdentifier
        )
        assertEquals(RequestGroupStatus.DRAFT.asCode(), transformed.status)
        assertEquals(RequestGroupIntent.OPTION.asCode(), transformed.intent)
        assertEquals(RequestPriority.ASAP.asCode(), transformed.priority)
        assertEquals(CodeableConcept(text = "request-group".asFHIR()), transformed.code)
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(
            Reference(reference = "reference".asFHIR()),
            transformed.encounter
        )
        assertEquals(null, transformed.authoredOn)
        assertEquals(Reference(reference = "reference".asFHIR()), transformed.author)
        assertEquals(listOf(CodeableConcept(text = "request-group reason code".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(reference = "reference".asFHIR())), transformed.reasonReference)
        assertEquals(listOf(Annotation(text = Markdown("annotation"))), transformed.note)
        assertEquals(
            listOf(
                RequestGroupAction(
                    title = "request-group-action".asFHIR(),
                    description = "request-group-description".asFHIR(),
                    resource = Reference(reference = "reference".asFHIR())
                )
            ),
            transformed.action
        )
    }
}
