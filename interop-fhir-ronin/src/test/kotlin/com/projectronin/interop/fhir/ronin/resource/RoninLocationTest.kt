package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
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
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninLocationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `validate fails if no tenant identifier provided`() {
        val location = Location(
            identifier = listOf(Identifier(value = "id"))
        )
        val exception =
            assertThrows<IllegalArgumentException> {
                RoninLocation.validate(location).alertIfErrors()
            }
        assertEquals("Tenant identifier is required", exception.message)
    }

    @Test
    fun `validate succeeds for valid location`() {
        val location = Location(
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test"
                )
            )
        )

        RoninLocation.validate(location)
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
            listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), allDay = true))
        val position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132))
        val endpoint = listOf(Reference(reference = "Endpoint/4321"))
        val local = """${tenant.mnemonic}-"""
        val location = Location(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/location"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
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
            status = LocationStatus.ACTIVE,
            operationalStatus = operationalStatus,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            mode = LocationMode.INSTANCE,
            type = type,
            telecom = listOf(ContactPoint(value = "123-456-7890", system = ContactPointSystem.PHONE)),
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
        assertEquals(Id("""${local}12345"""), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical("https://www.hl7.org/fhir/location"))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), transformed.text)
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
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertEquals(LocationStatus.ACTIVE, transformed.status)
        assertEquals(operationalStatus, transformed.operationalStatus)
        assertEquals("My Office", transformed.name)
        assertEquals("Guest Room", transformed.alias.first())
        assertEquals("Sun Room", transformed.description)
        assertEquals(LocationMode.INSTANCE, transformed.mode)
        assertEquals(type, transformed.type)
        assertEquals(
            listOf(ContactPoint(value = "123-456-7890", system = ContactPointSystem.PHONE)),
            transformed.telecom
        )
        assertEquals(Address(country = "USA"), transformed.address)
        assertEquals(physicalType, transformed.physicalType)
        assertEquals(position, transformed.position)
        assertEquals(Reference(reference = """Organization/${local}1234"""), transformed.managingOrganization)
        assertEquals(Reference(reference = """Location/${local}1234"""), transformed.partOf)
        assertEquals(hoursOfOperation, transformed.hoursOfOperation)
        assertEquals("Call for details", transformed.availabilityExceptions)
        assertEquals(endpoint.map { it.localize(tenant) }, transformed.endpoint)
    }

    @Test
    fun `transforms location with only required attributes`() {
        val location = Location(
            id = Id("12345"),
        )

        val transformed = RoninLocation.transform(location, tenant)

        transformed!! // Force it to be treated as non-null
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertNull(transformed.meta)
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<ContainedResource>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            transformed.identifier
        )
        assertNull(transformed.name)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertNull(transformed.address)
    }
}
