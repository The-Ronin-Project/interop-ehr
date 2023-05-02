package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceRelatesTo
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninDocumentReferenceTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val categoryCodingList = listOf(
        Coding(
            system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri,
            code = Code("clinical-note"),
            display = "Clinical Note".asFHIR()
        )
    )
    private val adhdPlanTypeCode = Code("74155-3")
    private val docTypeCoding =
        Coding(system = CodeSystem.LOINC.uri, code = adhdPlanTypeCode, display = "ADHD action plan".asFHIR())
    private val docTypeCodingList = listOf(docTypeCoding)
    private val docStatusSystem = Uri("http://hl7.org/fhir/document-reference-status")
    private val statusCurrentCoding =
        Coding(system = docStatusSystem, code = DocumentReferenceStatus.CURRENT.asCode(), display = "Current".asFHIR())
    private val statusSupsersededCoding = Coding(
        system = docStatusSystem,
        code = DocumentReferenceStatus.SUPERSEDED.asCode(),
        display = "Superseded".asFHIR()
    )
    private val statusErrorCoding = Coding(
        system = docStatusSystem,
        code = DocumentReferenceStatus.ENTERED_IN_ERROR.asCode(),
        display = "Entered in Error".asFHIR()
    )
    private val statusCodingList = listOf(statusCurrentCoding, statusSupsersededCoding, statusErrorCoding)
    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("DocumentReference.status", RoninProfile.DOCUMENT_REFERENCE.value)
        } returns statusCodingList
        every {
            getRequiredValueSet("DocumentReference.type", RoninProfile.DOCUMENT_REFERENCE.value)
        } returns docTypeCodingList
    }
    private val roninDocumentReference = RoninDocumentReference(normalizer, localizer, normRegistryClient)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninDocumentReference.qualifies(
                DocumentReference(
                    type = CodeableConcept(
                        coding = docTypeCodingList
                    ),
                    status = DocumentReferenceStatus.CURRENT.asCode()
                )
            )
        )
    }

    @Test
    fun `validate - fails if missing identifiers`() {
        val documentReference = DocumentReference(
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri,
                            code = Code("clinical-note"),
                            display = "Clinical Note".asFHIR()
                        )
                    )
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ DocumentReference.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ DocumentReference.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ DocumentReference.identifier",
            exception.message
        )
    }

    @Test
    fun `validate - fails if missing category`() {
        val documentReference = DocumentReference(
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
            status = DocumentReferenceStatus.CURRENT.asCode(),
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: category is a required element @ DocumentReference.category",
            exception.message
        )
    }

    @Test
    fun `validate - fails if status does not use required valueset`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = Code("x"),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'x' is outside of required value set @ DocumentReference.status",
            exception.message
        )
    }

    @Test
    fun `validate - fails if category does not use required code`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri,
                            code = Code("bad"),
                            display = "bad".asFHIR()
                        )
                    )
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'http://hl7.org/fhir/us/core/CodeSystem/us-core-documentreference-category|bad' is outside of required value set @ DocumentReference.category",
            exception.message
        )
    }

    @Test
    fun `validate - fails if category does not use required system`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("bad"),
                            code = Code("clinical-note"),
                            display = "bad".asFHIR()
                        )
                    )
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad|clinical-note' is outside of required value set @ DocumentReference.category",
            exception.message
        )
    }

    @Test
    fun `validate - error - docStatus is present and does not match the value set`() {
        // except for the test case details,
        // all attributes are correct

        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            docStatus = Code("bad"),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad' is outside of required value set @ DocumentReference.docStatus",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds with just required attributes`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )
        roninDocumentReference.validate(documentReference, null).alertIfErrors()
    }

    @Test
    fun `transform - succeeds with just required attributes`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val (transformed, validation) = roninDocumentReference.transform(documentReference, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(3, transformed.identifier.size)
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
        assertEquals(documentReference.type, transformed.type)
        assertEquals(documentReference.status, transformed.status)
    }

    @Test
    fun `transform and validate - succeeds with all attributes present`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/DocumentReference.html"))
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
            identifier = listOf(Identifier(value = "67890".asFHIR())),

            docStatus = CompositionStatus.FINAL.asCode(),
            date = Instant("2003-04-03T00:00:00Z"),
            author = listOf(Reference(reference = "Practitioner/456".asFHIR())),
            authenticator = Reference(reference = "Practitioner/123".asFHIR()),
            custodian = Reference(reference = "Organization/123".asFHIR()),
            relatesTo = listOf(
                DocumentReferenceRelatesTo(
                    code = DocumentRelationshipType.SIGNS.asCode(),
                    target = Reference(reference = "DocumentReference/ABC".asFHIR())
                )
            ),
            description = "everywhere".asFHIR(),
            securityLabel = listOf(
                CodeableConcept(
                    coding = listOf(Coding(code = Code("a"), system = Uri("b"), display = "c".asFHIR())),
                    text = "d".asFHIR()
                )
            ),
            context = DocumentReferenceContext(
                encounter = listOf(Reference(reference = "Enocunter/ABC".asFHIR())),
                related = listOf(Reference(reference = "DocumentReference/XYZ".asFHIR()))
            ),
            type = CodeableConcept(
                coding = docTypeCodingList
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        // transformation
        val (transformed, validation) = roninDocumentReference.transform(documentReference, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.DOCUMENT_REFERENCE.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(documentReference.implicitRules, transformed.implicitRules)
        assertEquals(documentReference.language, transformed.language)
        assertEquals(documentReference.text, transformed.text)
        assertEquals(documentReference.contained, transformed.contained)
        assertEquals(documentReference.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
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
        assertEquals(documentReference.type, transformed.type)
        assertEquals(documentReference.status, transformed.status)
        assertEquals(documentReference.docStatus, transformed.docStatus)
        assertEquals(documentReference.date, transformed.date)
        assertEquals(documentReference.author, transformed.author)
        assertEquals(documentReference.authenticator, transformed.authenticator)
        assertEquals(documentReference.custodian, transformed.custodian)
        assertEquals(documentReference.relatesTo, transformed.relatesTo)
        assertEquals(documentReference.description, transformed.description)
        assertEquals(documentReference.securityLabel, transformed.securityLabel)
        assertEquals(documentReference.context, transformed.context)

        // validation
        roninDocumentReference.validate(transformed, null).alertIfErrors()
    }

    @Test
    fun `transform - returns null if validation fails - for example a required attribute is missing`() {
        val documentReference = DocumentReference(
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
            status = null,
            type = null
        )
        val (transformed, _) = roninDocumentReference.transform(documentReference, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate fails if no subject`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList,
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = null,
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ DocumentReference.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if subject has no reference attribute`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList,
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ DocumentReference.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if subject reference is wrong type`() {
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList,
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(reference = "DocumentReference/12345".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ DocumentReference.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if content has no attachment`() {
        DocumentReference(
            status = null

        )
        val documentReference = DocumentReference(
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
            type = CodeableConcept(
                coding = docTypeCodingList,
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = null
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: attachment is a required element @ DocumentReference.content[0].attachment",
            exception.message
        )
    }
}
