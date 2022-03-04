package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Location
import com.projectronin.interop.ehr.model.enums.DataSource
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
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.fhir.r4.resource.Location as R4Location

class R4LocationTransformerTest {
    private val transformer = R4LocationTransformer()
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `non R4 location`() {
        val location = mockk<Location> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformLocation(location, tenant)
        }

        assertEquals("Location is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `fails for location with no ID`() {
        val r4Location = R4Location()
        val location = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val oncologyLocation = transformer.transformLocation(location, tenant)
        assertNull(oncologyLocation)
    }

    @Test
    fun `transforms location with all attributes`() {
        val operationalStatus = Coding(code = Code("O"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v2-0116"))
        val type = listOf(CodeableConcept(text = "Diagnostic", coding = listOf(Coding(code = Code("DX"), system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")))))
        val physicalType = CodeableConcept(text = "Room", coding = listOf(Coding(code = Code("ro"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type"))))
        val hoursOfOperation = listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), allDay = true))
        val position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132))
        val endpoint = listOf(Reference(reference = "Endpoint/4321"))
        val local = """${tenant.mnemonic}-"""
        val r4Location = R4Location(
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
            telecom = listOf(ContactPoint(value = "123-456-7890")),
            address = Address(country = "USA"),
            physicalType = physicalType,
            position = position,
            managingOrganization = Reference(reference = "Organization/1234"),
            partOf = Reference(reference = "Location/1234"),
            hoursOfOperation = hoursOfOperation,
            availabilityExceptions = "Call for details",
            endpoint = endpoint
        )
        val location = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val oncologyLocation = transformer.transformLocation(location, tenant)

        oncologyLocation!! // Force it to be treated as non-null
        assertEquals("Location", oncologyLocation.resourceType)
        assertEquals(Id("""${local}12345"""), oncologyLocation.id)
        assertEquals(
            Meta(profile = listOf(Canonical("https://www.hl7.org/fhir/location"))),
            oncologyLocation.meta
        )
        assertEquals(Uri("implicit-rules"), oncologyLocation.implicitRules)
        assertEquals(Code("en-US"), oncologyLocation.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED, div = "div"), oncologyLocation.text)
        assertEquals(
            listOf(ContainedResource("""{"resourceType":"Banana","id":"24680"}""")),
            oncologyLocation.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyLocation.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            oncologyLocation.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id"),
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyLocation.identifier
        )
        assertEquals(LocationStatus.ACTIVE, oncologyLocation.status)
        assertEquals(operationalStatus, oncologyLocation.operationalStatus)
        assertEquals("My Office", oncologyLocation.name)
        assertEquals("Guest Room", oncologyLocation.alias.first())
        assertEquals("Sun Room", oncologyLocation.description)
        assertEquals(LocationMode.INSTANCE, oncologyLocation.mode)
        assertEquals(type, oncologyLocation.type)
        assertEquals(listOf(ContactPoint(value = "123-456-7890")), oncologyLocation.telecom)
        assertEquals(Address(country = "USA"), oncologyLocation.address)
        assertEquals(physicalType, oncologyLocation.physicalType)
        assertEquals(position, oncologyLocation.position)
        assertEquals(Reference(reference = """Organization/${local}1234"""), oncologyLocation.managingOrganization)
        assertEquals(Reference(reference = """Location/${local}1234"""), oncologyLocation.partOf)
        assertEquals(hoursOfOperation, oncologyLocation.hoursOfOperation)
        assertEquals("Call for details", oncologyLocation.availabilityExceptions)
        assertEquals(endpoint.map { it.localize(tenant) }, oncologyLocation.endpoint)
    }

    @Test
    fun `transforms location with only required attributes`() {
        val r4Location = R4Location(
            id = Id("12345"),
        )
        val location = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val oncologyLocation = transformer.transformLocation(location, tenant)

        oncologyLocation!! // Force it to be treated as non-null
        assertEquals("Location", oncologyLocation.resourceType)
        assertEquals(Id("test-12345"), oncologyLocation.id)
        assertNull(oncologyLocation.meta)
        assertNull(oncologyLocation.implicitRules)
        assertNull(oncologyLocation.language)
        assertNull(oncologyLocation.text)
        assertEquals(listOf<ContainedResource>(), oncologyLocation.contained)
        assertEquals(listOf<Extension>(), oncologyLocation.extension)
        assertEquals(listOf<Extension>(), oncologyLocation.modifierExtension)
        assertEquals(
            listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test")
            ),
            oncologyLocation.identifier
        )
        assertNull(oncologyLocation.name)
        assertEquals(listOf<ContactPoint>(), oncologyLocation.telecom)
        assertNull(oncologyLocation.address)
    }

    @Test
    fun `non R4 bundle`() {
        val bundle = mockk<Bundle<Location>> {
            every { dataSource } returns DataSource.EPIC_APPORCHARD
        }

        val exception = assertThrows<IllegalArgumentException> {
            transformer.transformLocations(bundle, tenant)
        }

        assertEquals("Bundle is not an R4 FHIR resource", exception.message)
    }

    @Test
    fun `bundle transformation returns empty when no valid transformations`() {
        val invalidLocation = R4Location()

        val location1 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidLocation
        }
        val location2 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidLocation
        }

        val bundle = mockk<Bundle<Location>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(location1, location2)
        }

        val oncologyLocations = transformer.transformLocations(bundle, tenant)
        assertEquals(0, oncologyLocations.size)
    }

    @Test
    fun `bundle transformation returns only valid transformations`() {
        val invalidLocation = R4Location()
        val location1 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns invalidLocation
        }

        val r4Location = R4Location(
            id = Id("12345"),
        )
        val location2 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val bundle = mockk<Bundle<Location>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(location1, location2)
        }

        val oncologyLocations = transformer.transformLocations(bundle, tenant)
        assertEquals(1, oncologyLocations.size)
    }

    @Test
    fun `bundle transformation returns all when all valid`() {
        val r4Location = R4Location(
            id = Id("12345"),
        )
        val location1 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val location2 = mockk<Location> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resource } returns r4Location
        }

        val bundle = mockk<Bundle<Location>> {
            every { dataSource } returns DataSource.FHIR_R4
            every { resources } returns listOf(location1, location2)
        }

        val oncologyLocations = transformer.transformLocations(bundle, tenant)
        assertEquals(2, oncologyLocations.size)
    }
}
