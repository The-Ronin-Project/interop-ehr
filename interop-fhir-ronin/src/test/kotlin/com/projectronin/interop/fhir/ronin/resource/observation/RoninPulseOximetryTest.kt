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
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninPulseOximetryTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "subject".asFHIR(), // r4 required?
        reference = "Patient/123".asFHIR()
    )
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val pulseOximetryCode = Code("59408-5")
    private val pulseOximetryCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Pulse Oximetry".asFHIR(),
        code = pulseOximetryCode
    )
    private val pulseOximetryCodingList = listOf(pulseOximetryCoding)
    private val pulseOximetryConcept = CodeableConcept(
        text = "Pulse Oximetry".asFHIR(),
        coding = pulseOximetryCodingList
    )

    private val tenantPulseOximetryCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Pulse Oximetry".asFHIR(),
        code = Code("bad-body-height")
    )
    private val tenantPulseOximetryConcept = CodeableConcept(
        text = "Tenant Pulse Oximetry".asFHIR(),
        coding = listOf(tenantPulseOximetryCoding)
    )
    private val mappedTenantPulseOximetryConcept = CodeableConcept(
        text = "Pulse Oximetry".asFHIR(),
        coding = pulseOximetryCodingList
    )
    private val tenantPulseOximetrySourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantPulseOximetryConcept
        )
    )

    private val flowRateCoding = listOf(
        Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8"))
    )
    private val flowRateCodeableConcept =
        CodeableConcept(
            coding = flowRateCoding,
            text = "Flow Rate".asFHIR()
        )
    private val flowRateSourceExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            flowRateCodeableConcept
        )
    )
    private val concentrationCoding = listOf(
        Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0"))
    )
    private val concentrationCodeableConcept =
        CodeableConcept(
            coding = concentrationCoding,
            text = "Concentration".asFHIR()
        )
    private val concentrationSourceExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            concentrationCodeableConcept
        )
    )

    private val vitalSignsCategoryCode = Code("vital-signs")
    private val vitalSignsCategoryCoding = Coding(
        system = CodeSystem.OBSERVATION_CATEGORY.uri,
        code = vitalSignsCategoryCode
    )
    private val vitalSignsCategoryCodingList = listOf(vitalSignsCategoryCoding)
    private val vitalSignsCategoryConcept = CodeableConcept(coding = vitalSignsCategoryCodingList)
    private val vitalSignsCategoryConceptList = listOf(vitalSignsCategoryConcept)
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
    // Raw tenantPulseOximetryCoding is successfully mapped to pulseOximetryCoding.
    // Raw pulseOximetryCoding is not mapped, so triggers a concept mapping error.
    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
        } returns ValueSetList(pulseOximetryCodingList, valueSetMetadata)
        every {
            getRequiredValueSet("Observation.component:FlowRate.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
        } returns ValueSetList(flowRateCoding, valueSetMetadata)
        every {
            getRequiredValueSet(
                "Observation.component:Concentration.code",
                RoninProfile.OBSERVATION_PULSE_OXIMETRY.value
            )
        } returns ValueSetList(concentrationCoding, valueSetMetadata)
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                pulseOximetryConcept,
                any<Observation>()
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                tenantPulseOximetryConcept,
                any<Observation>()
            )
        } returns ConceptMapCodeableConcept(
            pulseOximetryConcept,
            tenantPulseOximetrySourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.code",
                flowRateCodeableConcept,
                any<Observation>()
            )
        } returns ConceptMapCodeableConcept(
            flowRateCodeableConcept,
            flowRateSourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.code",
                concentrationCodeableConcept,
                any<Observation>()
            )
        } returns ConceptMapCodeableConcept(
            concentrationCodeableConcept,
            concentrationSourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("faulty"),
                            code = pulseOximetryCode
                        )
                    )
                ),
                any<Observation>()
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            code = Code("8867-4")
                        )
                    )
                ),
                any<Observation>()
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                ),
                any<Observation>()
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            code = Code("13245")
                        )
                    )
                ),
                any<Observation>()
            )
        } returns null
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninPulseOximetry = RoninPulseOximetry(normalizer, localizer, normRegistryClient)

    @Test
    fun `does not qualify when no category`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = pulseOximetryConcept
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = null
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "code".asFHIR())
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when code coding is present, but no entries match pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("8867-4")
                    ),
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = vitalSignsCategoryCode
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when wrong system for code coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("faulty"),
                        code = pulseOximetryCode
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when components are good, with bad code coding code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("13245")
                    )
                )
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `does not qualify when components are good, with bad code coding system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("faulty"),
                        code = pulseOximetryCode
                    )
                )
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        assertFalse(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `qualifies for profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = pulseOximetryConcept
        )

        assertTrue(roninPulseOximetry.qualifies(observation))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
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
    fun `validate fails if non-pulse oximetry code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Pulse Oximetry".asFHIR(),
                        code = Code("random")
                    )
                )
            ),
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_003: Must match this system|code: http://loinc.org|59408-5 @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no components and no data absent reason for pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PXOBS_004: Must match this system|code: http://loinc.org|3151-8 @ Observation.component:FlowRate.code\n" +
                "ERROR RONIN_PXOBS_005: Must match this system|code: http://loinc.org|3150-0 @ Observation.component:Concentration.code",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if no components and data absent reason is provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        roninPulseOximetry.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if flowRate quantity and flowRate data absent reason are both provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[0]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if flowRate quantity has data absent reason instead of value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        roninPulseOximetry.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting flowRate quantity values`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PXOBS_005: Only 1 entry is allowed for pulse oximetry flow rate @ Observation.component:FlowRate.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.component:FlowRate.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.component:FlowRate.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if flowRate quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.component:FlowRate.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no flowRate quantity code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.component:FlowRate.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if flowRate quantity code is outside the required value set`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.component:FlowRate.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity and concentration data absent reason are both provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[1]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if concentration quantity has data absent reason instead of value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        roninPulseOximetry.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting concentration quantity values`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_PXOBS_006: Only 1 entry is allowed for pulse oximetry oxygen concentration @ Observation.component:Concentration.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.component:Concentration.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.component:Concentration.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.component:Concentration.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no concentration quantity code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.component:Concentration.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if concentration quantity code is outside the required value set`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.component:Concentration.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate checks US Core vital signs profile`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("bad-code")
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject and type but no data authority reference extension`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient")
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        roninPulseOximetry.validate(observation).alertIfErrors()
    }

    @Test
    fun `transform fails for observation with no ID`() {
        val observation = Observation(
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
                )
            ),
            code = tenantPulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
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

        val (transformResponse, _) = roninPulseOximetry.transform(observation, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transforms observation with all attributes - maps Observation code`() {
        val observation = Observation(
            id = Id("123"),
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
            identifier = listOf(Identifier(value = "id".asFHIR())),
            basedOn = listOf(Reference(reference = "CarePlan/1234".asFHIR())),
            partOf = listOf(Reference(reference = "MedicationStatement/1234".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
            category = vitalSignsCategoryConceptList,
            code = tenantPulseOximetryConcept,
            subject = localizeReferenceTest(mockReference), // check that it transforms
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(reference = "Organization/1234".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(reference = "Specimen/1234".asFHIR()),
            device = Reference(reference = "DeviceMetric/1234".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(reference = "Observation/5678".asFHIR())),
            derivedFrom = listOf(Reference(reference = "DocumentReference/1234".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
                )
            )
        )

        val (transformResponse, validation) = roninPulseOximetry.transform(observation, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
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
                tenantPulseOximetrySourceExtension
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
        assertEquals(listOf(Reference(reference = "CarePlan/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "MedicationStatement/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            vitalSignsCategoryConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantPulseOximetryConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                reference = "Patient/test-123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                display = "subject".asFHIR()
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
        assertEquals(listOf(Reference(reference = "Organization/1234".asFHIR())), transformed.performer)
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
        assertEquals(Reference(reference = "DeviceMetric/1234".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(reference = "Observation/5678".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(reference = "DocumentReference/1234".asFHIR())), transformed.derivedFrom)
        assertEquals(
            ObservationComponent(
                extension = listOf(flowRateSourceExtension),
                code = flowRateCodeableConcept,
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 110.0),
                        unit = "L/min".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("L/min")
                    )
                )
            ),
            transformed.component[0]
        )
        assertEquals(
            ObservationComponent(
                extension = listOf(concentrationSourceExtension),
                code = concentrationCodeableConcept,
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 70.0),
                        unit = "%".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("%")
                    )
                )
            ),
            transformed.component[1]
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
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
            code = tenantPulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformResponse, validation) = roninPulseOximetry.transform(observation, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf(tenantPulseOximetrySourceExtension), transformed.extension)
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
            vitalSignsCategoryConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantPulseOximetryConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                display = "subject".asFHIR()
            ),
            transformed.subject
        )
        assertEquals(listOf<Reference>(), transformed.focus)
        assertNull(transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
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
    fun `transforms observation with only required attributes - fails if no concept map entry`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(source = Uri("source")),
            status = ObservationStatus.AMENDED.asCode(),
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformResponse, validation) = roninPulseOximetry.transform(observation, tenant)

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertNull(transformResponse)
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '59408-5' " +
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
            status = Code("bad-status"),
            code = tenantPulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            val (transformResponse, validation) = roninPulseOximetry.transform(observation, tenant)
            assertNull(transformResponse)
            validation.alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad-status' is outside of required value set @ Observation.status",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid flow rate`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(
                                DynamicValueType.CODEABLE_CONCEPT,
                                CodeableConcept(
                                    coding = listOf(
                                        Coding(
                                            system = CodeSystem.LOINC.uri,
                                            display = "Flow Rate".asFHIR(),
                                            code = Code("blah")
                                        )
                                    ),
                                    text = "Flow Rate".asFHIR()
                                )
                            )
                        )
                    ),
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Flow Rate".asFHIR(),
                                code = Code("blah")
                            )
                        ),
                        text = "Flow Rate".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PXOBS_004: Must match this system|code: http://loinc.org|3151-8 @ Observation.component:FlowRate.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid concentration`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(
                                DynamicValueType.CODEABLE_CONCEPT,
                                CodeableConcept(
                                    coding = listOf(
                                        Coding(
                                            system = CodeSystem.LOINC.uri,
                                            display = "Concentration".asFHIR(),
                                            code = Code("blah")
                                        )
                                    ),
                                    text = "Concentration".asFHIR()
                                )
                            )
                        )
                    ),
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.LOINC.uri,
                                display = "Concentration".asFHIR(),
                                code = Code("blah")
                            )
                        ),
                        text = "Concentration".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_PXOBS_005: Must match this system|code: http://loinc.org|3150-0 @ Observation.component:Concentration.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    unit = "%".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("%")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(98.5),
                    system = CodeSystem.UCUM.uri,
                    code = Code("%")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(98.5),
                    unit = "%".asFHIR(),
                    system = CodeSystem.LOINC.uri,
                    code = Code("%")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no quantity code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(98.5),
                    unit = "%".asFHIR(),
                    system = CodeSystem.UCUM.uri
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if quantity code is outside the required value set`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
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
            extension = listOf(tenantPulseOximetrySourceExtension),
            code = pulseOximetryConcept,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = vitalSignsCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    extension = listOf(flowRateSourceExtension),
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(concentrationSourceExtension),
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(98.5),
                    unit = "%".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("invalid-code")
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninPulseOximetry.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.valueQuantity.code",
            exception.message
        )
    }
}
