package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.DynamicValues
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.medicationReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.resource.reportedReferenceOptions
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.test.data.generator.NullDataGenerator
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninDynamicValueUtilTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `generate rcdm optional reference as null when null input and no type and no id`() {
        val reported =
            generateOptionalDynamicValueReference(
                null,
                reportedReferenceOptions,
                tenant.mnemonic,
            )
        assertNull(reported)
    }

    @Test
    fun `generate rcdm optional reference as null when nullish input and no type and no id`() {
        val reported =
            generateOptionalDynamicValueReference(
                NullDataGenerator<DynamicValue<Any>>().generate(),
                reportedReferenceOptions,
                tenant.mnemonic,
            )
        assertNull(reported)
    }

    @Test
    fun `generate rcdm optional reference as null when invalid input and no type and no id`() {
        val initialValue = DynamicValues.reference(reference("Patient"))
        assertNull(initialValue.value.type?.extension)

        val reported =
            generateOptionalDynamicValueReference(
                initialValue,
                reportedReferenceOptions,
                tenant.mnemonic,
            )
        assertNull(reported)
    }

    @Test
    fun `generate rcdm optional reference when invalid input and valid type and no id`() {
        val initialValue = DynamicValues.reference(reference("Patient"))
        assertNull(initialValue.value.type?.extension)

        val reported =
            generateOptionalDynamicValueReference(
                initialValue,
                reportedReferenceOptions,
                tenant.mnemonic,
                "Patient",
            )
        val actualValue = reported?.value as Reference
        assertEquals("Patient", actualValue.decomposedType())
        assertEquals(actualValue.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm optional reference as null when invalid input and invalid type and no id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertNull(initialValue.value.type?.extension)

        val exception =
            assertThrows<IllegalArgumentException> {
                generateOptionalDynamicValueReference(
                    initialValue,
                    reportedReferenceOptions,
                    tenant.mnemonic,
                    "Medication",
                )
            }
        assertEquals(
            "Medication is not one of Practitioner, Organization, Patient, PractitionerRole",
            exception.message,
        )
    }

    @Test
    fun `generate rcdm optional reference when invalid input and valid type and empty id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertNull(initialValue.value.type?.extension)

        val reported =
            generateOptionalDynamicValueReference(
                initialValue,
                reportedReferenceOptions,
                tenant.mnemonic,
                "Patient",
                "",
            )
        val actualValue = reported?.value as Reference
        assertEquals("Patient", actualValue.decomposedType())
        assertEquals(actualValue.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm optional reference when invalid input and valid type and valid id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertNull(initialValue.value.type?.extension)

        val reported =
            generateOptionalDynamicValueReference(
                initialValue,
                reportedReferenceOptions,
                tenant.mnemonic,
                "Patient",
                "1234",
            )
        val actualValue = reported?.value as Reference
        assertEquals("Patient", actualValue.decomposedType())
        assertEquals(actualValue.type?.extension, dataAuthorityExtension)
        assertEquals("Patient/test-1234".asFHIR(), actualValue.reference)
    }

    @Test
    fun `accept valid rcdm required reference if provided`() {
        val initialValue = DynamicValues.reference(rcdmReference("Medication", "1234"))
        val medication =
            generateDynamicValueReference(
                initialValue,
                medicationReferenceOptions,
                tenant.mnemonic,
            )
        assertTrue(medication.value.decomposedType() in medicationReferenceOptions)
        assertEquals(dataAuthorityExtension, medication.value.type?.extension)
        assertEquals("Medication/1234".asFHIR(), medication.value.reference)
    }

    @Test
    fun `generate rcdm required reference when invalid input and no type and no id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertTrue(initialValue.value.decomposedType() in medicationReferenceOptions)
        assertNull(initialValue.value.type?.extension)

        val medication =
            generateDynamicValueReference(
                initialValue,
                medicationReferenceOptions,
                tenant.mnemonic,
            )
        assertTrue(medication.value.decomposedType() in medicationReferenceOptions)
        assertEquals(medication.value.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm required reference when invalid input and valid type and no id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertTrue(initialValue.value.decomposedType() in medicationReferenceOptions)
        assertNull(initialValue.value.type?.extension)

        val medication =
            generateDynamicValueReference(
                initialValue,
                medicationReferenceOptions,
                tenant.mnemonic,
                "Medication",
            )
        assertEquals("Medication", medication.value.decomposedType())
        assertEquals(medication.value.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm required reference when invalid input and invalid type and no id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertTrue(initialValue.value.decomposedType() in medicationReferenceOptions)
        assertNull(initialValue.value.type?.extension)

        val exception =
            assertThrows<IllegalArgumentException> {
                generateDynamicValueReference(
                    initialValue,
                    medicationReferenceOptions,
                    tenant.mnemonic,
                    "Patient",
                )
            }
        assertEquals(
            "Patient is not Medication",
            exception.message,
        )
    }

    @Test
    fun `generate rcdm required reference when invalid input and valid type and empty id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertTrue(initialValue.value.decomposedType() in medicationReferenceOptions)
        assertNull(initialValue.value.type?.extension)

        val medication =
            generateDynamicValueReference(
                initialValue,
                medicationReferenceOptions,
                tenant.mnemonic,
                "Medication",
                "",
            )
        assertEquals("Medication", medication.value.decomposedType())
        assertEquals(medication.value.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm required reference when invalid input and valid type and valid id`() {
        val initialValue = DynamicValues.reference(reference("Medication"))
        assertTrue(initialValue.value.decomposedType() in medicationReferenceOptions)
        assertNull(initialValue.value.type?.extension)

        val medication =
            generateDynamicValueReference(
                initialValue,
                medicationReferenceOptions,
                tenant.mnemonic,
                "Medication",
                "1234",
            )
        assertEquals("Medication", medication.value.decomposedType())
        assertEquals(medication.value.type?.extension, dataAuthorityExtension)
        assertEquals("Medication/test-1234".asFHIR(), medication.value.reference)
    }
}
