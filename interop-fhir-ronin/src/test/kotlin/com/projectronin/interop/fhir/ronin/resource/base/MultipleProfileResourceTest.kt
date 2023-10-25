package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.observation.RoninObservation
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MultipleProfileResourceTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val extension1 = Extension(
        url = Uri("http://example.com/extension"),
        value = DynamicValue(DynamicValueType.STRING, FHIRString("value"))
    )

    private lateinit var normalizer: Normalizer
    private lateinit var localizer: Localizer
    private lateinit var testProfile1: BaseProfile<Location>
    private lateinit var testProfile2: BaseProfile<Location>
    private lateinit var testProfile3: BaseProfile<Location>
    private lateinit var profile: TestMultipleProfileResource

    @BeforeEach
    fun setup() {
        normalizer = mockk {
            every { normalize(any(), tenant) } answers { firstArg() }
        }
        localizer = mockk {
            every { localize(any(), tenant) } answers { firstArg() }
        }
        testProfile1 = mockk()
        testProfile2 = mockk()
        testProfile3 = mockk()
        profile = TestMultipleProfileResource(normalizer, localizer, listOf(testProfile1, testProfile2, testProfile3))
    }

    @Test
    fun `validate with no qualifying profiles`() {
        val location = mockk<Location>()

        every { testProfile1.qualifies(location) } returns false
        every { testProfile2.qualifies(location) } returns false
        every { testProfile3.qualifies(location) } returns false

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(location).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PROFILE_001: No profiles qualified @ Location",
            exception.message
        )
    }

    @Test
    fun `validate with no qualifying profiles and default profile`() {
        val observation = mockk<Observation>(relaxed = true)
        val profile1 = mockk<BaseProfile<Observation>>()
        val profile2 = mockk<BaseProfile<Observation>>()
        val profile3 = mockk<RoninObservation>(relaxed = true)
        every { profile1.qualifies(observation) } returns false
        every { profile2.qualifies(observation) } returns false
        every { profile3.validate(observation, any()) } returns Validation()
        val profiles = TestProfileWithDefault(normalizer, localizer, listOf(profile1, profile2), profile3)

        val validation = profiles.validate(observation)
        assertTrue(validation.hasIssues())
        assertFalse(validation.hasErrors())
        val issueList = validation.issues().map {
            "${it.severity} ${it.code}: ${it.description}"
        }.joinToString()

        assertEquals(
            "WARNING RONIN_PROFILE_003: No profiles qualified, the default profile was used",
            issueList
        )
    }

    @Test
    fun `validate with single qualifying profile`() {
        val location = mockk<Location>()

        every { testProfile1.qualifies(location) } returns true
        every { testProfile1.validate(location, LocationContext(Location::class)) } returns Validation()

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

        val (transformResponse, _) = profile.transform(location, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transform with single qualifying profile`() {
        val location = Location(id = Id("1234"))
        val mappedLocation = Location(id = Id("1234"), extension = listOf(extension1))
        val transformedLocation = Location(id = Id("test-1234"), extension = listOf(extension1))

        every {
            testProfile1.conceptMap(location, any(), tenant)
        } returns Pair(mappedLocation, Validation())
        every {
            testProfile1.transformInternal(
                mappedLocation,
                any(),
                tenant
            )
        } returns Pair(TransformResponse(transformedLocation), Validation())

        every { testProfile1.qualifies(location) } returns true
        every { testProfile1.qualifies(mappedLocation) } returns true
        every { testProfile1.qualifies(transformedLocation) } returns true
        every { testProfile1.validate(transformedLocation, LocationContext(Location::class)) } returns Validation()

        every { testProfile2.qualifies(location) } returns false
        every { testProfile2.qualifies(mappedLocation) } returns false
        every { testProfile2.qualifies(transformedLocation) } returns false

        every { testProfile3.qualifies(location) } returns false
        every { testProfile3.qualifies(mappedLocation) } returns false
        every { testProfile3.qualifies(transformedLocation) } returns false

        val (transformResponse, _) = profile.transform(location, tenant)
        assertEquals(transformedLocation, transformResponse!!.resource)
    }

    @Test
    fun `transform with no qualifying profiles and default profile`() {
        val original = mockk<Observation> {
            every { id } returns Id("1234")
        }
        every { normalizer.normalize(original, tenant) } returns original

        val mappedObservation = mockk<Observation> {
            every { id } returns Id("1234")
            every { extension } returns listOf(extension1)
        }
        val roninObservation = mockk<Observation> {
            every { id } returns Id("test-1234")
            every { extension } returns listOf(extension1)
        }
        every { localizer.localize(roninObservation, tenant) } returns roninObservation

        val profile1 = mockk<BaseProfile<Observation>>()
        val profile2 = mockk<BaseProfile<Observation>>()
        val profile3 = mockk<RoninObservation>()
        val roninObservations = TestProfileWithDefault(normalizer, localizer, listOf(profile1, profile2), profile3)

        every { profile1.qualifies(original) } returns false
        every { profile2.qualifies(original) } returns false
        every { profile3.qualifies(original) } returns true

        every { profile3.conceptMap(original, LocationContext(Observation::class), tenant) } returns Pair(
            mappedObservation,
            Validation()
        )

        every { profile1.qualifies(mappedObservation) } returns false
        every { profile2.qualifies(mappedObservation) } returns false
        every { profile3.qualifies(mappedObservation) } returns true

        every {
            profile3.transformInternal(
                mappedObservation,
                LocationContext(Observation::class),
                tenant
            )
        } returns Pair(
            TransformResponse(roninObservation),
            Validation()
        )
        every { profile1.qualifies(roninObservation) } returns false
        every { profile2.qualifies(roninObservation) } returns false
        every { profile3.qualifies(roninObservation) } returns true
        every { profile3.validate(roninObservation, LocationContext(Observation::class)) } returns Validation()

        val (transformResponse, validation) = roninObservations.transform(original, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(roninObservation, transformed)
    }

    private class TestMultipleProfileResource(
        normalizer: Normalizer,
        localizer: Localizer,
        override val potentialProfiles: List<BaseProfile<Location>>
    ) : MultipleProfileResource<Location>(normalizer, localizer)

    private class TestMultipleProfileResourceObs(
        normalizer: Normalizer,
        localizer: Localizer,
        override val potentialProfiles: List<BaseProfile<Observation>>
    ) : MultipleProfileResource<Observation>(normalizer, localizer)

    private class TestProfileWithDefault(
        normalizer: Normalizer,
        localizer: Localizer,
        override val potentialProfiles: List<BaseProfile<Observation>>,
        override val defaultProfile: BaseProfile<Observation>?
    ) : MultipleProfileResource<Observation>(normalizer, localizer)
}
