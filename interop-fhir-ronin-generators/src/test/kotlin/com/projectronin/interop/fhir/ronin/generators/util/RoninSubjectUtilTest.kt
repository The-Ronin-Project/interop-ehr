package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ReferenceGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninSubjectUtilTest {
    private val subjectOptions = rcdmReference("Location", "123")
    private val providedSubject = rcdmReference("ProvidedSubject", "123")
    private val subject = ReferenceGenerator()

    @Test
    fun `generate subject when none is provided`() {
        val roninSubject = generateSubject(subject.generate(), subjectOptions)
        assertEquals(roninSubject.type?.value, "Location")
        assertEquals(roninSubject, subjectOptions)
    }

    @Test
    fun `keep provided subject`() {
        val roninSubject = generateSubject(providedSubject, subjectOptions)
        assertEquals(roninSubject.type?.value, "ProvidedSubject")
        assertEquals(roninSubject, providedSubject)
    }
}
