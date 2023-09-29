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
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.DiagnosticReportMedia
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.diagnosticReport.RoninDiagnosticReportLaboratory
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

class RoninDiagnosticReportLaboratoryTest {
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
    private val roninDiagnosticReport = RoninDiagnosticReportLaboratory(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninDiagnosticReport.qualifies(
                DiagnosticReport(
                    code = CodeableConcept(text = "dx report".asFHIR()),
                    status = Code("registered"),
                    category = listOf(
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                                    code = Code("LAB")
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `validate fails without ronin identifiers`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val (transformed, _) = roninDiagnosticReport.transform(dxReport, tenant)

        assertNull(transformed)
    }

    @Test
    fun `validate fails with no subject provided`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ DiagnosticReport.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with no subject type provided`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            subject = Reference(display = "something".asFHIR(), reference = "Patient/1234".asFHIR()),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ DiagnosticReport.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject type but no data authority extension identifier`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Condition/something".asFHIR(), type = Uri("Condition")),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered")
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ DiagnosticReport.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate fails with no category provided`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: category is a required element @ DiagnosticReport.category\n" +
                "ERROR USCORE_DX_RPT_001: Must match this system|code: http://terminology.hl7.org/CodeSystem/v2-0074|LAB @ DiagnosticReport.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong category code provided`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("NOT-LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_DX_RPT_001: Must match this system|code: http://terminology.hl7.org/CodeSystem/v2-0074|LAB @ DiagnosticReport.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong category system provided`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074-wrong"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_DX_RPT_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/v2-0074|LAB @ DiagnosticReport.category",
            exception.message
        )
    }

    @Test
    fun `validate against R4 profile for DiagnosticReport with missing attributes`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
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
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
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
    fun `validate checks meta`() {
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ DiagnosticReport.meta",
            exception.message
        )
    }

    @Test
    fun `validate is successful with required attributes`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                display = "display".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        roninDiagnosticReport.validate(dxReport).alertIfErrors()
    }

    @Test
    fun `transform diagnostic report with all attributes`() {
        val dxReport = DiagnosticReport(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/diagnosticreport.html")),
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
            identifier = listOf(Identifier(value = "id".asFHIR())),
            basedOn = listOf(
                Reference(id = "basedOnId".asFHIR(), display = "basedOnDisplay".asFHIR())
            ),
            status = Code("registered"),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            subject = localizeReferenceTest(mockReference), // check it transforms correctly
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
            Meta(profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)), source = Uri("source")),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
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
            meta = Meta(source = Uri("source")),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = localizeReferenceTest(mockReference) // check that it transforms
        )

        val (transformed, validation) = roninDiagnosticReport.transform(dxReport, tenant)
        validation.alertIfErrors()
        transformed!!

        assertEquals("DiagnosticReport", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
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
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)),
                source = Uri("source")
            ),
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
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                            code = Code("LAB")
                        )
                    )
                )
            ),
            code = CodeableConcept(text = "dx report".asFHIR()),
            status = Code("registered"),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                id = "subject".asFHIR(),
                display = "display".asFHIR()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninDiagnosticReport.validate(dxReport).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ DiagnosticReport.subject.type",
            exception.message
        )
    }
}
