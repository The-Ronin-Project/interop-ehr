package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ReferenceGenerator
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.observation.rcdmObservation
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninReferenceUtilTest {
    private val subjectOptions = listOf("Location", "Group")
    private val subject = ReferenceGenerator()
    private lateinit var roninObs: RoninObservation
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        val normalizer: Normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        val localizer: Localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        roninObs = RoninObservation(normalizer, localizer)
    }

    @Test
    fun `generate rcdm reference`() {
        val roninRef = rcdmReference("Patient", "1234")
        assertEquals(roninRef.type?.value, "Patient")
        assertEquals(roninRef.reference, "Patient/1234".asFHIR())
        assertEquals(roninRef.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate rcdm reference for profile`() {
        val subjectReference = subject.generate()
        val roninRef = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient", "99")
        assertEquals(roninRef.type?.value, "Patient")
        assertEquals(roninRef.reference, "Patient/test-99".asFHIR())
        assertEquals(roninRef.type?.extension, dataAuthorityExtension)
    }

    @Test
    fun `generate subject when no input id and empty reference provided`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input id and empty reference provided`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient", "")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type and empty reference provided`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, null, "123")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input type and empty reference provided`() {
        val subjectReference = subject.generate()
        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "", "123")
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input id and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input id and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient", "")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, null, "123")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when empty input type and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "", "123")
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `generate subject when no input type or id and unusable reference provided`() {
        val subjectReference = Reference(reference = "Practitioner/123".asFHIR())
        assertEquals("Practitioner/123", subjectReference.reference?.value)
        assertNull(subjectReference.type)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic)
        assertNotEquals("Practitioner/123", roninSubject.reference?.value)
        assertEquals(roninSubject.type?.extension, dataAuthorityExtension)
        assertTrue(roninSubject.type?.value in subjectOptions)
    }

    @Test
    fun `use reference provided when no input id and reference is valid rcdm`() {
        val subjectReference = rcdmReference("Practitioner", "test-123")
        assertEquals(dataAuthorityExtension, subjectReference.type?.extension)
        assertEquals("Practitioner", subjectReference.type?.value)

        val roninSubject = generateReference(subjectReference, subjectOptions, tenant.mnemonic, "Patient")
        assertEquals(subjectReference, roninSubject)
    }

    @Test
    fun `invalid subject input - fails validation`() {
        val roninObservation = rcdmObservation("test") {
            subject of rcdmReference("Device", "456")
        }
        val validation = roninObs.validate(roninObservation, null)
        assertEquals(validation.hasErrors(), true)
        assertEquals("RONIN_INV_REF_TYPE", validation.issues()[0].code)
        assertEquals("The referenced resource type was not one of Patient, Location", validation.issues()[0].description)
        assertEquals(LocationContext(element = "Observation", field = "subject"), validation.issues()[0].location)
    }
}
