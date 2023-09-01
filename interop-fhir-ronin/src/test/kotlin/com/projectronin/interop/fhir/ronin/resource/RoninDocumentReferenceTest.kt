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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceRelatesTo
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
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

    // DocumentReference.type
    private val docRefTypeCode = Code("74155-3")
    private val docRefTypeCoding = Coding(
        system = CodeSystem.LOINC.uri,
        code = docRefTypeCode,
        display = "ADHD action plan".asFHIR()
    )
    private val docRefTypeCodingList = listOf(docRefTypeCoding)
    private val docRefTypeConcept = CodeableConcept(
        coding = docRefTypeCodingList,
        text = "ADHD action plan".asFHIR()
    )

    // tenant DocumentReference.type
    private val tenantDocRefTypeCode = Code("Tenant-74155-3")
    private val tenantDocRefTypeCoding = Coding(
        system = CodeSystem.LOINC.uri,
        code = tenantDocRefTypeCode,
        display = "Tenant action plan".asFHIR()
    )
    private val tenantDocRefTypeCodingList = listOf(tenantDocRefTypeCoding)
    private val tenantDocRefTypeConcept = CodeableConcept(
        coding = tenantDocRefTypeCodingList,
        text = "Tenant action plan concept".asFHIR()
    )
    private val tenantDocRefTypeExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.value),
        value = DynamicValue(
            type = DynamicValueType.CODEABLE_CONCEPT,
            value = tenantDocRefTypeConcept
        )
    )
    private val docRefTypeMetadata = ConceptMapMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "docreference",
        conceptMapUuid = "2c353c65-e4d7-4932-b518-7bc42d98772d",
        version = "1"
    )
    private val documentReferenceExtension = listOf(tenantDocRefTypeExtension)

    // registry and profile
    private val registryClient = mockk<NormalizationRegistryClient> {
        every {
            getConceptMapping(
                tenant,
                "DocumentReference.type",
                docRefTypeConcept
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "DocumentReference.type",
                tenantDocRefTypeConcept
            )
        } returns ConceptMapCodeableConcept(docRefTypeConcept, tenantDocRefTypeExtension, listOf(docRefTypeMetadata))
        every {
            getConceptMapping(
                tenant,
                "DocumentReference.type",
                CodeableConcept(text = "bad".asFHIR())
            )
        } returns ConceptMapCodeableConcept(
            CodeableConcept(text = "worse".asFHIR()),
            tenantDocRefTypeExtension,
            listOf(docRefTypeMetadata)
        )
    }
    private val roninDocumentReference = RoninDocumentReference(normalizer, localizer, registryClient)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninDocumentReference.qualifies(
                DocumentReference(
                    type = docRefTypeConcept,
                    status = DocumentReferenceStatus.CURRENT.asCode()
                )
            )
        )
    }

    @Test
    fun `validate - fails if missing identifiers`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
            type = docRefTypeConcept,
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
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = Code("x"),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
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
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
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
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            docStatus = Code("bad"),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad' is outside of required value set @ DocumentReference.docStatus",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val documentReference = DocumentReference(
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ DocumentReference.meta",
            exception.message
        )
    }

    @Test
    fun `validate - fails with no attachment URL`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(data = Base64Binary("c3po"), contentType = Code("plain/text"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: url is a required element @ DocumentReference.content[0].attachment.url",
            exception.message
        )
    }

    @Disabled
    @Test
    fun `validate - fails with no datalake attachment extension`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(url = Url("Binary/1234"))
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_003: Datalake Attachment URL extension is missing or invalid @ DocumentReference.content[0].attachment.url.extension",
            exception.message
        )
    }

    @Disabled
    @Test
    fun `validate - fails with datalake attachment extension with wrong type`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.STRING, FHIRString("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_003: Datalake Attachment URL extension is missing or invalid @ DocumentReference.content[0].attachment.url.extension",
            exception.message
        )
    }

    @Test
    fun `validate - succeeds with just required attributes`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )
        roninDocumentReference.validate(documentReference).alertIfErrors()
    }

    @Test
    fun `transform - succeeds and adds sourceDocumentReferenceType extension with just required attributes`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(source = Uri("fake-source-fake-url")),
            type = tenantDocRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val (transformed, validation) = roninDocumentReference.transform(documentReference, tenant)
        validation.alertIfErrors()
        transformed!!
        assertEquals(
            documentReferenceExtension,
            transformed.extension
        )
    }

    @Test
    fun `transform - succeeds with just required attributes`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            type = tenantDocRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
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
            Meta(
                source = Uri("source"),
                profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value))
            ),
            transformed.meta
        )
        assertEquals(
            documentReferenceExtension,
            transformed.extension
        )
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
        assertEquals(docRefTypeConcept, transformed.type)
        assertEquals(documentReference.status, transformed.status)
    }

    @Test
    fun `transform and validate - succeeds with all attributes present`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/DocumentReference.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
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
            type = tenantDocRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
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
        assertEquals(
            documentReference.extension + documentReferenceExtension,
            transformed.extension
        )
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
        assertEquals(docRefTypeConcept, transformed.type)
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
        roninDocumentReference.validate(transformed).alertIfErrors()
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
            type = tenantDocRefTypeConcept
        )
        val (transformed, _) = roninDocumentReference.transform(documentReference, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate fails if no subject`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = null,
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(display = "reference".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
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
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(reference = "DocumentReference/12345".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ DocumentReference.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if content has no attachment`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            type = docRefTypeConcept,
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
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: attachment is a required element @ DocumentReference.content[0].attachment",
            exception.message
        )
    }

    @Test
    fun `validate - fails for no type`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: type is a required element @ DocumentReference.type",
            exception.message
        )
    }

    @Test
    fun `validate - fails for type with no coding`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_002: One, and only one, coding entry is allowed for type @ DocumentReference.type.coding",
            exception.message
        )
    }

    @Test
    fun `validate - fails for type with multiple coding`() {
        val documentReference = DocumentReference(
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            extension = documentReferenceExtension,
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
                coding = docRefTypeCodingList + docRefTypeCodingList,
                text = "code".asFHIR()
            ),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_002: One, and only one, coding entry is allowed for type @ DocumentReference.type.coding",
            exception.message
        )
    }

    @Test
    fun `validate fails if source document reference type extension is missing`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
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
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_001: Tenant source Document Reference extension is missing or invalid @ DocumentReference.extension",
            exception.message
        )
    }

    @Test
    fun `validate fails if source document reference type extension has a bad url`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = docRefTypeCodingList,
                            text = "code".asFHIR()
                        )
                    )
                )
            ),
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_001: Tenant source Document Reference extension is missing or invalid @ DocumentReference.extension",
            exception.message
        )
    }

    @Test
    fun `validate fails if source document reference type extension has the wrong datatype`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODING,
                        docRefTypeCoding
                    )
                )
            ),
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = categoryCodingList
                )
            ),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDocumentReference.validate(documentReference).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_001: Tenant source Document Reference extension is missing or invalid @ DocumentReference.extension",
            exception.message
        )
    }

    @Test
    fun `transform and validate - fails if concept map has no entry for type`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(source = Uri("fake-source-fake-url")),
            type = docRefTypeConcept,
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val (transformed, validation) = roninDocumentReference.transform(documentReference, tenant)
        assertNull(transformed)
        val exception = assertThrows<IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '74155-3' has no target defined in " +
                "any DocumentReference.type concept map for tenant 'test' @ DocumentReference.type\n" +
                "ERROR RONIN_DOCREF_001: Tenant source Document Reference extension is missing or invalid @ DocumentReference.extension",
            exception.message
        )
    }

    @Test
    fun `transform and validate - fails if concept map has entry for type, but that entry cannot validate`() {
        val documentReference = DocumentReference(
            id = Id("12345"),
            meta = Meta(source = Uri("fake-source-fake-url")),
            type = CodeableConcept(text = "bad".asFHIR()),
            status = DocumentReferenceStatus.CURRENT.asCode(),
            category = listOf(CodeableConcept(coding = categoryCodingList)),
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        url = Url(
                            "Binary/1234",
                            extension = listOf(
                                Extension(
                                    url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                    value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234"))
                                )
                            )
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val (transformed, validation) = roninDocumentReference.transform(documentReference, tenant)
        assertNull(transformed)
        val exception = assertThrows<IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DOCREF_002: One, and only one, coding entry is allowed for type @ DocumentReference.type.coding",
            exception.message
        )
    }
}
