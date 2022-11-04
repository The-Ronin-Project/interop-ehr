package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.awt.SystemColor.text
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

class NormalizerTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val extensions = listOf(
        Extension(
            id = "5678",
            url = Uri("http://normalhost/extension"),
            value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/1234"))
        )
    )

    val normalizer = Normalizer::class.objectInstance!!

    private fun normalizeCoding(coding: Coding, parameterName: String = "coding"): Coding? {
        val normalizeCodingMethod = Normalizer::class.functions.find { it.name == "normalizeCoding" }!!
        normalizeCodingMethod.isAccessible = true
        val normalized = normalizeCodingMethod.call(normalizer, coding, parameterName, tenant) as? Coding
        normalizeCodingMethod.isAccessible = false
        return normalized
    }

    private fun normalizeIdentifier(identifier: Identifier, parameterName: String = "identifier"): Identifier? {
        val normalizeIdentifierMethod = Normalizer::class.functions.find { it.name == "normalizeIdentifier" }!!
        normalizeIdentifierMethod.isAccessible = true
        val normalized = normalizeIdentifierMethod.call(normalizer, identifier, parameterName, tenant) as? Identifier
        normalizeIdentifierMethod.isAccessible = false
        return normalized
    }

    @Test
    fun `normalize works for object with no unnormalized types supplied`() {
        val location = Location(id = Id("1234"))
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with no normalization`() {
        val location = Location(
            id = Id("1234"),
            operationalStatus = Coding(
                id = "12345",
                extension = extensions,
                system = Uri("non-normalizable-system"),
                version = "version",
                code = Code("code"),
                display = "Display",
                userSelected = true
            )
        )
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with normalization`() {
        val location = Location(
            id = Id("1234"),
            operationalStatus = Coding(
                id = "12345",
                extension = extensions,
                system = Uri("urn:oid:2.16.840.1.113883.6.1"),
                version = "version",
                code = Code("code"),
                display = "Display",
                userSelected = true
            )
        )
        val normalizedLocation = normalizer.normalize(location, tenant)

        val expectedLocation = Location(
            id = Id("1234"),
            operationalStatus = Coding(
                id = "12345",
                extension = extensions,
                system = Uri("http://loinc.org"),
                version = "version",
                code = Code("code"),
                display = "Display",
                userSelected = true
            )
        )
        assertEquals(expectedLocation, normalizedLocation)
    }

    @Test
    fun `normalizes coding - mapped system with urn`() {
        val coding = Coding(
            id = "12345",
            extension = extensions,
            system = Uri("urn:oid:2.16.840.1.113883.6.1"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        val normalizedCoding = normalizeCoding(coding)
        assertNotEquals(coding, normalizedCoding)

        val expectedCoding = Coding(
            id = "12345",
            extension = extensions,
            system = Uri("http://loinc.org"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        assertEquals(expectedCoding, normalizedCoding)
    }

    @Test
    fun `normalizes coding - mapped system`() {
        val coding = Coding(
            id = "12345",
            extension = extensions,
            system = Uri("2.16.840.1.113883.6.1"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        val normalizedCoding = normalizeCoding(coding)
        assertNotEquals(coding, normalizedCoding)

        val expectedCoding = Coding(
            id = "12345",
            extension = extensions,
            system = Uri("http://loinc.org"),
            version = "version",
            code = Code("code"),
            display = "Display",
            userSelected = true
        )
        assertEquals(expectedCoding, normalizedCoding)
    }

    @Test
    fun `normalizes identifier - mapped system with urn`() {
        val identifier = Identifier(
            id = "12345",
            extension = extensions,
            use = IdentifierUse.OFFICIAL.asCode(),
            type = CodeableConcept(text = "type"),
            system = Uri("urn:oid:2.16.840.1.113883.4.1"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val normalizedIdentifier = normalizeIdentifier(identifier)
        assertNotEquals(extensions, normalizedIdentifier)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = extensions,
            use = IdentifierUse.OFFICIAL.asCode(),
            type = CodeableConcept(text = "type"),
            system = Uri("http://hl7.org/fhir/sid/us-ssn"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, normalizedIdentifier)
    }

    @Test
    fun `normalizes identifier - mapped system`() {
        val identifier = Identifier(
            id = "12345",
            extension = extensions,
            use = IdentifierUse.OFFICIAL.asCode(),
            type = CodeableConcept(text = "type"),
            system = Uri("2.16.840.1.113883.4.1"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        val normalizedIdentifier = normalizeIdentifier(identifier)
        assertNotEquals(extensions, normalizedIdentifier)

        val expectedIdentifier = Identifier(
            id = "12345",
            extension = extensions,
            use = IdentifierUse.OFFICIAL.asCode(),
            type = CodeableConcept(text = "type"),
            system = Uri("http://hl7.org/fhir/sid/us-ssn"),
            value = "value",
            period = Period(start = DateTime("2021")),
            assigner = Reference(display = "assigner")
        )
        assertEquals(expectedIdentifier, normalizedIdentifier)
    }

    @Test
    fun `normalize codeable concept text - text already set - no normalization`() {
        val codeableConcept = CodeableConcept(text = "Text Set")
        // Using medication as a wrapper for the codeable concept.
        val medication = Medication(code = codeableConcept)
        val normalizeMedication = normalizer.normalize(medication, tenant)

        val expectedCodeableConcept = CodeableConcept(text = "Text Set")
        assertEquals(expectedCodeableConcept, normalizeMedication.code)
    }

    @Test
    fun `normalize codeable concept text - no text - pull from single coding`() {
        val codeableConcept = CodeableConcept(coding = listOf(Coding(display = "Coding Display")))
        val medication = Medication(code = codeableConcept)
        val normalizeMedication = normalizer.normalize(medication, tenant)

        val expectedCodeableConcept =
            CodeableConcept(text = "Coding Display", coding = listOf(Coding(display = "Coding Display")))
        assertEquals(expectedCodeableConcept, normalizeMedication.code)
    }

    @Test
    fun `normalize codeable concept text - no text - pull from single coding user selected`() {
        val codeableConcept = CodeableConcept(
            coding = listOf(
                Coding(display = "Coding Display"),
                Coding(display = "Coding Display 2"),
                Coding(display = "User Selected Display", userSelected = true)
            )
        )
        val medication = Medication(code = codeableConcept)
        val normalizeMedication = normalizer.normalize(medication, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                text = "User Selected Display",
                coding = listOf(
                    Coding(display = "Coding Display"),
                    Coding(display = "Coding Display 2"),
                    Coding(display = "User Selected Display", userSelected = true)
                )
            )
        assertEquals(expectedCodeableConcept, normalizeMedication.code)
    }

    @Test
    fun `normalize codeable concept text - no text, multiple codings - no normalization`() {
        val codeableConcept = CodeableConcept(
            coding = listOf(
                Coding(display = "Coding Display"),
                Coding(display = "Coding Display 2")
            )
        )
        val medication = Medication(code = codeableConcept)
        val normalizeMedication = normalizer.normalize(medication, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                coding = listOf(
                    Coding(display = "Coding Display"),
                    Coding(display = "Coding Display 2")
                )
            )
        assertEquals(expectedCodeableConcept, normalizeMedication.code)
    }

    @Test
    fun `normalize codeable concept text - no text, multiple user selected codings - no normalization`() {
        val codeableConcept = CodeableConcept(
            coding = listOf(
                Coding(display = "Coding Display"),
                Coding(display = "Coding Display 2"),
                Coding(display = "User Selected Display 1", userSelected = true),
                Coding(display = "User Selected Display 2", userSelected = true)
            )
        )
        val medication = Medication(code = codeableConcept)
        val normalizeMedication = normalizer.normalize(medication, tenant)

        val expectedCodeableConcept =
            CodeableConcept(
                coding = listOf(
                    Coding(display = "Coding Display"),
                    Coding(display = "Coding Display 2"),
                    Coding(display = "User Selected Display 1", userSelected = true),
                    Coding(display = "User Selected Display 2", userSelected = true)
                )
            )
        assertEquals(expectedCodeableConcept, normalizeMedication.code)
    }
}
