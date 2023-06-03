package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Dosage
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.DispenseRequest
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Substitution
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.RequestPriority
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
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

class RoninMedicationRequestTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninMedicationRequest = RoninMedicationRequest(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninMedicationRequest.qualifies(
                MedicationRequest(
                    status = MedicationRequestStatus.COMPLETED.asCode(),
                    intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                    medication = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(text = "medication".asFHIR())
                    ),
                    subject = Reference(reference = "Patient/1234".asFHIR()),
                    requester = Reference(reference = "Practitioner/1234".asFHIR())
                )
            )
        )
    }

    @Test
    fun `validates ronin identifiers`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ MedicationRequest.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ MedicationRequest.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ MedicationRequest.identifier",
            exception.message
        )
    }

    @Test
    fun `validates requester is provided`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            requester = null,
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: requester is a required element @ MedicationRequest.requester",
            exception.message
        )
    }

    @Test
    fun `validates R4 profile`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        mockkObject(R4MedicationRequestValidator)
        every {
            R4MedicationRequestValidator.validate(
                medicationRequest,
                LocationContext(MedicationRequest::class)
            )
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(MedicationRequest::authoredOn),
                LocationContext(MedicationRequest::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: authoredOn is a required element @ MedicationRequest.authoredOn",
            exception.message
        )

        unmockkObject(R4MedicationRequestValidator)
    }

    @Test
    fun `validate fails with subject but no type`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR()),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ MedicationRequest.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject and type but no data authority reference extension`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient")),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ MedicationRequest.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val medicationRequest = MedicationRequest(
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ MedicationRequest.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        roninMedicationRequest.validate(medicationRequest).alertIfErrors()
    }

    @Test
    fun `transforms medication request with all attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("MedicationRequest")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","field":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
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
            status = MedicationRequestStatus.CANCELLED.asCode(),
            statusReason = CodeableConcept(text = "statusReason".asFHIR()),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            category = listOf(CodeableConcept(text = "category".asFHIR())),
            priority = RequestPriority.ASAP.asCode(),
            doNotPerform = FHIRBoolean.FALSE,
            reported = DynamicValue(DynamicValueType.BOOLEAN, true),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            supportingInformation = listOf(Reference(reference = "Condition/1234".asFHIR())),
            authoredOn = DateTime("2022-11-03"),
            requester = Reference(reference = "Practitioner/1234".asFHIR()),
            performer = Reference(reference = "Practitioner/5678".asFHIR()),
            performerType = CodeableConcept(text = "performer type".asFHIR()),
            recorder = Reference(reference = "Practitioner/3456".asFHIR()),
            reasonCode = listOf(CodeableConcept(text = "reason".asFHIR())),
            reasonReference = listOf(Reference(reference = "Condition/5678".asFHIR())),
            instantiatesCanonical = listOf(Canonical("canonical")),
            instantiatesUri = listOf(Uri("uri")),
            basedOn = listOf(Reference(reference = "CarePlan/1234".asFHIR())),
            groupIdentifier = Identifier(value = "group".asFHIR()),
            courseOfTherapyType = CodeableConcept(text = "therapy".asFHIR()),
            insurance = listOf(Reference(reference = "Coverage/1234".asFHIR())),
            note = listOf(Annotation(text = Markdown("note"))),
            dosageInformation = listOf(Dosage(text = "dosage".asFHIR())),
            dispenseRequest = DispenseRequest(numberOfRepeatsAllowed = UnsignedInt(2)),
            substitution = Substitution(allowed = DynamicValue(DynamicValueType.BOOLEAN, true)),
            priorPrescription = Reference(reference = "MedicationRequest/1234".asFHIR()),
            detectedIssue = listOf(Reference(reference = "DetectedIssue/1234".asFHIR())),
            eventHistory = listOf(Reference(reference = "Provenance/1234".asFHIR()))
        )

        val (transformed, validation) = roninMedicationRequest.transform(medicationRequest, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","field":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(MedicationRequestStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(CodeableConcept(text = "statusReason".asFHIR()), transformed.statusReason)
        assertEquals(MedicationRequestIntent.PROPOSAL.asCode(), transformed.intent)
        assertEquals(listOf(CodeableConcept(text = "category".asFHIR())), transformed.category)
        assertEquals(RequestPriority.ASAP.asCode(), transformed.priority)
        assertEquals(FHIRBoolean.FALSE, transformed.doNotPerform)
        assertEquals(DynamicValue(DynamicValueType.BOOLEAN, true), transformed.reported)
        assertEquals(
            DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            transformed.medication
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(listOf(Reference(reference = "Condition/1234".asFHIR())), transformed.supportingInformation)
        assertEquals(DateTime("2022-11-03"), transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.requester)
        assertEquals(Reference(reference = "Practitioner/5678".asFHIR()), transformed.performer)
        assertEquals(CodeableConcept(text = "performer type".asFHIR()), transformed.performerType)
        assertEquals(Reference(reference = "Practitioner/3456".asFHIR()), transformed.recorder)
        assertEquals(listOf(CodeableConcept(text = "reason".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(reference = "Condition/5678".asFHIR())), transformed.reasonReference)
        assertEquals(listOf(Canonical("canonical")), transformed.instantiatesCanonical)
        assertEquals(listOf(Uri("uri")), transformed.instantiatesUri)
        assertEquals(listOf(Reference(reference = "CarePlan/1234".asFHIR())), transformed.basedOn)
        assertEquals(Identifier(value = "group".asFHIR()), transformed.groupIdentifier)
        assertEquals(CodeableConcept(text = "therapy".asFHIR()), transformed.courseOfTherapyType)
        assertEquals(listOf(Reference(reference = "Coverage/1234".asFHIR())), transformed.insurance)
        assertEquals(listOf(Annotation(text = Markdown("note"))), transformed.note)
        assertEquals(listOf(Dosage(text = "dosage".asFHIR())), transformed.dosageInformation)
        assertEquals(DispenseRequest(numberOfRepeatsAllowed = UnsignedInt(2)), transformed.dispenseRequest)
        assertEquals(Substitution(allowed = DynamicValue(DynamicValueType.BOOLEAN, true)), transformed.substitution)
        assertEquals(Reference(reference = "MedicationRequest/1234".asFHIR()), transformed.priorPrescription)
        assertEquals(listOf(Reference(reference = "DetectedIssue/1234".asFHIR())), transformed.detectedIssue)
        assertEquals(listOf(Reference(reference = "Provenance/1234".asFHIR())), transformed.eventHistory)
    }

    @Test
    fun `transforms medication request with only required attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val (transformed, validation) = roninMedicationRequest.transform(medicationRequest, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
        assertEquals(MedicationRequestStatus.CANCELLED.asCode(), transformed.status)
        assertNull(transformed.statusReason)
        assertEquals(MedicationRequestIntent.PROPOSAL.asCode(), transformed.intent)
        assertEquals(listOf<CodeableConcept>(), transformed.category)
        assertNull(transformed.priority)
        assertNull(transformed.doNotPerform)
        assertNull(transformed.reported)
        assertEquals(
            DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            transformed.medication
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertNull(transformed.encounter)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.requester)
        assertNull(transformed.performer)
        assertNull(transformed.performerType)
        assertNull(transformed.recorder)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertEquals(listOf<Canonical>(), transformed.instantiatesCanonical)
        assertEquals(listOf<Uri>(), transformed.instantiatesUri)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertNull(transformed.groupIdentifier)
        assertNull(transformed.courseOfTherapyType)
        assertEquals(listOf<Reference>(), transformed.insurance)
        assertEquals(listOf<Annotation>(), transformed.note)
        assertEquals(listOf<Dosage>(), transformed.dosageInformation)
        assertNull(transformed.dispenseRequest)
        assertNull(transformed.substitution)
        assertNull(transformed.priorPrescription)
        assertEquals(listOf<Reference>(), transformed.detectedIssue)
        assertEquals(listOf<Reference>(), transformed.eventHistory)
    }

    @Test
    fun `transform fails for medication request with missing id`() {
        val medicationRequest = MedicationRequest(
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val (transformed, _) = roninMedicationRequest.transform(medicationRequest, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate fails with missing subject reference attribute`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                display = "reference".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ MedicationRequest.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong subject reference type`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Condition/1234".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ MedicationRequest.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with missing subject`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = null,
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ MedicationRequest.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with missing requester reference attribute`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            requester = Reference(display = "reference".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of Device, Organization, Patient, Practitioner, PractitionerRole, RelatedPerson @ MedicationRequest.requester",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong requester reference type`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("MedicationRequest", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Condition/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of Device, Organization, " +
                "Patient, Practitioner, PractitionerRole, RelatedPerson @ MedicationRequest.requester",
            exception.message
        )
    }

    @Test
    fun `validate fails with missing requester`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            requester = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: requester is a required element @ MedicationRequest.requester",
            exception.message
        )
    }

    @Test
    fun `validate fails with bad priority`() {
        val medicationRequest = MedicationRequest(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Practitioner/1234".asFHIR()),
            priority = Code("bad")
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationRequest.validate(medicationRequest).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad' is outside of required value set @ MedicationRequest.priority",
            exception.message
        )
    }
}
