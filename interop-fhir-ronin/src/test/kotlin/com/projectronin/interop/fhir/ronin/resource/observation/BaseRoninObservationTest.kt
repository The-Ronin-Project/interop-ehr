package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BaseRoninObservationTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val bodyHeightCode = Code("8302-2")
    private val bodyHeightCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Body Height".asFHIR(),
        code = bodyHeightCode
    )
    private val bodyHeightCodingList = listOf(bodyHeightCoding)
    private val tenantBodyHeightCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Body Height".asFHIR(),
        code = Code("bad-body-height")
    )
    private val tenantBodyHeightConcept = CodeableConcept(
        text = "Tenant Body Height".asFHIR(),
        coding = listOf(tenantBodyHeightCoding)
    )
    private val tenantBodyHeightSourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantBodyHeightConcept
        )
    )
    private val stagingRelatedCode = Code("some-code")
    private val stagingRelatedCoding = Coding(
        system = Uri("some-system-uri"),
        code = stagingRelatedCode,
        display = "some-display".asFHIR()
    )
    private val stagingRelatedCodingList = listOf(stagingRelatedCoding)
    private val tenantStagingRelatedCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Staging Related".asFHIR(),
        code = Code("bad-body-height")
    )
    private val tenantStagingRelatedConcept = CodeableConcept(
        text = "Tenant Staging Related".asFHIR(),
        coding = listOf(tenantStagingRelatedCoding)
    )
    private val mappedTenantStagingRelatedConcept = CodeableConcept(
        text = "Staging Related".asFHIR(),
        coding = stagingRelatedCodingList
    )
    private val tenantStagingRelatedSourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantStagingRelatedConcept
        )
    )
    private val conceptMapMetadata = ConceptMapMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "test-concept-map",
        conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
        version = "1"
    )
    private val valueSetMetadata = ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "test-value-set",
        valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
        version = "2"
    )

    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_HEIGHT.value)
        } returns ValueSetList(bodyHeightCodingList, valueSetMetadata)
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_STAGING_RELATED.value)
        } returns ValueSetList(stagingRelatedCodingList, valueSetMetadata)
    }
    private val roninVitalSign = RoninBodyHeight(normalizer, localizer, normRegistryClient)

    @Test
    fun `validate fails if category is invalid`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(tenantBodyHeightSourceExtension),
            code = CodeableConcept(
                coding = bodyHeightCodingList
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("not-a-vital-sign")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninVitalSign.validate(observation, LocationContext(Observation::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no category`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(tenantBodyHeightSourceExtension),
            code = CodeableConcept(
                coding = bodyHeightCodingList
            ),
            category = emptyList(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninVitalSign.validate(observation, LocationContext(Observation::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate succeeds when a category list is defined and the supplied category matches`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(tenantBodyHeightSourceExtension),
            code = CodeableConcept(
                coding = bodyHeightCodingList
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("any")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val testRoninVitalSign = TestRoninBodyHeight(normalizer, localizer, normRegistryClient)
        testRoninVitalSign.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate succeeds when qualifying category list is defined as empty - everything validates`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)),
                source = Uri("source")
            ),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system-uri")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("some-system-here"),
                            code = Code("some-code")
                        )
                    )
                )
            ),
            extension = listOf(tenantBodyHeightSourceExtension),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val roninStagingObservation = RoninStagingRelated(normalizer, localizer, normRegistryClient)
        roninStagingObservation.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails when a non-empty category list is defined and the supplied category does not match`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BODY_HEIGHT.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            extension = listOf(tenantBodyHeightSourceExtension),
            code = CodeableConcept(
                coding = bodyHeightCodingList
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("vital-signs")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(182.88),
                    unit = "cm".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("cm")
                )
            )
        )

        val testRoninVitalSign = TestRoninBodyHeight(normalizer, localizer, normRegistryClient)
        val exception = assertThrows<IllegalArgumentException> {
            testRoninVitalSign.validate(observation).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|any, " +
                "http://terminology.hl7.org/CodeSystem/observation-category|bae " +
                "@ Observation.category",
            exception.message
        )
    }

    class TestRoninBodyHeight(
        normalizer: Normalizer,
        localizer: Localizer,
        registryClient: NormalizationRegistryClient
    ) :
        RoninBodyHeight(
            normalizer,
            localizer,
            registryClient
        ) {
        override fun qualifyingCategories() = listOf(
            Coding(
                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                code = Code("any")
            ),
            Coding(
                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                code = Code("bae")
            )
        )
    }
}
