package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BaseRoninProfileTest {
    private val validTenantIdentifier =
        Identifier(
            system = RoninCodeSystem.TENANT.uri,
            type = RoninCodeableConcepts.TENANT,
            value = "tenant"
        )
    private val validFhirIdentifier =
        Identifier(
            system = RoninCodeSystem.FHIR_ID.uri,
            type = RoninCodeableConcepts.FHIR_ID,
            value = "fhir"
        )

    private lateinit var location: Location

    @BeforeEach
    fun setup() {
        location = mockk(relaxed = true) {
            every { validate(R4LocationValidator, eq(LocationContext(Location::class))) } returns Validation()
        }
    }

    @Test
    fun `no tenant identifier`() {
        every { location.identifier } returns listOf(validFhirIdentifier)

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `tenant identifier system with wrong type`() {
        every { location.identifier } returns listOf(validFhirIdentifier, validTenantIdentifier.copy(type = null))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_002: Tenant identifier provided without proper CodeableConcept defined @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `tenant identifier with no value`() {
        every { location.identifier } returns listOf(validFhirIdentifier, validTenantIdentifier.copy(value = null))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_003: Tenant identifier value is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `no FHIR identifier`() {
        every { location.identifier } returns listOf(validTenantIdentifier)

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `FHIR identifier system with wrong type`() {
        every { location.identifier } returns listOf(validTenantIdentifier, validFhirIdentifier.copy(type = null))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_002: FHIR identifier provided without proper CodeableConcept defined @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `FHIR identifier with no value`() {
        every { location.identifier } returns listOf(validTenantIdentifier, validFhirIdentifier.copy(value = null))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile().validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_003: FHIR identifier value is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `all ronin identifiers are valid`() {
        every { location.identifier } returns listOf(validTenantIdentifier, validFhirIdentifier)

        TestProfile().validate(location, null).alertIfErrors()
    }

    @Test
    fun `sets profile on meta transform for null meta`() {
        val profile = object : TestProfile() {
            fun transformMeta(meta: Meta?, tenant: Tenant): Meta {
                return meta.transform(tenant)
            }
        }

        val tenant = mockk<Tenant>()

        val transformed = profile.transformMeta(null, tenant)
        assertEquals(listOf(Canonical("profile")), transformed.profile)
    }

    @Test
    fun `sets profile on meta transform for non-null meta`() {
        val profile = object : TestProfile() {
            fun transformMeta(meta: Meta?, tenant: Tenant): Meta {
                return meta.transform(tenant)
            }
        }

        val meta = Meta(id = "123", profile = listOf(Canonical("old-profile")))
        val tenant = mockk<Tenant>()

        val transformed = profile.transformMeta(meta, tenant)
        assertEquals("123", transformed.id)
        assertEquals(listOf(Canonical("profile")), transformed.profile)
    }

    private open class TestProfile : BaseRoninProfile<Location>(R4LocationValidator, "profile") {
        override fun transformInternal(
            original: Location,
            parentContext: LocationContext,
            tenant: Tenant
        ): Pair<Location?, Validation> {
            return Pair(original, Validation())
        }

        override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
            validation.apply {
                requireRoninIdentifiers(element.identifier, parentContext, validation)
            }
        }
    }
}
