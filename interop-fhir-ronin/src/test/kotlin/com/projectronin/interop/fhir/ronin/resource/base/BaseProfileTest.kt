package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class BaseProfileTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }

    @Test
    fun `handles a successful transformation with no validation errors for a resource with no ID`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                return Pair(normalized, Validation())
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                // do nothing
            }
        }

        val original = Location()
        val (transformed, _) = profile.transform(original, tenant)
        assertNull(transformed)
    }

    @Test
    fun `handles a null transformation with no validation errors`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                return Pair(null, Validation())
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                fail<Nothing> { "Validate should not be called" }
            }
        }

        val original = Location(id = Id("1234"))
        val (transformed, _) = profile.transform(original, tenant)
        assertNull(transformed)
    }

    @Test
    fun `handles a null transformation with validation errors`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                val validation = validation {
                    checkNotNull(null, RequiredFieldError(Location::id), parentContext)
                }

                return Pair(null, validation)
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                fail<Nothing> { "Validate should not be called" }
            }
        }

        val original = Location(id = Id("1234"))
        val (transformed, _) = profile.transform(original, tenant)
        assertNull(transformed)
    }

    @Test
    fun `handles a successful transformation with no validation errors`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                return Pair(normalized, Validation())
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                // do nothing
            }
        }

        val original = Location(id = Id("1234"))
        val (transformed, _) = profile.transform(original, tenant)
        assertEquals(Id("1234"), transformed!!.id)
    }

    @Test
    fun `handles a successful transformation with validation errors`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                val validation = validation {
                    checkNotNull(null, RequiredFieldError(Location::id), parentContext)
                }

                return Pair(normalized, validation)
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                // do nothing
            }
        }

        val original = Location(id = Id("1234"))
        val (transformed, _) = profile.transform(original, tenant)
        assertNull(transformed)
    }

    @Test
    fun `handles a successful transformation that results in a failed validation`() {
        val profile = object : BaseProfile<Location>(null, normalizer, localizer) {
            override fun transformInternal(
                normalized: Location,
                parentContext: LocationContext,
                tenant: Tenant
            ): Pair<Location?, Validation> {
                return Pair(normalized, Validation())
            }

            override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
                validation.checkNotNull(null, RequiredFieldError(Location::id), parentContext)
            }
        }

        val original = Location(id = Id("1234"))
        val (transformed, _) = profile.transform(original, tenant)
        assertNull(transformed)
    }
}
