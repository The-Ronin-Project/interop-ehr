package com.projectronin.interop.fhir.ronin.resource.diagnosticReport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Attachment
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
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.DiagnosticReportMedia
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.util.localizeReferenceTest
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

class RoninDiagnosticReportNoteExchangeTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "reference".asFHIR(), // r4 required?
        reference = "Patient/123".asFHIR()
    )
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninDiagnosticReport = RoninDiagnosticReportNoteExchange(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninDiagnosticReport.qualifies(
                DiagnosticReport(
                    code = CodeableConcept(text = "dx report".asFHIR()),
                    category = listOf(CodeableConcept(text = "dx report".asFHIR())),
                    status = Code("registered")
                )
            )
        )
    }

    @Test
    fun `validate fails without ronin identifiers`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/123".asFHIR()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ DiagnosticReport.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ DiagnosticReport.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ DiagnosticReport.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails without id`() {
        val dxReport = DiagnosticReport(
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR()
            )
        )

        val (transformed, _) = roninDiagnosticReport.transform(dxReport, tenant)

        assertNull(transformed)
    }

    @Test
    fun `validate fails with no subject provided`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ DiagnosticReport.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with no category provided`() {
        val dxReport = DiagnosticReport(
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
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                id = "subject".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: category is a required element @ DiagnosticReport.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with no subject type`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ DiagnosticReport.subject.",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject type but no data authority extension identifier`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                id = "subject".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient")
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ DiagnosticReport.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate against R4 profile for DiagnosticReport with missing attributes`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                id = "subject".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/123".asFHIR()
            )
        )

        mockkObject(R4DiagnosticReportValidator)
        every {
            R4DiagnosticReportValidator.validate(
                dxReport,
                LocationContext(DiagnosticReport::class)
            )
        } returns validation {
            checkTrue(
                false,
                RequiredFieldError(DiagnosticReport::category),
                LocationContext(DiagnosticReport::class)
            )
            checkNotNull(
                null,
                RequiredFieldError(DiagnosticReport::subject),
                LocationContext(DiagnosticReport::class)
            )
        }

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: category is a required element @ DiagnosticReport.category\n" +
                "ERROR REQ_FIELD: subject is a required element @ DiagnosticReport.subject",
            exception.message
        )

        unmockkObject(R4OrganizationValidator)
    }

    @Test
    fun `validate is successful with required attributes`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                id = "subject".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/123".asFHIR()
            )
        )

        roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
    }

    @Test
    fun `transform diagnostic report with all attributes`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/diagnosticreport.html"))
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
            basedOn = listOf(
                Reference(id = "basedOnId".asFHIR(), display = "basedOnDisplay".asFHIR())
            ),
            status = Code("registered"),
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            subject = localizeReferenceTest(mockReference), // check transform
            encounter = Reference(id = "encounterReference".asFHIR(), display = "encounterDisplay".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2018-02-02T00:00:00Z"),
            performer = listOf(
                Reference(id = "performerId".asFHIR(), display = "performerDisplay".asFHIR())
            ),
            resultsInterpreter = listOf(
                Reference(id = "resultsInterpreter".asFHIR(), display = "resultsInterpreterDisplay".asFHIR())
            ),
            specimen = listOf(
                Reference(id = "specimenId".asFHIR(), display = "specimenDisplay".asFHIR())
            ),
            result = listOf(
                Reference(id = "resultId".asFHIR(), display = "resultDisplay".asFHIR())
            ),
            imagingStudy = listOf(
                Reference(id = "imagingStudyId".asFHIR(), display = "imagingStudyDisplay".asFHIR())
            ),
            media = listOf(
                DiagnosticReportMedia(
                    id = "mediaId".asFHIR(),
                    link = Reference(id = "linkId".asFHIR(), display = "linkDisplay".asFHIR())
                )
            ),
            conclusion = "conclusionFhirString".asFHIR(),
            conclusionCode = listOf(
                CodeableConcept(text = "conclusionCode".asFHIR())
            ),
            presentedForm = listOf(
                Attachment(id = "attachmentId".asFHIR())
            )
        )

        val (transformed, validation) = roninDiagnosticReport.transform(dxReport, tenant)
        validation.alertIfErrors()
        transformed!!

        assertEquals("DiagnosticReport", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_NOTE_EXCHANGE.value))),
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
                Reference(id = "basedOnId".asFHIR(), display = "basedOnDisplay".asFHIR())
            ),
            transformed.basedOn
        )
        assertEquals(Code("registered"), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                text = "dx report".asFHIR()
            ),
            transformed.code
        )
        assertEquals(
            Reference(
                reference = "Patient/test-123".asFHIR(),
                display = "reference".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(
            Reference(
                id = "encounterReference".asFHIR(),
                display = "encounterDisplay".asFHIR()
            ),
            transformed.encounter
        )
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(
            Instant("2018-02-02T00:00:00Z"),
            transformed.issued
        )
        assertEquals(
            listOf(
                Reference(id = "performerId".asFHIR(), display = "performerDisplay".asFHIR())
            ),
            transformed.performer
        )
        assertEquals(
            listOf(
                Reference(id = "resultsInterpreter".asFHIR(), display = "resultsInterpreterDisplay".asFHIR())
            ),
            transformed.resultsInterpreter
        )
        assertEquals(
            listOf(
                Reference(id = "specimenId".asFHIR(), display = "specimenDisplay".asFHIR())
            ),
            transformed.specimen
        )
        assertEquals(
            listOf(
                Reference(id = "resultId".asFHIR(), display = "resultDisplay".asFHIR())
            ),
            transformed.result
        )
        assertEquals(
            listOf(
                Reference(id = "imagingStudyId".asFHIR(), display = "imagingStudyDisplay".asFHIR())
            ),
            transformed.imagingStudy
        )
        assertEquals(
            listOf(
                DiagnosticReportMedia(
                    id = "mediaId".asFHIR(),
                    link = Reference(id = "linkId".asFHIR(), display = "linkDisplay".asFHIR())
                )
            ),
            transformed.media
        )
        assertEquals("conclusionFhirString".asFHIR(), transformed.conclusion)
        assertEquals(
            listOf(
                CodeableConcept(text = "conclusionCode".asFHIR())
            ),
            transformed.conclusionCode
        )
        assertEquals(
            listOf(
                Attachment(id = "attachmentId".asFHIR())
            ),
            transformed.presentedForm
        )
    }

    @Test
    fun `transform diagnostic report with only required attributes`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = localizeReferenceTest(mockReference) // check transform
        )

        val (transformed, validation) = roninDiagnosticReport.transform(dxReport, tenant)
        validation.alertIfErrors()
        transformed!!

        assertEquals("DiagnosticReport", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_NOTE_EXCHANGE.value))),
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(Code("registered"), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                text = "dx report".asFHIR()
            ),
            transformed.code
        )
        assertEquals(
            Reference(
                display = "reference".asFHIR(),
                reference = "Patient/test-123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(null, transformed.encounter)
        assertEquals(null, transformed.effective)
        assertEquals(null, transformed.issued)
        assertEquals(listOf<Reference>(), transformed.performer)
        assertEquals(listOf<Reference>(), transformed.resultsInterpreter)
        assertEquals(listOf<Reference>(), transformed.specimen)
        assertEquals(listOf<Reference>(), transformed.result)
        assertEquals(listOf<Reference>(), transformed.imagingStudy)
        assertEquals(listOf<DiagnosticReportMedia>(), transformed.media)
        assertEquals(null, transformed.conclusion)
        assertEquals(listOf<CodeableConcept>(), transformed.conclusionCode)
        assertEquals(listOf<Attachment>(), transformed.presentedForm)
    }

    @Test
    fun `validate fails with missing reference attribute`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                id = "123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("DiagnosisReport", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ DiagnosticReport.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong reference type`() {
        val dxReport = DiagnosticReport(
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
            category = listOf(
                CodeableConcept(text = "dx report".asFHIR())
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Condition/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport, null).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ DiagnosticReport.subject",
            exception.message
        )
    }
}
