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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
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
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(RoninMedicationStatement.qualifies(MedicationStatement()))
    }

    @Test
    fun `validates Ronin identifiers`() {
        val medicationStatement = MedicationStatement()

        mockkObject(R4MedicationStatementValidator)
        every {
            R4MedicationStatementValidator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        } returns validation { }

        val exception = assertThrows<IllegalArgumentException> {
            RoninMedicationStatement.validate(medicationStatement, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ MedicationStatement.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ MedicationStatement.identifier",
            exception.message
        )

        unmockkObject(R4MedicationStatementValidator)
    }

    @Test
    fun `validates R4 profile`() {
        val medicationStatement = MedicationStatement(
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
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
            RoninMedicationStatement.validate(medicationStatement, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: status is a required element @ MedicationStatement.status",
            exception.message
        )

        unmockkObject(R4MedicationStatementValidator)
    }

    @Test
    fun `validate succeeds`() {
        val medicationStatement = MedicationStatement(
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept()
            ),
            subject = Reference(display = "display")
        )

        RoninMedicationStatement.validate(medicationStatement, null).alertIfErrors()
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val medicationStatement = MedicationStatement(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/medicationstatement.html"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div"
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
            identifier = listOf(Identifier(value = "id")),
            basedOn = listOf(Reference(display = "reference")),
            partOf = listOf(Reference(display = "partOf")),
            status = MedicationStatementStatus.ACTIVE.asCode(),
            statusReason = listOf(CodeableConcept(text = "statusReason")),
            category = CodeableConcept(text = "category"),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication")
            ),
            subject = Reference(display = "subject"),
            context = Reference(display = "context"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            ),
            dateAsserted = DateTime("1905-08-23"),
            informationSource = Reference(display = "informationSource"),
            derivedFrom = listOf(Reference(display = "derivedFrom")),
            reasonCode = listOf(CodeableConcept(text = "reasonCode")),
            reasonReference = listOf(Reference(display = "reasonReference")),
            note = listOf(Annotation(text = Markdown("annotation"))),
            dosage = listOf(Dosage(text = "dosage"))
        )

        val roninMedicationStatement = RoninMedicationStatement.transform(medicationStatement, tenant)

        roninMedicationStatement!!
        assertEquals(Id("test-12345"), roninMedicationStatement.id)
        assertEquals(
            "http://projectronin.io/fhir/ronin.common-fhir-model.uscore-r4/StructureDefinition/ronin-medicationStatement",
            roninMedicationStatement.meta!!.profile[0].value
        )
        assertEquals(medicationStatement.implicitRules, roninMedicationStatement.implicitRules)
        assertEquals(medicationStatement.language, roninMedicationStatement.language)
        assertEquals(medicationStatement.text, roninMedicationStatement.text)
        assertEquals(medicationStatement.contained, roninMedicationStatement.contained)
        assertEquals(medicationStatement.extension, roninMedicationStatement.extension)
        assertEquals(medicationStatement.modifierExtension, roninMedicationStatement.modifierExtension)
        assertEquals(3, roninMedicationStatement.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            roninMedicationStatement.identifier
        )
        assertEquals(medicationStatement.basedOn, roninMedicationStatement.basedOn)
        assertEquals(medicationStatement.partOf, roninMedicationStatement.partOf)
        assertEquals(medicationStatement.status, roninMedicationStatement.status)
        assertEquals(medicationStatement.statusReason, roninMedicationStatement.statusReason)
        assertEquals(medicationStatement.category, roninMedicationStatement.category)
        assertEquals(medicationStatement.medication, roninMedicationStatement.medication)
        assertEquals(medicationStatement.subject, roninMedicationStatement.subject)
        assertEquals(medicationStatement.context, roninMedicationStatement.context)
        assertEquals(medicationStatement.effective, roninMedicationStatement.effective)
        assertEquals(medicationStatement.informationSource, roninMedicationStatement.informationSource)
        assertEquals(medicationStatement.derivedFrom, roninMedicationStatement.derivedFrom)
        assertEquals(medicationStatement.reasonCode, roninMedicationStatement.reasonCode)
        assertEquals(medicationStatement.reasonReference, roninMedicationStatement.reasonReference)
        assertEquals(medicationStatement.note, roninMedicationStatement.note)
        assertEquals(medicationStatement.dosage, roninMedicationStatement.dosage)
    }

    @Test
    fun `transform succeeds with just required attributes`() {
        val medicationStatement = MedicationStatement(
            id = Id("12345"),
            status = MedicationStatementStatus.ACTIVE.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.CODEABLE_CONCEPT,
                value = CodeableConcept(text = "medication")
            ),
            subject = Reference(display = "subject"),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val roninMedicationStatement = RoninMedicationStatement.transform(medicationStatement, tenant)

        roninMedicationStatement!!
        assertEquals(2, roninMedicationStatement.identifier.size)
        assertEquals(
            listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            roninMedicationStatement.identifier
        )
        assertEquals(medicationStatement.status, roninMedicationStatement.status)
        assertEquals(medicationStatement.medication, roninMedicationStatement.medication)
        assertEquals(medicationStatement.subject, roninMedicationStatement.subject)
        assertEquals(medicationStatement.effective, roninMedicationStatement.effective)
    }

    @Test
    fun `transform can handle all dynamic types of medication`() {
        fun testMedication(type: DynamicValueType, value: Any) {
            val medicationStatement = MedicationStatement(
                id = Id("12345"),
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication = DynamicValue(
                    type = type,
                    value = value
                ),
                subject = Reference(display = "subject"),
                effective = DynamicValue(
                    type = DynamicValueType.DATE_TIME,
                    value = DateTime("1905-08-23")
                )
            )
            val roninMedicationStatement = RoninMedicationStatement.transform(medicationStatement, tenant)
            assertEquals(medicationStatement.medication, roninMedicationStatement!!.medication)
        }

        testMedication(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "medication"))
        testMedication(DynamicValueType.REFERENCE, Reference(display = "reference"))
    }

    @Test
    fun `transform can handle all dynamic types of effective`() {
        fun testMedication(type: DynamicValueType, value: Any) {
            val medicationStatement = MedicationStatement(
                id = Id("12345"),
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication = DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value = CodeableConcept(text = "codeableConcep")
                ),
                subject = Reference(display = "subject"),
                effective = DynamicValue(
                    type = type,
                    value = value
                )
            )
            val roninMedicationStatement = RoninMedicationStatement.transform(medicationStatement, tenant)
            assertEquals(medicationStatement.medication, roninMedicationStatement!!.medication)
        }

        testMedication(DynamicValueType.DATE_TIME, DateTime("2022-10-14"))
        testMedication(DynamicValueType.PERIOD, Period(start = DateTime("2022-10-14")))
    }

    @Test
    fun `transform fails with missing attributes`() {
        val medicationStatement = MedicationStatement()
        val roninMedicationStatement = RoninMedicationStatement.transform(medicationStatement, tenant)
        assertNull(roninMedicationStatement)
    }
}
