package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.datatype.LocationPosition
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.code.RoninCodeSystem
import com.projectronin.interop.fhir.ronin.code.RoninCodeableConcepts
import com.projectronin.interop.fhir.ronin.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninLocationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(RoninLocation.qualifies(Location()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val location = Location(
            id = Id("12345"),
            name = "Name"
        )

        val exception = assertThrows<IllegalArgumentException> {
            RoninLocation.validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Location.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `no name is provided - Unnamed Location`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            )
        )

        val locationPair = RoninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val roninLocation = locationPair.first
        assertNotNull(roninLocation)
        assertEquals(
            "Unnamed Location",
            roninLocation!!.name
        )
        RoninLocation.validate(roninLocation, null).alertIfErrors()
    }

    @Test
    fun `empty name is provided - Unnamed Location`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            name = ""
        )

        val locationPair = RoninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val roninLocation = locationPair.first
        assertNotNull(roninLocation)
        assertEquals(
            "Unnamed Location",
            roninLocation!!.name
        )
        RoninLocation.validate(roninLocation, null).alertIfErrors()
    }

    @Test
    fun `validate checks R4 profile`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            name = "My Office"
        )

        mockkObject(R4LocationValidator)
        every { R4LocationValidator.validate(location, LocationContext(Location::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Location::mode),
                LocationContext(Location::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            RoninLocation.validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: mode is a required element @ Location.mode",
            exception.message
        )

        unmockkObject(R4LocationValidator)
    }

    @Test
    fun `validate succeeds with name provided`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            name = "My Office"
        )

        RoninLocation.validate(location, null).alertIfErrors()
    }

    @Test
    fun `transform fails for location with no ID`() {
        val location = Location()

        val transformed = RoninLocation.transform(location, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms location with all attributes`() {
        val operationalStatus =
            Coding(code = Code("O"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v2-0116"))
        val type = listOf(
            CodeableConcept(
                text = "Diagnostic",
                coding = listOf(
                    Coding(
                        code = Code("DX"),
                        system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                    )
                )
            )
        )
        val physicalType = CodeableConcept(
            text = "Room",
            coding = listOf(
                Coding(
                    code = Code("ro"),
                    system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                )
            )
        )
        val hoursOfOperation =
            listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.SATURDAY.asCode(), DayOfWeek.SUNDAY.asCode()),
                    allDay = true
                )
            )
        val position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132))
        val endpoint = listOf(Reference(reference = "Endpoint/4321"))
        val location = Location(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/location"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div"),
            contained = listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id")),
            status = LocationStatus.ACTIVE.asCode(),
            operationalStatus = operationalStatus,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            mode = LocationMode.INSTANCE.asCode(),
            type = type,
            telecom = listOf(ContactPoint(value = "123-456-7890", system = ContactPointSystem.PHONE.asCode())),
            address = Address(country = "USA"),
            physicalType = physicalType,
            position = position,
            managingOrganization = Reference(reference = "Organization/1234"),
            partOf = Reference(reference = "Location/1234"),
            hoursOfOperation = hoursOfOperation,
            availabilityExceptions = "Call for details",
            endpoint = endpoint
        )

        val transformed = RoninLocation.transform(location, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RONIN_LOCATION_PROFILE))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div"), transformed.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(LocationStatus.ACTIVE.asCode(), transformed.status)
        assertEquals(operationalStatus, transformed.operationalStatus)
        assertEquals("My Office", transformed.name)
        assertEquals("Guest Room", transformed.alias.first())
        assertEquals("Sun Room", transformed.description)
        assertEquals(LocationMode.INSTANCE.asCode(), transformed.mode)
        assertEquals(type, transformed.type)
        assertEquals(
            listOf(ContactPoint(value = "123-456-7890", system = ContactPointSystem.PHONE.asCode())),
            transformed.telecom
        )
        assertEquals(Address(country = "USA"), transformed.address)
        assertEquals(physicalType, transformed.physicalType)
        assertEquals(position, transformed.position)
        assertEquals(Reference(reference = "Organization/test-1234"), transformed.managingOrganization)
        assertEquals(Reference(reference = "Location/test-1234"), transformed.partOf)
        assertEquals(hoursOfOperation, transformed.hoursOfOperation)
        assertEquals("Call for details", transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/test-4321")), transformed.endpoint)
    }

    @Test
    fun `transforms location with only required attributes`() {
        val location = Location(
            id = Id("12345"),
            name = "Name"
        )

        val transformed = RoninLocation.transform(location, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RONIN_LOCATION_PROFILE))),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = RoninCodeableConcepts.FHIR_ID, system = RoninCodeSystem.FHIR_ID.uri, value = "12345"),
                Identifier(type = RoninCodeableConcepts.TENANT, system = RoninCodeSystem.TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertNull(transformed.status)
        assertNull(transformed.operationalStatus)
        assertEquals("Name", transformed.name)
        assertEquals(listOf<String>(), transformed.alias)
        assertNull(transformed.description)
        assertNull(transformed.mode)
        assertEquals(listOf<CodeableConcept>(), transformed.type)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertNull(transformed.address)
        assertNull(transformed.physicalType)
        assertNull(transformed.position)
        assertNull(transformed.managingOrganization)
        assertNull(transformed.partOf)
        assertEquals(listOf<LocationHoursOfOperation>(), transformed.hoursOfOperation)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
