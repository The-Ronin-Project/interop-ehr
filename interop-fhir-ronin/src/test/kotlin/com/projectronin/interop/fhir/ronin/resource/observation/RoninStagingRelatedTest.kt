package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.resource.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.normalization.ValueSetList
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.util.localizeReferenceTest
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninStagingRelatedTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "reference".asFHIR(), // r4 required?
        reference = "Patient/123".asFHIR()
    )
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }

    private val stagingRelatedCode = Code("some-code")
    private val stagingRelatedCoding = Coding(
        system = Uri("some-system-uri"),
        code = stagingRelatedCode,
        display = "some-display".asFHIR()
    )
    private val stagingRelatedCodingList = listOf(stagingRelatedCoding)
    private val stagingRelatedConcept = CodeableConcept(
        text = "Staging Related".asFHIR(),
        coding = stagingRelatedCodingList
    )
    private val stagingRelatedConceptList = listOf(stagingRelatedConcept)

    private val tenantStagingRelatedCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Staging Related".asFHIR(),
        code = Code("bad-staging-code")
    )
    private val tenantStagingRelatedConcept = CodeableConcept(
        text = "Tenant Staging Related".asFHIR(),
        coding = listOf(tenantStagingRelatedCoding)
    )
    private val mappedTenantStagingRelatedConcept = CodeableConcept(
        text = "Staging Related".asFHIR(),
        coding = stagingRelatedCodingList
    )
    private val componentCode = CodeableConcept(text = "code".asFHIR())
    private val componentCode2 = CodeableConcept(text = "code2".asFHIR())

    private val tenantStagingRelatedSourceExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantStagingRelatedConcept
        )
    )
    private val componentCodeExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            componentCode
        )
    )
    private val componentCode2Extension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            componentCode2
        )
    )
    private val tenantStagingRelatedCompValueExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
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

    // In this registry:
    // Raw tenantStagingRelatedCoding is successfully mapped to stagingRelatedCoding.
    // Raw stagingRelatedCoding is not mapped, so triggers a concept mapping error.
    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_STAGING_RELATED.value)
        } returns ValueSetList(stagingRelatedCodingList, valueSetMetadata)
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                stagingRelatedConcept
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                tenantStagingRelatedConcept
            )
        } returns ConceptMapCodeableConcept(
            stagingRelatedConcept,
            tenantStagingRelatedSourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.code",
                componentCode
            )
        } returns ConceptMapCodeableConcept(
            componentCode,
            componentCodeExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.code",
                componentCode2
            )
        } returns ConceptMapCodeableConcept(
            componentCode2,
            componentCode2Extension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    text = "fake staging code".asFHIR(),
                    coding = listOf()
                )
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.component.value",
                stagingRelatedConcept
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.component.value",
                tenantStagingRelatedConcept
            )
        } returns ConceptMapCodeableConcept(
            stagingRelatedConcept,
            tenantStagingRelatedCompValueExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.value",
                CodeableConcept(
                    text = "fake staging value".asFHIR(),
                    coding = listOf()
                )
            )
        } returns null
    }

    private val roninStagingRelated = RoninStagingRelated(normalizer, localizer, normRegistryClient)

    @Test
    fun `does not qualify when no category`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            category = listOf(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            code = stagingRelatedConcept
        )

        assertFalse(roninStagingRelated.qualifies(observation))
    }

    @Test
    fun `does not qualify when no category coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            category = listOf(CodeableConcept(coding = listOf())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            code = stagingRelatedConcept
        )

        assertFalse(roninStagingRelated.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            code = CodeableConcept(text = "fake staging code".asFHIR())
        )

        assertFalse(roninStagingRelated.qualifies(observation))
    }

    @Test
    fun `validate checks for ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Observation.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Observation.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Observation.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails with empty category and therefore no coding`() {
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
            category = listOf(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_STAGING_OBS_002: Coding is required @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with empty category code`() {
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
            category = listOf(CodeableConcept(coding = listOf())),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_STAGING_OBS_002: Coding is required @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with empty code`() {
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
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: code is a required element @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails when code coding has more than one entry `() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)),
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    stagingRelatedCoding,
                    stagingRelatedCoding
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://something")
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_STAGING_OBS_001: Coding list must contain exactly 1 entry @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails with no code coding`() {
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
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = CodeableConcept(
                text = "fake staging code".asFHIR(),
                coding = listOf()
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Observation.code\n" +
                "ERROR RONIN_OBS_003: Must match this system|code: some-system-uri|some-code @ Observation.code\n" +
                "ERROR RONIN_STAGING_OBS_001: Coding list must contain exactly 1 entry @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `transform fails with no code coding`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
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
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            extension = listOf(tenantStagingRelatedSourceExtension),
            code = CodeableConcept(
                text = "fake staging code".asFHIR(),
                coding = listOf()
            )
        )

        val (transformed, _) = roninStagingRelated.transform(observation, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails with no ID`() {
        val observation = Observation(
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
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
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept
        )

        val (transformed, _) = roninStagingRelated.transform(observation, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform fails with empty code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
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
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            code = null
        )

        val (transformed, _) = roninStagingRelated.transform(observation, tenant)
        assertNull(transformed)
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val observation = Observation(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            contained = listOf(Location(id = Id("67890"))),
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
            identifier = listOf(
                Identifier(value = "id".asFHIR())
            ),
            basedOn = listOf(Reference(reference = "ServiceRequest/1234".asFHIR())),
            partOf = listOf(Reference(reference = "ImagingStudy/1234".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            code = tenantStagingRelatedConcept,
            subject = localizeReferenceTest(mockReference), // check that it transforms
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(reference = "RelatedPerson/1234".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            note = listOf(
                Annotation(
                    text = Markdown("note"),
                    author = DynamicValue(type = DynamicValueType.STRING, value = "THE NOTE")
                )
            ),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(reference = "Specimen/1234".asFHIR()),
            device = Reference(reference = "Device/1234".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(reference = "Observation/2345".asFHIR())),
            derivedFrom = listOf(Reference(reference = "Observation/3456".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = componentCode2,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        tenantStagingRelatedConcept
                    ),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/valueExtension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value")
                        )
                    )
                )
            )
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                ),
                tenantStagingRelatedSourceExtension
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
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(listOf(Reference(reference = "ServiceRequest/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "ImagingStudy/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            stagingRelatedConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantStagingRelatedConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                display = "reference".asFHIR(),
                reference = "Patient/test-123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(listOf(Reference(display = "focus".asFHIR())), transformed.focus)
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(reference = "RelatedPerson/1234".asFHIR())), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            transformed.value
        )
        assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation".asFHIR())), transformed.interpretation)
        assertEquals(CodeableConcept(text = "bodySite".asFHIR()), transformed.bodySite)
        assertEquals(CodeableConcept(text = "method".asFHIR()), transformed.method)
        assertEquals(Reference(reference = "Specimen/1234".asFHIR()), transformed.specimen)
        assertEquals(Reference(reference = "Device/1234".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(reference = "Observation/2345".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(reference = "Observation/3456".asFHIR())), transformed.derivedFrom)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = componentCode2,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/valueExtension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value")
                        ),
                        componentCode2Extension,
                        tenantStagingRelatedCompValueExtension
                    )
                )
            ),
            transformed.component
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("note"),
                    author = DynamicValue(type = DynamicValueType.STRING, value = "THE NOTE")
                )
            ),
            transformed.note
        )
    }

    @Test
    fun `transforms observation with only required attributes - maps Observation code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(source = Uri("source")),
            status = ObservationStatus.AMENDED.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("something-system"),
                            code = Code("could-be-any-code")
                        )
                    )
                )
            ),
            code = tenantStagingRelatedConcept,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR())
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(
            listOf(tenantStagingRelatedSourceExtension),
            transformed.extension
        )
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
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
            transformed.identifier
        )
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(listOf<Reference>(), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("something-system"),
                            code = Code("could-be-any-code")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            mappedTenantStagingRelatedConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(listOf<Reference>(), transformed.focus)
        assertNull(transformed.encounter)
        assertNull(transformed.effective)
        assertNull(transformed.issued)
        assertEquals(listOf<Reference>(), transformed.performer)
        assertNull(transformed.value)
        assertEquals(CodeableConcept(text = "dataAbsent".asFHIR()), transformed.dataAbsentReason)
        assertEquals(listOf<CodeableConcept>(), transformed.interpretation)
        assertNull(transformed.bodySite)
        assertNull(transformed.method)
        assertNull(transformed.specimen)
        assertNull(transformed.device)
        assertEquals(listOf<ObservationReferenceRange>(), transformed.referenceRange)
        assertEquals(listOf<Reference>(), transformed.hasMember)
        assertEquals(listOf<Reference>(), transformed.derivedFrom)
        assertEquals(listOf<ObservationComponent>(), transformed.component)
        assertEquals(listOf<Annotation>(), transformed.note)
    }

    @Test
    fun `transforms observation with only required attributes - fails when no concept map`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(source = Uri("source")),
            status = ObservationStatus.AMENDED.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("something-system"),
                            code = Code("could-be-any-code")
                        )
                    )
                )
            ),
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),

            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR())
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertNull(transformed)
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'some-code' " +
                "has no target defined in any Observation.code concept map for tenant 'test' " +
                "@ Observation.code",
            exception.message
        )
    }

    @Test
    fun `transform inherits R4 validation`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            status = Code("bad-status"),
            code = tenantStagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
            assertNull(transformed)
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad-status' is outside of required value set @ Observation.status",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject not being Patient`() {
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Group/1234".asFHIR(),
                type = Uri("something", extension = dataAuthorityExtension)
            )
        )

        val validation = roninStagingRelated.validate(observation)

        println(validation.issues())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "WARNING INV_REF_TYPE: reference can only be one of the following: Patient @ Observation.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails subject but no type`() {
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR()),

            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ Observation.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails subject and type but no data authority reference extension`() {
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient")),

            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Observation.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val observation = Observation(
            id = Id("123"),
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Observation.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds`() {
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = componentCode2,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    extension = listOf(componentCode2Extension, tenantStagingRelatedCompValueExtension)
                )
            )
        )

        roninStagingRelated.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate - fails if missing required source code extension`() {
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
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong url`() {
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "b".asFHIR(),
                            coding = stagingRelatedCodingList
                        )
                    )
                )
            ),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source code extension has wrong datatype`() {
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODING,
                        stagingRelatedCodingList
                    )
                )
            ),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            exception.message
        )
    }

    @Test
    fun `transform succeeds with Observation Component value mapping success`() {
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
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = tenantStagingRelatedConcept, // in registry map
            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        tenantStagingRelatedConcept // in registry map
                    ),
                    code = componentCode
                )
            )
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        validation.alertIfErrors()
        assertNotNull(transformed)
    }

    @Test
    fun `transform fails with Observation Component value mapping error`() {
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
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = tenantStagingRelatedConcept,
            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    code = componentCode
                )
            )
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        assertNull(transformed)
        val exception = assertThrows<IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'some-code' has no target defined in any Observation.component.value concept map for tenant 'test' @ Observation.component[0].value\n" +
                "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            exception.message
        )
    }

    @Test
    fun `transform fails and concatenates validation issues when both code and Component value have mapping errors`() {
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
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = stagingRelatedConcept,
            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    code = componentCode
                )
            )
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        assertNull(transformed)
        val exception = assertThrows<IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'some-code' has no target defined in any Observation.code concept map for tenant 'test' @ Observation.code\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'some-code' has no target defined in any Observation.component.value concept map for tenant 'test' @ Observation.component[0].value\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension\n" +
                "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if missing required source component value extension`() {
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
            code = stagingRelatedConcept,
            extension = listOf(tenantStagingRelatedSourceExtension),
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR())),
            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    extension = listOf(componentCodeExtension),
                    code = componentCode
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            exception.message
        )
    }

    @Test
    fun `validate - fails if source component value extension has wrong url`() {
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
            extension = listOf(tenantStagingRelatedSourceExtension),
            code = stagingRelatedConcept,
            category = stagingRelatedConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR())),

            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        stagingRelatedConcept
                    ),
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri,
                            value = DynamicValue(
                                DynamicValueType.CODEABLE_CONCEPT,
                                CodeableConcept(
                                    text = "b".asFHIR(),
                                    coding = stagingRelatedCodingList
                                )
                            )
                        ),
                        componentCodeExtension
                    ),
                    code = componentCode
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninStagingRelated.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            exception.message
        )
    }

    @Test
    fun `transform succeeds with Observation Component value type not CodeableConcept`() {
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
                )
            ),
            status = ObservationStatus.AMENDED.asCode(),
            category = stagingRelatedConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            code = tenantStagingRelatedConcept, // in registry map
            component = listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.DATE_TIME,
                        DateTime("2009-01-07")
                    ),
                    code = componentCode
                )
            )
        )

        val (transformed, validation) = roninStagingRelated.transform(observation, tenant)
        assertFalse(validation.hasErrors())
        assertEquals(
            listOf(
                ObservationComponent(
                    value = DynamicValue(
                        DynamicValueType.DATE_TIME,
                        DateTime("2009-01-07")
                    ),
                    code = componentCode,
                    extension = listOf(componentCodeExtension)
                )
            ),
            transformed?.component
        )
    }
}
