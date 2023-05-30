package com.projectronin.interop.fhir.ronin.resource.base

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.LocalDateTime

class BaseRoninProfileTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }

    private val validTenantIdentifier =
        Identifier(
            system = CodeSystem.RONIN_TENANT.uri,
            type = CodeableConcepts.RONIN_TENANT,
            value = "tenant".asFHIR()
        )
    private val validFhirIdentifier =
        Identifier(
            system = CodeSystem.RONIN_FHIR_ID.uri,
            type = CodeableConcepts.RONIN_FHIR_ID,
            value = "fhir".asFHIR()
        )
    private val validDataAuthorityIdentifier =
        Identifier(
            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
            value = "EHR Data Authority".asFHIR()
        )

    private lateinit var location: Location

    @BeforeEach
    fun setup() {
        location = mockk(relaxed = true) {
            every { meta } returns mockk {
                every { profile } returns listOf(Canonical("profile"))
                every { source } returns Uri("source")
            }
            every { validate(R4LocationValidator, eq(LocationContext(Location::class))) } returns Validation()
        }
    }

    @Test
    fun `no tenant identifier`() {
        every { location.identifier } returns listOf(validFhirIdentifier, validDataAuthorityIdentifier)

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `tenant identifier system with wrong type`() {
        every { location.identifier } returns listOf(
            validFhirIdentifier,
            validDataAuthorityIdentifier,
            validTenantIdentifier.copy(type = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_002: Tenant identifier provided without proper CodeableConcept defined @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `tenant identifier with no value`() {
        every { location.identifier } returns listOf(
            validFhirIdentifier,
            validDataAuthorityIdentifier,
            validTenantIdentifier.copy(value = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_003: Tenant identifier value is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `no FHIR identifier`() {
        every { location.identifier } returns listOf(validTenantIdentifier, validDataAuthorityIdentifier)

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `FHIR identifier system with wrong type`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validDataAuthorityIdentifier,
            validFhirIdentifier.copy(type = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_002: FHIR identifier provided without proper CodeableConcept defined @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `FHIR identifier with no value`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validDataAuthorityIdentifier,
            validFhirIdentifier.copy(value = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_FHIR_ID_003: FHIR identifier value is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `no Data Authority identifier`() {
        every { location.identifier } returns listOf(validTenantIdentifier, validFhirIdentifier)

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `Data Authority identifier system with wrong type`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier.copy(type = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_ID_002: Data Authority identifier provided without proper CodeableConcept defined @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `Data Authority identifier with no value`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier.copy(value = null)
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_ID_003: Data Authority identifier value is required @ Location.identifier",
            exception.message
        )
    }

    @Test
    fun `all ronin identifiers are valid`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )

        TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
    }

    @Test
    fun `sets profile on meta transform for null meta`() {
        val profile = object : TestProfile(normalizer, localizer) {
            fun transformMeta(meta: Meta?): Meta {
                return meta.transform()
            }
        }

        val transformed = profile.transformMeta(null)
        assertEquals(listOf(Canonical("profile")), transformed.profile)
    }

    @Test
    fun `sets profile on meta transform for non-null meta`() {
        val profile = object : TestProfile(normalizer, localizer) {
            fun transformMeta(meta: Meta?): Meta {
                return meta.transform()
            }
        }

        val meta = Meta(id = "123".asFHIR(), profile = listOf(Canonical("old-profile")))

        val transformed = profile.transformMeta(meta)
        assertEquals("123".asFHIR(), transformed.id)
        assertEquals(listOf(Canonical("profile")), transformed.profile)
    }

    @Test
    fun `getExtensionOrEmptyList - null codeableConcept`() {
        val locationTest = Location(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical("profile")), source = Uri("source")),
            identifier = listOf(validTenantIdentifier, validFhirIdentifier, validDataAuthorityIdentifier)
        )

        val (transformedLocation, _) = TestProfile(normalizer, localizer).transform(locationTest, tenant)

        assertEquals(emptyList<Extension>(), transformedLocation!!.extension)
    }

    @Test
    fun `getExtensionOrEmptyList - not null codeableConcept`() {
        val locationTest = Location(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical("profile")), source = Uri("source")),
            identifier = listOf(validTenantIdentifier, validFhirIdentifier, validDataAuthorityIdentifier),
            physicalType = CodeableConcept(
                text = "b".asFHIR(),
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                )
            )
        )

        val (transformedLocation, _) = TestProfile(normalizer, localizer).transform(locationTest, tenant)

        assertEquals(
            listOf(
                Extension(
                    url = Uri(RoninExtension.RONIN_CONCEPT_MAP_SCHEMA.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "b".asFHIR(),
                            coding = listOf(
                                Coding(
                                    system = CodeSystem.RXNORM.uri,
                                    code = Code("b"),
                                    version = "1.0.0".asFHIR(),
                                    display = "b".asFHIR()
                                )
                            )
                        )
                    )
                )
            ),
            transformedLocation!!.extension
        )
    }

    @Test
    fun `codeable concept contains coding missing display - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `codeable concept contains coding missing code - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    display = "display".asFHIR()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `codeable concept contains coding missing system - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code("code"),
                    display = "display".asFHIR()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `codeable concept contains invalid and valid coding - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    code = Code("code"),
                    display = "display".asFHIR()
                ),
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code2"),
                    display = "display2".asFHIR()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `codeable concept blanks counted as error - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code(" "),
                    display = "display".asFHIR()
                ),
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code2"),
                    display = "".asFHIR()
                ),
                Coding(
                    system = Uri("   "),
                    code = Code("code3"),
                    display = "display 3".asFHIR()
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `codeable concept contains all valid coding - no validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code"),
                    display = "display".asFHIR()
                ),
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code2"),
                    display = "display2".asFHIR()
                )
            )
        )

        TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
    }

    @Test
    fun `codeable concept contains single user selected - no validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code"),
                    display = "display".asFHIR(),
                    userSelected = FHIRBoolean.TRUE
                ),
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code2"),
                    display = "display2".asFHIR()
                )
            )
        )

        TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
    }

    @Test
    fun `codeable concept contains multiple user selected - validation error`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.physicalType } returns CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code"),
                    display = "display".asFHIR(),
                    userSelected = FHIRBoolean.TRUE
                ),
                Coding(
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("code2"),
                    display = "display2".asFHIR(),
                    userSelected = FHIRBoolean.TRUE
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_CODING_SEL_001: More than one coding entry has userSelected true @ Location.physicalType",
            exception.message
        )
    }

    @Test
    fun `contained resource logged`() {
        val outputStream = ByteArrayOutputStream()
        val out = System.out
        System.setOut(PrintStream(outputStream))

        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.contained } returns listOf(ContainedResource("Patient"))

        TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        val output = outputStream.toString()
        assertTrue(output.contains("contained resource found @ Location"))
        System.setOut(out)
    }

    @Test
    fun `contained resource warning`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.contained } returns listOf(ContainedResource("Patient"))
        val warning = TestProfile(normalizer, localizer).validate(location, null)

        assertTrue(warning.issues().isNotEmpty())
        assertEquals(
            warning.issues()[0].toString(),
            "WARNING RONIN_CONTAINED_RESOURCE: There is a Contained Resource present @ Location.contained"
        )
    }

    @Test
    fun `no meta`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.meta } returns null

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Location.meta",
            exception.message
        )
    }

    @Test
    fun `no meta profiles`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.meta } returns Meta(profile = listOf(), source = Uri("source"))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_META_001: No profiles found for expected type `profile` @ Location.meta.profile",
            exception.message
        )
    }

    @Test
    fun `no meta profile matching expected profile`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.meta } returns Meta(profile = listOf(Canonical("other-profile")), source = Uri("source"))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_META_001: No profiles found for expected type `profile` @ Location.meta.profile",
            exception.message
        )
    }

    @Test
    fun `no meta source`() {
        every { location.identifier } returns listOf(
            validTenantIdentifier,
            validFhirIdentifier,
            validDataAuthorityIdentifier
        )
        every { location.meta } returns Meta(profile = listOf(Canonical("profile")))

        val exception = assertThrows<IllegalArgumentException> {
            TestProfile(normalizer, localizer).validate(location, null).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: source is a required element @ Location.meta.source",
            exception.message
        )
    }

    private open class TestProfile(normalizer: Normalizer, localizer: Localizer) :
        BaseRoninProfile<Location>(R4LocationValidator, "profile", normalizer, localizer) {
        override val rcdmVersion: RCDMVersion = RCDMVersion.values().last()
        override val profileVersion: Int = 1

        override fun transformInternal(
            normalized: Location,
            parentContext: LocationContext,
            tenant: Tenant,
            forceCacheReloadTS: LocalDateTime?
        ): Pair<Location?, Validation> {
            val tenantSourceCodeExtension = getExtensionOrEmptyList(
                RoninExtension.RONIN_CONCEPT_MAP_SCHEMA,
                normalized.physicalType
            )
            return Pair(normalized.copy(extension = tenantSourceCodeExtension), Validation())
        }

        override fun validate(element: Location, parentContext: LocationContext, validation: Validation) {
            validation.apply {
                requireMeta(element.meta, parentContext, validation)
                requireRoninIdentifiers(element.identifier, parentContext, validation)
                requireCodeableConcept("physicalType", element.physicalType, parentContext, validation)
                containedResourcePresent(element.contained, parentContext, validation)
            }
        }
    }
}
