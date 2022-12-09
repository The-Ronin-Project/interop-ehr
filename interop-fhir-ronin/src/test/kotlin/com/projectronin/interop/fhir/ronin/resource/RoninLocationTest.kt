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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.conceptmap.ConceptMapClient
import com.projectronin.interop.fhir.ronin.profile.RoninConceptMap
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninLocationTest {
    private lateinit var conceptMapClient: ConceptMapClient
    private lateinit var roninLocation: RoninLocation

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @BeforeEach
    fun setup() {
        conceptMapClient = mockk()
        roninLocation = RoninLocation.create(conceptMapClient)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(roninLocation.qualifies(Location()))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val location = Location(
            id = Id("12345"),
            name = "Name".asFHIR()
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLocation.validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Location.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails for no name`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLocation.validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: name is a required element @ Location.name",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "My Office".asFHIR()
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
            roninLocation.validate(location, null).alertIfErrors()
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
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "My Office".asFHIR()
        )

        roninLocation.validate(location, null).alertIfErrors()
    }

    @Test
    fun `transform fails for location with no ID`() {
        val location = Location()

        val (transformed, _) = roninLocation.transform(location, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transforms location with all attributes`() {
        conceptMapClient = mockk {
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Location",
                    "Location.telecom.system",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                        code = Code(value = "telephone")
                    ),
                    ContactPointSystem::class
                )
            } returns Pair(systemCoding("phone"), systemExtension("telephone"))
            every {
                getConceptMappingForEnum(
                    tenant,
                    "Location",
                    "Location.telecom.use",
                    Coding(
                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                        code = Code(value = "cell")
                    ),
                    ContactPointUse::class
                )
            } returns Pair(useCoding("mobile"), useExtension("cell"))
        }
        val operationalStatus =
            Coding(code = Code("O"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v2-0116"))
        val type = listOf(
            CodeableConcept(
                text = "Diagnostic".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("DX"),
                        system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                    )
                )
            )
        )
        val physicalType = CodeableConcept(
            text = "Room".asFHIR(),
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
                    allDay = FHIRBoolean.TRUE
                )
            )
        val position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132))
        val endpoint = listOf(Reference(reference = "Endpoint/4321".asFHIR()))
        val location = Location(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/location"))
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
            identifier = listOf(Identifier(value = "id".asFHIR())),
            status = LocationStatus.ACTIVE.asCode(),
            operationalStatus = operationalStatus,
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            mode = LocationMode.INSTANCE.asCode(),
            type = type,
            telecom = listOf(
                ContactPoint(
                    id = "12345".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                        )
                    ),
                    system = Code(value = "telephone"),
                    use = Code(value = "cell"),
                    value = "8675309".asFHIR(),
                    rank = PositiveInt(1),
                    period = Period(
                        start = DateTime("2021-11-18"),
                        end = DateTime("2022-11-17")
                    )
                ),
                ContactPoint(
                    system = Code("telephone"),
                    value = "1112223333".asFHIR()
                )
            ),
            address = Address(country = "USA".asFHIR()),
            physicalType = physicalType,
            position = position,
            managingOrganization = Reference(reference = "Organization/1234".asFHIR()),
            partOf = Reference(reference = "Location/1234".asFHIR()),
            hoursOfOperation = hoursOfOperation,
            availabilityExceptions = "Call for details".asFHIR(),
            endpoint = endpoint
        )
        roninLocation = RoninLocation.create(conceptMapClient)
        val (transformed, validation) = roninLocation.transform(location, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value))),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
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
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(LocationStatus.ACTIVE.asCode(), transformed.status)
        assertEquals(operationalStatus, transformed.operationalStatus)
        assertEquals("My Office".asFHIR(), transformed.name)
        assertEquals("Guest Room".asFHIR(), transformed.alias.first())
        assertEquals("Sun Room".asFHIR(), transformed.description)
        assertEquals(LocationMode.INSTANCE.asCode(), transformed.mode)
        assertEquals(type, transformed.type)
        assertEquals(
            listOf(
                ContactPoint(
                    id = "12345".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                        )
                    ),
                    value = "8675309".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "telephone")
                                    )
                                )
                            )
                        )
                    ),
                    use = Code(
                        value = "mobile",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_USE.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.use"
                                            )
                                        ),
                                        code = Code(value = "cell")
                                    )
                                )
                            )
                        )
                    ),
                    rank = PositiveInt(1),
                    period = Period(start = DateTime(value = "2021-11-18"), end = DateTime(value = "2022-11-17"))
                ),
                ContactPoint(
                    value = "1112223333".asFHIR(),
                    system = Code(
                        value = "phone",
                        extension = listOf(
                            Extension(
                                url = Uri(value = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value),
                                value = DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value = Coding(
                                        system = Uri(
                                            value = RoninConceptMap.CODE_SYSTEMS.toUriString(
                                                tenant,
                                                "ContactPoint.system"
                                            )
                                        ),
                                        code = Code(value = "telephone")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            transformed.telecom
        )
        assertEquals(Address(country = "USA".asFHIR()), transformed.address)
        assertEquals(physicalType, transformed.physicalType)
        assertEquals(position, transformed.position)
        assertEquals(Reference(reference = "Organization/test-1234".asFHIR()), transformed.managingOrganization)
        assertEquals(Reference(reference = "Location/test-1234".asFHIR()), transformed.partOf)
        assertEquals(hoursOfOperation, transformed.hoursOfOperation)
        assertEquals("Call for details".asFHIR(), transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/test-4321".asFHIR())), transformed.endpoint)
    }

    @Test
    fun `transforms location with only required attributes`() {
        val location = Location(
            id = Id("12345"),
            name = "Name".asFHIR()
        )

        val (transformed, validation) = roninLocation.transform(location, tenant)
        validation.alertIfErrors()

        transformed!! // Force it to be treated as non-null
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value))),
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
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.status)
        assertNull(transformed.operationalStatus)
        assertEquals("Name".asFHIR(), transformed.name)
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

    @Test
    fun `transforms location when no name is provided`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            )
        )

        val locationPair = roninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val transformed = locationPair.first
        transformed!!
        assertEquals(
            "Unnamed Location".asFHIR(),
            transformed.name
        )
        roninLocation.validate(transformed, null).alertIfErrors()
    }

    @Test
    fun `transforms location when empty name is provided`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "".asFHIR()
        )

        val locationPair = roninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val transformed = locationPair.first
        transformed!!
        assertEquals(
            "Unnamed Location".asFHIR(),
            transformed.name
        )
        roninLocation.validate(transformed, null).alertIfErrors()
    }

    @Test
    fun `transforms location with name containing id and extensions`() {
        val name = FHIRString(
            value = "Name",
            id = FHIRString("id"),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            )
        )
        val location = Location(
            id = Id("12345"),
            name = name
        )

        val (transformed, validation) = roninLocation.transform(location, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("test-12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value))),
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
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertNull(transformed.status)
        assertNull(transformed.operationalStatus)
        assertEquals(name, transformed.name)
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

    @Test
    fun `transforms location when empty name with id and extensions is provided`() {
        val nameId = FHIRString("id")
        val nameExtensions = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        )
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = FHIRString(value = "", id = nameId, extension = nameExtensions)
        )

        val locationPair = roninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val transformed = locationPair.first
        transformed!!
        assertEquals(
            FHIRString("Unnamed Location", nameId, nameExtensions),
            transformed.name
        )
        roninLocation.validate(transformed, null).alertIfErrors()
    }

    @Test
    fun `transforms location when name has null value with id and extensions is provided`() {
        val nameId = FHIRString("id")
        val nameExtensions = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        )
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = FHIRString(value = null, id = nameId, extension = nameExtensions)
        )

        val locationPair = roninLocation.transformInternal(location, LocationContext(Location::class), tenant)
        val transformed = locationPair.first
        transformed!!
        assertEquals(
            FHIRString("Unnamed Location", nameId, nameExtensions),
            transformed.name
        )
        roninLocation.validate(transformed, null).alertIfErrors()
    }

    @Test
    fun `validate fails for missing telecom system`() {
        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "Location A".asFHIR(),
            telecom = listOf(ContactPoint(value = "1234567890".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLocation.validate(location, LocationContext(Location::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: system is a required element @ Location.telecom[0].system\n" +
                "ERROR R4_CNTCTPT_001: A system is required if a value is provided @ Location.telecom[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails for missing telecom value`() {

        val emailSystemValue = ContactPointSystem.EMAIL.code
        val emailSystemExtensionUri = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri
        val emailSystemExtensionValue = DynamicValue(
            type = DynamicValueType.CODING,
            value = RoninConceptMap.CODE_SYSTEMS.toCoding(tenant, "ContactPoint.system", emailSystemValue)
        )
        val emailSystem = Code(
            value = emailSystemValue,
            extension = listOf(Extension(url = emailSystemExtensionUri, value = emailSystemExtensionValue))
        )

        val location = Location(
            id = Id("12345"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                )
            ),
            name = "Location A".asFHIR(),
            telecom = listOf(ContactPoint(system = emailSystem))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLocation.validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: value is a required element @ Location.telecom[0].value",
            exception.message
        )
    }

    private fun systemCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
        code = Code(value = value)
    )

    private fun systemExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomSystem"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointSystem"),
                code = Code(value = value)
            )
        )
    )

    private fun useCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
        code = Code(value = value)
    )

    private fun useExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceTelecomUse"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/ContactPointUse"),
                code = Code(value = value)
            )
        )
    )
}
