package com.projectronin.interop.fhir.ronin.error

import com.projectronin.interop.fhir.r4.resource.Observation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvalidReferenceResourceTypeErrorTest {

    @Test
    fun `empty string`() {
        val error = InvalidReferenceResourceTypeError(Observation::subject, emptyList())
        assertEquals("The referenced resource type was not valid", error.description)
    }

    @Test
    fun `single non-empty string`() {
        val error = InvalidReferenceResourceTypeError(Observation::subject, listOf("Apple"))
        assertEquals("The referenced resource type was not Apple", error.description)
    }

    @Test
    fun `multiple non-empty strings`() {
        val error = InvalidReferenceResourceTypeError(Observation::subject, listOf("Apple", "Banana", "Cherry"))
        assertEquals("The referenced resource type was not one of Apple, Banana, Cherry", error.description)
    }
}
