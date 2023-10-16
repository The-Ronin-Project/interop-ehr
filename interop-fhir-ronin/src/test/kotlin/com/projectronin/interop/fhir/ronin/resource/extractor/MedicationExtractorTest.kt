package com.projectronin.interop.fhir.ronin.resource.extractor

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Ingredient
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.Resource
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MedicationExtractorTest {
    private val extractor = MedicationExtractor()

    @Test
    fun `null medication returns null`() {
        val medicationDynamicValue: DynamicValue<Any>? = null
        val contained = listOf<Resource<*>>(mockk<Medication>())
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `null resourceId returns null`() {
        val medicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("Medication/1234")))
        val contained = listOf<Resource<*>>()
        val resourceId: Id? = null

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `resourceId with null value returns null`() {
        val medicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("Medication/1234")))
        val contained = listOf<Resource<*>>()
        val resourceId = Id(null)

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `medication with unsupported type returns null`() {
        val medicationDynamicValue = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
        val contained = listOf<Resource<*>>()
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `medication codeable concept with user selected coding generates id based off user selected`() {
        val codeableConcept = CodeableConcept(
            text = FHIRString("acetaminophen"),
            coding = listOf(
                Coding(
                    system = Uri("http://www.nlm.nih.gov/research/umls/rxnorm"),
                    code = Code("161"),
                    display = FHIRString("Acetaminophen"),
                    userSelected = FHIRBoolean.FALSE
                ),
                Coding(
                    system = Uri("https://fhir.cerner.com/accountId/synonym"),
                    code = Code("2748023"),
                    display = FHIRString("acetaminophen"),
                    userSelected = FHIRBoolean.TRUE
                )
            )
        )
        val medicationDynamicValue = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, codeableConcept)
        val contained = listOf<Resource<*>>()
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        extraction!!

        val expectedMedication = Medication(
            id = Id("codeable-1234-2748023"),
            code = codeableConcept
        )
        assertEquals(expectedMedication, extraction.extractedMedication)
        assertEquals(
            DynamicValue(
                DynamicValueType.REFERENCE,
                Reference(reference = FHIRString("Medication/codeable-1234-2748023"))
            ),
            extraction.updatedMedication
        )
        assertNull(extraction.updatedContained)
    }

    @Test
    fun `medication codeable concept with no user selected codings generates id based off all codings`() {
        val codeableConcept = CodeableConcept(
            text = FHIRString("acetaminophen"),
            coding = listOf(
                Coding(
                    system = Uri("http://www.nlm.nih.gov/research/umls/rxnorm"),
                    code = Code("161"),
                    display = FHIRString("Acetaminophen"),
                    userSelected = FHIRBoolean.FALSE
                ),
                Coding(
                    system = Uri("https://fhir.cerner.com/accountId/synonym"),
                    code = Code("2748023"),
                    display = FHIRString("acetaminophen"),
                    userSelected = null
                )
            )
        )
        val medicationDynamicValue = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, codeableConcept)
        val contained = listOf<Resource<*>>()
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        extraction!!

        val expectedMedication = Medication(
            id = Id("codeable-1234-161-2748023"),
            code = codeableConcept
        )
        assertEquals(expectedMedication, extraction.extractedMedication)
        assertEquals(
            DynamicValue(
                DynamicValueType.REFERENCE,
                Reference(reference = FHIRString("Medication/codeable-1234-161-2748023"))
            ),
            extraction.updatedMedication
        )
        assertNull(extraction.updatedContained)
    }

    @Test
    fun `medication reference with no contained returns null`() {
        val medicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("Medication/1234")))
        val contained = listOf<Resource<*>>()
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `medication reference without local reference returns null`() {
        val medicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("Medication/1234")))
        val contained = listOf<Resource<*>>(mockk<Medication>())
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        assertNull(extraction)
    }

    @Test
    fun `medication reference with local reference to contained resource updates appropriately`() {
        val medicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("#5678")))

        val ingredient = Ingredient(
            item = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = FHIRString("acetaminophen"),
                    coding = listOf(
                        Coding(
                            system = Uri("http://www.nlm.nih.gov/research/umls/rxnorm"),
                            code = Code("161"),
                            display = FHIRString("Acetaminophen"),
                            userSelected = FHIRBoolean.FALSE
                        ),
                        Coding(
                            system = Uri("https://fhir.cerner.com/accountId/synonym"),
                            code = Code("2748023"),
                            display = FHIRString("acetaminophen"),
                            userSelected = FHIRBoolean.TRUE
                        )
                    )
                )
            )
        )
        val containedMedication = Medication(
            id = Id("5678"),
            code = CodeableConcept(text = FHIRString("acetaminophen")),
            ingredient = listOf(ingredient)
        )
        val otherContained = mockk<Organization>()
        val contained = listOf<Resource<*>>(containedMedication, otherContained)
        val resourceId = Id("1234")

        val extraction = extractor.extractMedication(medicationDynamicValue, contained, resourceId)
        extraction!!

        val expectedMedication = Medication(
            id = Id("contained-1234-5678"),
            code = CodeableConcept(text = FHIRString("acetaminophen")),
            ingredient = listOf(ingredient)
        )
        assertEquals(expectedMedication, extraction.extractedMedication)
        assertEquals(
            DynamicValue(
                DynamicValueType.REFERENCE,
                Reference(reference = FHIRString("Medication/contained-1234-5678"))
            ),
            extraction.updatedMedication
        )
        assertEquals(listOf(otherContained), extraction.updatedContained)
    }
}
