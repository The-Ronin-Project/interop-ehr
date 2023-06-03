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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus
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

class RoninMedicationStatementTest {
    private val mockReference = Reference(
        display = "reference".asFHIR(), // r4 required?
        reference = "Patient/1234".asFHIR()
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
    private val roninMedicationStatement = RoninMedicationStatement(normalizer, localizer)

    @Test
    fun `always qualifies`() {
        assertTrue(roninMedicationStatement.qualifies(MedicationStatement()))
    }

    @Test
    fun `validates Ronin identifiers`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source"))
        )

        mockkObject(R4MedicationStatementValidator)
        every {
            R4MedicationStatementValidator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        } returns validation { }

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ MedicationStatement.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ MedicationStatement.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ MedicationStatement.identifier",
            exception.message
        )

        unmockkObject(R4MedicationStatementValidator)
    }

    @Test
    fun `validates R4 profile`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
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
            )
        )

        mockkObject(R4MedicationStatementValidator)
        every {
            R4MedicationStatementValidator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(MedicationStatement::status),
                LocationContext(MedicationStatement::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: status is a required element @ MedicationStatement.status",
            exception.message
        )

        unmockkObject(R4MedicationStatementValidator)
    }

    @Test
    fun `validate fails with subject but no type`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept()
            ),
            subject = Reference(reference = "Patient/1234".asFHIR())
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ MedicationStatement.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject and type but no data authority reference extension`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept()
            ),
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient"))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ MedicationStatement.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val medicationStatement = MedicationStatement(
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept()
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ MedicationStatement.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept()
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        roninMedicationStatement.validate(medicationStatement).alertIfErrors()
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val medicationStatement = MedicationStatement(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/medicationstatement.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
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
            basedOn = listOf(Reference(display = "reference".asFHIR())),
            partOf = listOf(Reference(display = "partOf".asFHIR())),
            status = MedicationStatementStatus.ACTIVE.asCode(),
            statusReason = listOf(CodeableConcept(text = "statusReason".asFHIR())),
            category = CodeableConcept(text = "category".asFHIR()),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication".asFHIR())
            ),
            subject = localizeReferenceTest(mockReference), // check that it transforms
            context = Reference(display = "context".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            ),
            dateAsserted = DateTime("1905-08-23"),
            informationSource = Reference(display = "informationSource".asFHIR()),
            derivedFrom = listOf(Reference(display = "derivedFrom".asFHIR())),
            reasonCode = listOf(CodeableConcept(text = "reasonCode".asFHIR())),
            reasonReference = listOf(Reference(display = "reasonReference".asFHIR())),
            note = listOf(Annotation(text = Markdown("annotation"))),
            dosage = listOf(Dosage(text = "dosage".asFHIR()))
        )

        val (transformed, validation) = roninMedicationStatement.transform(medicationStatement, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_STATEMENT.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medicationStatement.implicitRules, transformed.implicitRules)
        assertEquals(medicationStatement.language, transformed.language)
        assertEquals(medicationStatement.text, transformed.text)
        assertEquals(medicationStatement.contained, transformed.contained)
        assertEquals(medicationStatement.extension, transformed.extension)
        assertEquals(medicationStatement.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
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
        assertEquals(medicationStatement.basedOn, transformed.basedOn)
        assertEquals(medicationStatement.partOf, transformed.partOf)
        assertEquals(medicationStatement.status, transformed.status)
        assertEquals(medicationStatement.statusReason, transformed.statusReason)
        assertEquals(medicationStatement.category, transformed.category)
        assertEquals(medicationStatement.medication, transformed.medication)
        assertEquals(medicationStatement.subject, transformed.subject)
        assertEquals(medicationStatement.context, transformed.context)
        assertEquals(medicationStatement.effective, transformed.effective)
        assertEquals(medicationStatement.informationSource, transformed.informationSource)
        assertEquals(medicationStatement.derivedFrom, transformed.derivedFrom)
        assertEquals(medicationStatement.reasonCode, transformed.reasonCode)
        assertEquals(medicationStatement.reasonReference, transformed.reasonReference)
        assertEquals(medicationStatement.note, transformed.note)
        assertEquals(medicationStatement.dosage, transformed.dosage)
    }

    @Test
    fun `transform succeeds with just required attributes`() {
        val medicationStatement = MedicationStatement(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication".asFHIR())
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val (transformed, validation) = roninMedicationStatement.transform(medicationStatement, tenant)
        validation.alertIfErrors()

        transformed!!
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
        assertEquals(medicationStatement.status, transformed.status)
        assertEquals(medicationStatement.medication, transformed.medication)
        assertEquals(medicationStatement.subject, transformed.subject)
        assertEquals(medicationStatement.effective, transformed.effective)
    }

    @Test
    fun `transform can handle all dynamic types of medication`() {
        fun testMedication(type: DynamicValueType, value: Any) {
            val medicationStatement = MedicationStatement(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication = DynamicValue(
                    type = type,
                    value = value
                ),
                subject = Reference(
                    display = "subject".asFHIR(),
                    reference = "Patient/1234".asFHIR(),
                    type = Uri("Patient", extension = dataAuthorityExtension)
                ),
                effective = DynamicValue(
                    type = DynamicValueType.DATE_TIME,
                    value = DateTime("1905-08-23")
                )
            )
            val (transformed, validation) = roninMedicationStatement.transform(medicationStatement, tenant)
            validation.alertIfErrors()
            assertEquals(medicationStatement.medication, transformed!!.medication)
        }

        testMedication(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication".asFHIR()))
        testMedication(DynamicValueType.REFERENCE, Reference(display = "reference".asFHIR()))
    }

    @Test
    fun `transform can handle all dynamic types of effective`() {
        fun testMedication(type: DynamicValueType, value: Any) {
            val medicationStatement = MedicationStatement(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication = DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value = CodeableConcept(text = "codeableConcep".asFHIR())
                ),
                subject = Reference(
                    display = "subject".asFHIR(),
                    reference = "Patient/1234".asFHIR(),
                    type = Uri("Patient", extension = dataAuthorityExtension)
                ),
                effective = DynamicValue(
                    type = type,
                    value = value
                )
            )
            val (transformed, validation) = roninMedicationStatement.transform(medicationStatement, tenant)
            validation.alertIfErrors()
            assertEquals(medicationStatement.medication, transformed!!.medication)
        }

        testMedication(DynamicValueType.DATE_TIME, DateTime("2022-10-14"))
        testMedication(DynamicValueType.PERIOD, Period(start = DateTime("2022-10-14")))
    }

    @Test
    fun `transform fails with missing attributes`() {
        val medicationStatement = MedicationStatement()
        val (transformed, _) = roninMedicationStatement.transform(medicationStatement, tenant)
        assertNull(transformed)
    }

    @Test
    fun `validate fails with missing subject reference attribute`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
            identifier = listOf(
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication".asFHIR())
            ),
            subject = Reference(display = "subject".asFHIR(), type = Uri("", extension = dataAuthorityExtension)),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ MedicationStatement.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with wrong subject reference type`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
            identifier = listOf(
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication".asFHIR())
            ),
            subject = Reference(
                reference = "Condition/12345".asFHIR(),
                type = Uri("", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not Patient @ MedicationStatement.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails with missing subject`() {
        val medicationStatement = MedicationStatement(
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)), source = Uri("source")),
            identifier = listOf(
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
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication".asFHIR())
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninMedicationStatement.validate(medicationStatement).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ MedicationStatement.subject",
            exception.message
        )
    }
}
