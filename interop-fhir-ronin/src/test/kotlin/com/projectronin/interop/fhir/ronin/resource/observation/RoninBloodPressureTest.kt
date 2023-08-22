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

class RoninBloodPressureTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "reference".asFHIR(), // r4 required?
        reference = "Patient/1234".asFHIR()
    )
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val bloodPressureCode = Code("85354-9")
    private val bloodPressureCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Blood Pressure".asFHIR(),
        code = bloodPressureCode
    )
    private val bloodPressureCodingList = listOf(bloodPressureCoding)
    private val bloodPressureConcept = CodeableConcept(
        text = "Blood Pressure".asFHIR(),
        coding = bloodPressureCodingList
    )

    private val tenantBloodPressureCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Bad Blood Pressure".asFHIR(),
        code = Code("bad-blood-pressure")
    )
    private val tenantBloodPressureConcept = CodeableConcept(
        text = "Tenant Blood Pressure".asFHIR(),
        coding = listOf(tenantBloodPressureCoding)
    )
    private val mappedTenantBloodPressureConcept = CodeableConcept(
        text = "Blood Pressure".asFHIR(),
        coding = bloodPressureCodingList
    )
    private val tenantBloodPressureSourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantBloodPressureConcept
        )
    )

    private val systolicCoding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6")))
    private val diastolicCoding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4")))

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
    // Raw tenantBloodPressureCoding is successfully mapped to bloodPressureCoding.
    // Raw bloodPressureCoding is not mapped, so triggers a concept mapping error.
    private val normRegistryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
        } returns ValueSetList(bloodPressureCodingList, valueSetMetadata)
        every {
            getRequiredValueSet("Observation.component:systolic.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
        } returns ValueSetList(systolicCoding, valueSetMetadata)
        every {
            getRequiredValueSet("Observation.component:diastolic.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
        } returns ValueSetList(diastolicCoding, valueSetMetadata)
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                bloodPressureConcept
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                tenantBloodPressureConcept
            )
        } returns ConceptMapCodeableConcept(
            bloodPressureConcept,
            tenantBloodPressureSourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.UCUM.uri,
                            code = bloodPressureCode
                        )
                    )
                )
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
                            code = Code("1234")
                        )
                    )
                )
            )
        } returns null
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
    }
    private val roninBloodPressure = RoninBloodPressure(normalizer, localizer, normRegistryClient)

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
            code = CodeableConcept(text = "vital sign".asFHIR())
        )

        assertFalse(roninBloodPressure.qualifies(observation))
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

        assertFalse(roninBloodPressure.qualifies(observation))
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

        assertFalse(roninBloodPressure.qualifies(observation))
    }

    @Test
    fun `does not qualify when code coding code not for blood pressure`() {
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
                        code = Code("1234")
                    )
                )
            )
        )

        assertFalse(roninBloodPressure.qualifies(observation))
    }

    @Test
    fun `does not qualify when code coding code is for blood pressure but wrong system`() {
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
                        system = CodeSystem.UCUM.uri,
                        code = bloodPressureCode
                    )
                )
            )
        )

        assertFalse(roninBloodPressure.qualifies(observation))
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
            code = bloodPressureConcept
        )

        assertTrue(roninBloodPressure.qualifies(observation))
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
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
    fun `validate fails if non-blood pressure code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        display = "Blood Pressure".asFHIR(),
                        code = Code("random")
                    )
                )
            ),
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_003: Must match this system|code: http://loinc.org|85354-9 @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no components and no data absent reason for blood pressure`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_BPOBS_001: Must match this system|code: http://loinc.org|8480-6 @ Observation.component:systolic.code\n" +
                "ERROR USCORE_BPOBS_002: Must match this system|code: http://loinc.org|8462-4 @ Observation.component:diastolic.code",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if no components and data absent reason is provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        roninBloodPressure.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if systolic quantity and systolic data absent reason are both provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[0]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if systolic quantity has data absent reason instead of value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        roninBloodPressure.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting systolic quantity values`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_BPOBS_004: Only 1 entry is allowed for systolic blood pressure @ Observation.component:systolic.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no systolic quantity value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.component:systolic.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no systolic quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.component:systolic.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if systolic quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.component:systolic.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no systolic quantity code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.component:systolic.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if systolic quantity code is outside the required value set`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.component:systolic.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if diastolic quantity and diastolic data absent reason are both provided`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR R4_OBSCOM_001: dataAbsentReason SHALL only be present if value[x] is not present @ Observation.component[1]",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if diastolic quantity has data absent reason instead of value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        roninBloodPressure.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if components include conflicting diastolic quantity values`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    dataAbsentReason = CodeableConcept(coding = listOf(Coding(code = Code("absent reason"))))
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_BPOBS_005: Only 1 entry is allowed for diastolic blood pressure @ Observation.component:diastolic.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no diastolic quantity value`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.value is a required element @ Observation.component:diastolic.valueQuantity.value",
            exception.message
        )
    }

    @Test
    fun `validate fails if no diastolic quantity unit`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.unit is a required element @ Observation.component:diastolic.valueQuantity.unit",
            exception.message
        )
    }

    @Test
    fun `validate fails if diastolic quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = Uri("some-system"),
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.component:diastolic.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if no diastolic quantity code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: valueQuantity.code is a required element @ Observation.component:diastolic.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if diastolic quantity code is outside the required value set`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("invalid-code")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'invalid-code' is outside of required value set @ Observation.component:diastolic.valueQuantity.code",
            exception.message
        )
    }

    @Test
    fun `validate checks US Core vital signs profile`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
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
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject but no type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ Observation.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails with subject and type but no data authority reference extension`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient")),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        roninBloodPressure.validate(observation).alertIfErrors()
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
            code = tenantBloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
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

        val (transformed, _) = roninBloodPressure.transform(observation, tenant)
        assertNull(transformed)
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
            code = tenantBloodPressureConcept,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
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
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
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

        val (transformed, validation) = roninBloodPressure.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
                tenantBloodPressureSourceExtension
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
        assertEquals(
            listOf(Reference(reference = "MedicationStatement/1234".asFHIR())),
            transformed.partOf
        )
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            vitalSignsCategoryConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantBloodPressureConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
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
        assertEquals(
            listOf(Reference(reference = "Organization/1234".asFHIR())),
            transformed.performer
        )
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
        assertEquals(
            listOf(Reference(reference = "Observation/5678".asFHIR())),
            transformed.hasMember
        )
        assertEquals(
            listOf(Reference(reference = "DocumentReference/1234".asFHIR())),
            transformed.derivedFrom
        )
        assertEquals(
            ObservationComponent(
                code = CodeableConcept(
                    coding = systolicCoding,
                    text = "Systolic".asFHIR()
                ),
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 110.0),
                        unit = "mm[Hg]".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("mm[Hg]")
                    )
                )
            ),
            transformed.component[0]
        )
        assertEquals(
            ObservationComponent(
                code = CodeableConcept(
                    coding = diastolicCoding,
                    text = "Diastolic".asFHIR()
                ),
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 70.0),
                        unit = "mm[Hg]".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("mm[Hg]")
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
            code = tenantBloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = localizeReferenceTest(mockReference), // check transform
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = roninBloodPressure.transform(observation, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
                source = Uri("source")
            ),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(tenantBloodPressureSourceExtension),
            transformed.extension
        )
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
            mappedTenantBloodPressureConcept,
            transformed.code
        )
        assertEquals(
            Reference(
                display = "reference".asFHIR(),
                reference = "Patient/test-1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = localizeReferenceTest(mockReference), // check transform
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val (transformed, validation) = roninBloodPressure.transform(observation, tenant)

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertNull(transformed)
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value '85354-9' " +
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
            code = tenantBloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            )
        )

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            val (transformed, validation) = roninBloodPressure.transform(observation, tenant)
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
    fun `validate fails if invalid basedOn reference resource type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            basedOn = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of " +
                "CarePlan, MedicationRequest @ Observation.basedOn[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid derivedFrom reference resource type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            derivedFrom = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not " +
                "DocumentReference @ Observation.derivedFrom[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid hasMember reference resource type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            hasMember = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of " +
                "MolecularSequence, Observation, QuestionnaireResponse @ Observation.hasMember[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid partOf reference resource type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            partOf = listOf(Reference(reference = "".asFHIR()))
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_REF_TYPE: The referenced resource type was not one of " +
                "MedicationStatement, Procedure @ Observation.partOf[0]",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid systolic code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                code = Code("blah"),
                                display = "Systolic".asFHIR()
                            )
                        ),
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = diastolicCoding,
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_BPOBS_001: Must match this system|code: http://loinc.org|8480-6 @ Observation.component:systolic.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if invalid diastolic code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)),
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
            extension = listOf(tenantBloodPressureSourceExtension),
            code = bloodPressureConcept,
            category = vitalSignsCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            component = listOf(
                ObservationComponent(
                    code = CodeableConcept(
                        coding = systolicCoding,
                        text = "Systolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    code = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                code = Code("blah"),
                                display = "Diastolic".asFHIR()
                            )
                        ),
                        text = "Diastolic".asFHIR()
                    ),
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninBloodPressure.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_BPOBS_002: Must match this system|code: http://loinc.org|8462-4 @ Observation.component:diastolic.code",
            exception.message
        )
    }

    @Test
    fun `does not qualify when no category coding code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = null
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "code".asFHIR())
        )

        assertFalse(roninBloodPressure.qualifies(observation))
    }
}
