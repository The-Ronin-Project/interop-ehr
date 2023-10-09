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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
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
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
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
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninLaboratoryResultTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "subject".asFHIR(), // r4 required?
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

    private val laboratoryCategoryCode = Code("laboratory")
    private val laboratoryCategoryCoding = Coding(
        system = CodeSystem.OBSERVATION_CATEGORY.uri,
        code = laboratoryCategoryCode
    )
    private val laboratoryCategoryCodingList = listOf(laboratoryCategoryCoding)
    private val laboratoryCategoryConcept = CodeableConcept(coding = laboratoryCategoryCodingList)
    private val laboratoryCategoryConceptList = listOf(laboratoryCategoryConcept)

    private val laboratoryCodeCoding = Coding(
        code = Code("some-code"),
        display = "some-display".asFHIR(),
        system = CodeSystem.LOINC.uri
    )
    private val laboratoryCodeCodingList = listOf(laboratoryCodeCoding)
    private val laboratoryCodeConcept = CodeableConcept(
        text = "Laboratory Result".asFHIR(),
        coding = laboratoryCodeCodingList
    )

    private val tenantLaboratoryResultCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Laboratory Result".asFHIR(),
        code = Code("bad-lab-code")
    )
    private val tenantLaboratoryResultConcept = CodeableConcept(
        text = "Tenant Laboratory Result".asFHIR(),
        coding = listOf(tenantLaboratoryResultCoding)
    )
    private val mappedTenantLaboratoryResultConcept = CodeableConcept(
        text = "Laboratory Result".asFHIR(),
        coding = laboratoryCodeCodingList
    )
    private val tenantLaboratoryResultSourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantLaboratoryResultConcept
        )
    )
    private val tenantComponentCode = CodeableConcept(text = "code2".asFHIR())
    private val componentCode = CodeableConcept(text = "Real Code 2".asFHIR())
    private val tenantComponentSourceCodeExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantComponentCode
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
    // Raw tenantLaboratoryResultCoding is successfully mapped to laboratoryCodeCoding.
    // Raw laboratoryCodeCoding is not mapped, so triggers a concept mapping error.
    private val registryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_LABORATORY_RESULT.value)
        } returns ValueSetList(laboratoryCodeCodingList, valueSetMetadata)
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                laboratoryCodeConcept
            )
        } returns null
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                tenantLaboratoryResultConcept
            )
        } returns ConceptMapCodeableConcept(
            laboratoryCodeConcept,
            tenantLaboratoryResultSourceExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.component.code",
                tenantComponentCode
            )
        } returns ConceptMapCodeableConcept(
            componentCode,
            tenantComponentSourceCodeExtension,
            listOf(conceptMapMetadata)
        )
        every {
            getConceptMapping(
                tenant,
                "Observation.code",
                CodeableConcept(
                    text = "laboratory".asFHIR(),
                    coding = listOf()
                )
            )
        } returns null
    }

    private val roninLaboratoryResult = RoninLaboratoryResult(normalizer, localizer, registryClient)

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
            code = laboratoryCodeConcept
        )

        val qualified = roninLaboratoryResult.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when no category coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(CodeableConcept(coding = listOf())),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = laboratoryCodeConcept
        )

        val qualified = roninLaboratoryResult.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code not for laboratory`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("not-laboratory")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = laboratoryCodeConcept
        )

        val qualified = roninLaboratoryResult.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is for laboratory but wrong system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("laboratory")
                        )
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = laboratoryCodeConcept
        )

        val qualified = roninLaboratoryResult.qualifies(observation)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies for profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = laboratoryCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = laboratoryCodeConcept
        )

        val qualified = roninLaboratoryResult.qualifies(observation)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
                source = Uri("source")
            ),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
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
    fun `validate fails with empty category`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|laboratory @ Observation.category\n" +
                "ERROR REQ_FIELD: category is a required element @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if bad laboratory category code`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("not-laboratory")
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|laboratory @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no category system`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = laboratoryCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|laboratory @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if bad category system`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://bad.system"),
                            code = laboratoryCategoryCode
                        )
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_002: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/observation-category|laboratory @ Observation.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no subject`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = null
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ Observation.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if subject is not a Patient`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                reference = "Group/1234".asFHIR(),
                type = Uri("something", extension = dataAuthorityExtension)
            )
        )

        val validation = roninLaboratoryResult.validate(observation)

        println(validation.issues())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "WARNING INV_REF_TYPE: reference can only be one of the following: Patient @ Observation.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if invalid effective type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.BOOLEAN,
                value = true
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_INV_DYN_VAL: Ronin Laboratory Result profile restricts effective to one of: DateTime, Period @ Observation.effective\n" +
                "ERROR INV_DYN_VAL: effective can only be one of the following: DateTime, Period, Timing, Instant @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
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
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        mockkObject(R4ObservationValidator)
        every { R4ObservationValidator.validate(observation, LocationContext(Observation::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Observation::basedOn),
                LocationContext(Observation::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: basedOn is a required element @ Observation.basedOn",
            exception.message
        )

        unmockkObject(R4ObservationValidator)
    }

    @Test
    fun `validate fails if more than 1 entry in coding list for code when Observation type is a laboratory`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    laboratoryCodeCoding,
                    laboratoryCodeCoding,
                    laboratoryCodeCoding
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_LABOBS_002: Coding list must contain exactly 1 entry @ Observation.code",
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("laboratory")
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
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        roninLaboratoryResult.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails with subject but no type`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(display = "subject".asFHIR(), reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient")
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Observation.subject.type.extension",
            exception.message
        )
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
            code = tenantLaboratoryResultConcept,
            category = laboratoryCategoryConceptList,
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
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val (transformResponse, _) = roninLaboratoryResult.transform(observation, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transform fails for observation with no code coding`() {
        val observation = Observation(
            id = Id("12345"),
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
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf()
            ),
            category = laboratoryCategoryConceptList,
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
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val (transformResponse, _) = roninLaboratoryResult.transform(observation, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transform fails for observation with no code`() {
        val observation = Observation(
            id = Id("12345"),
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
            code = null,
            category = laboratoryCategoryConceptList,
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
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val (transformResponse, _) = roninLaboratoryResult.transform(observation, tenant)
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
            basedOn = listOf(Reference(reference = "ServiceRequest/1234".asFHIR())),
            partOf = listOf(Reference(reference = "Immunization/1234".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
            category = laboratoryCategoryConceptList,
            code = tenantLaboratoryResultConcept,
            subject = localizeReferenceTest(mockReference), // check that it transforms
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(reference = "Patient/1234".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(reference = "Specimen/1234".asFHIR()),
            device = Reference(reference = "Device/1234".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(reference = "Observation/2345".asFHIR())),
            derivedFrom = listOf(Reference(reference = "Observation/3456".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = tenantComponentCode,
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        "string"
                    )
                )
            ),
            note = listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.STRING, value = "Dr Adams")
                )
            )
        )

        val (transformResponse, validation) = roninLaboratoryResult.transform(observation, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)), source = Uri("source")),
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
                tenantLaboratoryResultSourceExtension
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
        assertEquals(listOf(Reference(reference = "ServiceRequest/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "Immunization/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            laboratoryCategoryConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantLaboratoryResultConcept,
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
        assertEquals(listOf(Reference(reference = "Patient/1234".asFHIR())), transformed.performer)
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
                    extension = listOf(tenantComponentSourceCodeExtension),
                    code = componentCode,
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        "string"
                    )
                )
            ),
            transformed.component
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.STRING, value = "Dr Adams")
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
            code = tenantLaboratoryResultConcept,
            category = laboratoryCategoryConceptList,
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

        val (transformResponse, validation) = roninLaboratoryResult.transform(observation, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(
            listOf(tenantLaboratoryResultSourceExtension),
            transformed.extension
        )
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)), source = Uri("source")),
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
            laboratoryCategoryConceptList,
            transformed.category
        )
        assertEquals(
            mappedTenantLaboratoryResultConcept,
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
            code = laboratoryCodeConcept,
            category = laboratoryCategoryConceptList,
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

        val (transformResponse, validation) = roninLaboratoryResult.transform(observation, tenant)

        val exception = assertThrows<java.lang.IllegalArgumentException> {
            validation.alertIfErrors()
        }
        assertNull(transformResponse)
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR NOV_CONMAP_LOOKUP: Tenant source value 'some-code' " +
                "has no target defined in any Observation.code concept map for tenant 'test' " +
                "@ Observation.code\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension i" +
                "s missing or invalid @ Observation.extension",
            exception.message
        )
    }

    @Test
    fun `transform inherits R4 validation`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(source = Uri("source")),
            status = Code("bad-status"),
            code = tenantLaboratoryResultConcept,
            category = laboratoryCategoryConceptList,
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
            val (transformResponse, validation) = roninLaboratoryResult.transform(observation, tenant)
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
    fun `validate lab result with missing LOINC system fails`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = Uri("some-system")
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
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
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_003: Must match this system|code: http://loinc.org|some-code @ Observation.code\n" +
                "ERROR RONIN_LABOBS_001: Code system must be LOINC @ Observation.code",
            exception.message
        )
    }

    @Test
    fun `validate lab result with day missing from effective fails`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_004: Datetime must be at least to day @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate lab result with month and day missing from effective fails`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )
        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_004: Datetime must be at least to day @ Observation.effective",
            exception.message
        )
    }

    @Test
    fun `validate fails if value quantity system is not UCUM`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = Uri("some-system"),
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            exception.message
        )
    }

    @Test
    fun `validate fails if value coding system is not SNOMED CT`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(
                tenantLaboratoryResultSourceExtension,
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    system = CodeSystem.RXNORM.uri,
                                    display = "display".asFHIR()
                                )
                            )
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("some-code"),
                            system = CodeSystem.RXNORM.uri,
                            display = "display".asFHIR()
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_LABOBS_003: Value code system must be SNOMED CT @ Observation.value",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if value coding system is SNOMED CT`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(
                tenantLaboratoryResultSourceExtension,
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    code = Code("some-code"),
                                    system = CodeSystem.SNOMED_CT.uri,
                                    display = "display".asFHIR()
                                )
                            )
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            code = Code("some-code"),
                            system = CodeSystem.SNOMED_CT.uri,
                            display = "display".asFHIR()
                        )
                    )
                )
            )
        )

        roninLaboratoryResult.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate fails if there is no child, value, or dataAbsentReason`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR USCORE_LABOBS_003: If there is no component or hasMember element then either a " +
                "value[x] or a data absent reason must be present @ Observation",
            exception.message
        )
    }

    @Test
    fun `validate succeeds if there is no value or dataAbsentReason, there is a hasMember and there are no components`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            hasMember = listOf(Reference(reference = "MolecularSequence/4321".asFHIR()))
        )

        roninLaboratoryResult.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate succeeds if there is no value or dataAbsentReason, there are components and there is no hasMember`() {
        val quantity = Quantity(
            value = Decimal(60.0),
            unit = FHIRString("mL/min/1.73m2"),
            system = Uri("http://unitsofmeasure.org"),
            code = Code("mL/min/{1.73_m2}")
        )
        val component1 = ObservationComponent(
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(coding = listOf(Coding(code = Code("code1"))))
                    )
                )
            ),
            code = CodeableConcept(coding = listOf(Coding(code = Code("code1")))),
            value = DynamicValue(DynamicValueType.QUANTITY, quantity)
        )
        val component2 = ObservationComponent(
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(coding = listOf(Coding(code = Code("code2"))))
                    )
                )
            ),
            code = CodeableConcept(coding = listOf(Coding(code = Code("code2")))),
            value = DynamicValue(DynamicValueType.QUANTITY, quantity)
        )
        val componentList = listOf(component1, component2)

        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(tenantLaboratoryResultSourceExtension),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            component = componentList
        )

        roninLaboratoryResult.validate(observation).alertIfErrors()
    }

    @Test
    fun `validate - fails if missing required source code extension`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "b".asFHIR(),
                            coding = laboratoryCodeCodingList
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
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
                profile = listOf(Canonical(RoninProfile.OBSERVATION_LABORATORY_RESULT.value)),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODING,
                        laboratoryCodeCodingList
                    )
                )
            ),
            code = CodeableConcept(
                text = "laboratory".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("some-code"),
                        display = "some-display".asFHIR(),
                        system = CodeSystem.LOINC.uri
                    )
                )
            ),
            category = laboratoryCategoryConceptList,
            subject = Reference(
                display = "subject".asFHIR(),
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01"
            ),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(68.04),
                    unit = "kg".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("kg")
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninLaboratoryResult.validate(observation).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            exception.message
        )
    }
}
