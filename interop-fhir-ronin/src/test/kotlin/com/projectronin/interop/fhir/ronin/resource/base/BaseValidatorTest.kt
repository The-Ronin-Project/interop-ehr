package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BaseValidatorTest {
    @Test
    fun `uses the supplied parent context`() {
        val locationContext = LocationContext(Bundle::entry)

        val validator = object : BaseValidator<Location>(null) {
            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                assertEquals(locationContext, parentContext)
            }
        }

        val location = mockk<Location>()

        val validation = validator.validate(location, locationContext)
        assertNotNull(validation)

        verify { location wasNot Called }
    }

    @Test
    fun `generates a parent context if one is not provided`() {
        val validator = object : BaseValidator<Location>(null) {
            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                assertEquals(LocationContext(Location::class), parentContext)
            }
        }

        val location = mockk<Location>()

        val validation = validator.validate(location)
        assertNotNull(validation)

        verify { location wasNot Called }
    }

    @Test
    fun `validates against the extended profile when provided`() {
        val validator = object : BaseValidator<Location>(R4LocationValidator) {
            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
            }
        }

        val location = mockk<Location> {
            every { validate(R4LocationValidator, eq(LocationContext(Location::class))) } returns Validation()
        }

        val validation = validator.validate(location)
        assertNotNull(validation)

        verify { location.validate(R4LocationValidator, eq(LocationContext(Location::class))) }
    }
}
