package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MultipleProfileResourceTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private lateinit var testProfile1: BaseProfile<Location>
    private lateinit var testProfile2: BaseProfile<Location>
    private lateinit var testProfile3: BaseProfile<Location>
    private lateinit var profile: TestMultipleProfileResource

    @BeforeEach
    fun setup() {
        testProfile1 = mockk()
        testProfile2 = mockk()
        testProfile3 = mockk()
        profile = TestMultipleProfileResource(listOf(testProfile1, testProfile2, testProfile3))
    }

    @Test
    fun `validate with no qualifying profiles`() {
        val location = mockk<Location>()

        every { testProfile1.qualifies(location) } returns false
        every { testProfile2.qualifies(location) } returns false
        every { testProfile3.qualifies(location) } returns false

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            profile.validate(location).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PROFILE_001: No profiles qualified @ Location",
            exception.message
        )
    }

    @Test
    fun `validate with multiple qualifying profiles`() {
        val location = mockk<Location>()

        every { testProfile1.qualifies(location) } returns true
        every { testProfile2.qualifies(location) } returns true
        every { testProfile3.qualifies(location) } returns true

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            profile.validate(location).alertIfErrors()
        }

        assertTrue(exception.message?.startsWith("Encountered validation error(s):\nERROR RONIN_PROFILE_002: Multiple profiles qualified: ") == true)
    }

    @Test
    fun `validate with single qualifying profile`() {
        val location = mockk<Location>()

        every { testProfile1.qualifies(location) } returns true
        every { testProfile1.validate(location, eq(LocationContext(Location::class))) } returns Validation()

        every { testProfile2.qualifies(location) } returns false
        every { testProfile3.qualifies(location) } returns false

        profile.validate(location).alertIfErrors()
    }

    @Test
    fun `transform with no qualifying profiles`() {
        val location = Location(id = Id("1234"))

        every { testProfile1.qualifies(location) } returns false
        every { testProfile2.qualifies(location) } returns false
        every { testProfile3.qualifies(location) } returns false

        val transformed = profile.transform(location, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform with multiple qualifying profiles`() {
        val location = Location(id = Id("1234"))

        every { testProfile1.qualifies(location) } returns true
        every { testProfile2.qualifies(location) } returns true
        every { testProfile3.qualifies(location) } returns true

        val transformed = profile.transform(location, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform with single qualifying profile`() {
        val location = Location(id = Id("1234"))
        val transformedLocation = Location(id = Id("test-1234"))

        every { testProfile1.qualifies(location) } returns true
        every {
            testProfile1.transform(
                location,
                tenant
            )
        } returns location // The "transformation" in this test actually occurs inside the base class.

        every { testProfile1.qualifies(transformedLocation) } returns true
        every { testProfile1.validate(transformedLocation, eq(LocationContext(Location::class))) } returns Validation()

        every { testProfile2.qualifies(location) } returns false
        every { testProfile2.qualifies(transformedLocation) } returns false

        every { testProfile3.qualifies(location) } returns false
        every { testProfile3.qualifies(transformedLocation) } returns false

        val transformed = profile.transform(location, tenant)
        assertEquals(transformedLocation, transformed)
    }

    private class TestMultipleProfileResource(
        override val potentialProfiles: List<BaseProfile<Location>>
    ) : MultipleProfileResource<Location>()
}
