package com.projectronin.interop.fhir.ronin.resource

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
import com.projectronin.interop.fhir.r4.datatype.medication.DispenseRequest
import com.projectronin.interop.fhir.r4.datatype.medication.Substitution
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
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.RequestPriority
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
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

class RoninMedicationRequestTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            RoninMedicationRequest.qualifies(
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
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedicationRequest.validate(medicationRequest, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ MedicationRequest.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ MedicationRequest.identifier",
            exception.message
        )
    }

    @Test
    fun `validates requester is provided`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedicationRequest.validate(medicationRequest, null).alertIfErrors()
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
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
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
            RoninMedicationRequest.validate(medicationRequest, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: authoredOn is a required element @ MedicationRequest.authoredOn",
            exception.message
        )

        unmockkObject(R4MedicationRequestValidator)
    }

    @Test
    fun `validate succeeds`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            status = MedicationRequestStatus.COMPLETED.asCode(),
            intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        RoninMedicationRequest.validate(medicationRequest, null).alertIfErrors()
    }

    @Test
    fun `transforms medication request with all attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("MedicationRequest")),
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
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

        val transformed = RoninMedicationRequest.transform(medicationRequest, tenant)

        transformed!!
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value))),
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
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
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
        assertEquals(Reference(reference = "Patient/test-1234".asFHIR()), transformed.subject)
        assertEquals(Reference(reference = "Encounter/test-1234".asFHIR()), transformed.encounter)
        assertEquals(listOf(Reference(reference = "Condition/test-1234".asFHIR())), transformed.supportingInformation)
        assertEquals(DateTime("2022-11-03"), transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/test-1234".asFHIR()), transformed.requester)
        assertEquals(Reference(reference = "Practitioner/test-5678".asFHIR()), transformed.performer)
        assertEquals(CodeableConcept(text = "performer type".asFHIR()), transformed.performerType)
        assertEquals(Reference(reference = "Practitioner/test-3456".asFHIR()), transformed.recorder)
        assertEquals(listOf(CodeableConcept(text = "reason".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(reference = "Condition/test-5678".asFHIR())), transformed.reasonReference)
        assertEquals(listOf(Canonical("canonical")), transformed.instantiatesCanonical)
        assertEquals(listOf(Uri("uri")), transformed.instantiatesUri)
        assertEquals(listOf(Reference(reference = "CarePlan/test-1234".asFHIR())), transformed.basedOn)
        assertEquals(Identifier(value = "group".asFHIR()), transformed.groupIdentifier)
        assertEquals(CodeableConcept(text = "therapy".asFHIR()), transformed.courseOfTherapyType)
        assertEquals(listOf(Reference(reference = "Coverage/test-1234".asFHIR())), transformed.insurance)
        assertEquals(listOf(Annotation(text = Markdown("note"))), transformed.note)
        assertEquals(listOf(Dosage(text = "dosage".asFHIR())), transformed.dosageInformation)
        assertEquals(DispenseRequest(numberOfRepeatsAllowed = UnsignedInt(2)), transformed.dispenseRequest)
        assertEquals(Substitution(allowed = DynamicValue(DynamicValueType.BOOLEAN, true)), transformed.substitution)
        assertEquals(Reference(reference = "MedicationRequest/test-1234".asFHIR()), transformed.priorPrescription)
        assertEquals(listOf(Reference(reference = "DetectedIssue/test-1234".asFHIR())), transformed.detectedIssue)
        assertEquals(listOf(Reference(reference = "Provenance/test-1234".asFHIR())), transformed.eventHistory)
    }

    @Test
    fun `transforms medication request with only required attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            requester = Reference(reference = "Practitioner/1234".asFHIR()),
        )

        val transformed = RoninMedicationRequest.transform(medicationRequest, tenant)

        transformed!!
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value))),
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
                    type = RoninCodeableConcepts.FHIR_ID,
                    system = RoninCodeSystem.FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = RoninCodeableConcepts.TENANT,
                    system = RoninCodeSystem.TENANT.uri,
                    value = "test".asFHIR()
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
        assertEquals(Reference(reference = "Patient/test-1234".asFHIR()), transformed.subject)
        assertNull(transformed.encounter)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/test-1234".asFHIR()), transformed.requester)
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
            subject = Reference(reference = "Patient/1234".asFHIR()),
            requester = Reference(reference = "Practitioner/1234".asFHIR()),
        )

        val transformed = RoninMedicationRequest.transform(medicationRequest, tenant)
        assertNull(transformed)
    }
}
