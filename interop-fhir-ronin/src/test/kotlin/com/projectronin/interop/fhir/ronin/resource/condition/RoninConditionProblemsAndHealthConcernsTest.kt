package com.projectronin.interop.fhir.ronin.resource.condition

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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.ConditionEvidence
import com.projectronin.interop.fhir.r4.resource.ConditionStage
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.ConceptMapCodeableConcept
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.ronin.util.localizeReferenceTest
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoninConditionProblemsAndHealthConcernsTest {
    // using to double-check transformation for reference
    private val mockReference = Reference(
        display = "Patient".asFHIR(),
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
    private val nausea = "2919008"
    private val problemCode = Code(nausea)
    private val problemCoding = Coding(
        system = CodeSystem.SNOMED_CT.uri,
        code = problemCode,
        display = "Nausea, vomiting and diarrhea".asFHIR()
    )
    private val problemCodingList = listOf(problemCoding)
    private val conditionCodeExtension = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceConditionCode"),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            value = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("2919008"),
                        display = "Nausea, vomiting and diarrhea".asFHIR()
                    )
                )
            )
        )
    )
    private val registry = mockk<NormalizationRegistryClient> {
        every {
            getConceptMapping(tenant, "Condition.code", any<CodeableConcept>(), any<Condition>(), any())
        } returns ConceptMapCodeableConcept(
            CodeableConcept(coding = problemCodingList),
            Extension(
                url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                value =
                DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    value = CodeableConcept(coding = problemCodingList)
                )
            ),
            emptyList()
        )
    }
    private val profile = RoninConditionProblemsAndHealthConcerns(normalizer, localizer, registry, "")

    @Test
    fun `does not qualify when no categories`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR())
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category with no codings`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(text = "category".asFHIR())
            )
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is not for a qualifying value`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("something")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is for qualifying code and wrong system`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.OBSERVATION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is problem-list-item and system is for health-concern`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify when category coding code is health-concern and system is for problem-list-item`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies for problem list item`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertTrue(qualified)
    }

    @Test
    fun `qualifies for health concern`() {
        val condition = Condition(
            id = Id("12345"),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(reference = "Patient/123".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val qualified = profile.qualifies(condition)
        assertTrue(qualified)
    }

    @Test
    fun `validate checks ronin identifiers`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Condition.identifier\n" +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Condition.identifier\n" +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier required @ Condition.identifier",
            exception.message
        )
    }

    @Test
    fun `validate fails without subject reference not having element type`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_REQ_REF_TYPE_001: Attribute Type is required for the reference @ Condition.subject.type",
            exception.message
        )
    }

    @Test
    fun `validate fails without subject reference having data authority extension identifier`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(reference = "Patient/1234".asFHIR(), type = Uri("Patient")),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Condition.subject.type.extension",
            exception.message
        )
    }

    @Test
    fun `validate fails with incorrect category code with correct system, wrong code for problem-list-item`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CND_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item, " +
                "http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern @ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate fails with incorrect code with correct system, wrong code for health-concern`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CND_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item, " +
                "http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern @ Condition.category",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if code coding is empty`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
                )
            ),
            code = CodeableConcept(
                coding = listOf()
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `validate fails if no code`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = null,
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: code is a required element @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails if not a qualifying category and no code coding`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
                )
            ),
            code = CodeableConcept(text = "code".asFHIR()),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code\n" +
                "ERROR USCORE_CNDPAHC_001: One of the following condition categories required for US Core Condition Problem and Health Concerns profile: problem-list-item, health-concern @ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate checks R4 profile`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        mockkObject(R4ConditionValidator)
        every { R4ConditionValidator.validate(condition, LocationContext(Condition::class)) } returns validation {
            checkNotNull(
                null,
                RequiredFieldError(Condition::onset),
                LocationContext(Condition::class)
            )
        }

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: onset is a required element @ Condition.onset",
            exception.message
        )

        unmockkObject(R4ConditionValidator)
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails with missing code coding code`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `validate fails with missing code coding system - no code matches the value set`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        code = problemCode,
                        display = "Nausea, vomiting and diarrhea".asFHIR()
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `validate fails with missing code coding display`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        code = problemCode,
                        system = CodeSystem.SNOMED_CT.uri
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_NOV_CODING_001: Coding list entry missing the required fields @ Condition.code",
            exception.message
        )
    }

    @Test
    fun `validate checks meta`() {
        val condition = Condition(
            id = Id("12345"),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: meta is a required element @ Condition.meta",
            exception.message
        )
    }

    @Test
    fun `validate succeeds with health-concern`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        profile.validate(condition).alertIfErrors()
    }

    @Test
    fun `validate succeeds with problem-list-item`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = localizeReferenceTest(mockReference), // check that this is transformed correctly,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        profile.validate(condition).alertIfErrors()
    }

    @Test
    fun `transform fails for condition with no ID`() {
        val condition = Condition(
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val (transformResponse, _) = profile.transform(condition, tenant)
        assertNull(transformResponse)
    }

    @Test
    @Disabled("Coding Validation is currently disabled due to lack of mapping content.")
    fun `transform fails for condition with no code coding display`() {
        val condition = Condition(
            id = Id("12345"),
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
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.SNOMED_CT.uri,
                        code = problemCode
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Condition", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            )
        )

        val (transformResponse, _) = profile.transform(condition, tenant)
        assertNull(transformResponse)
    }

    @Test
    fun `transforms condition with all attributes`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
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
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed".asFHIR()
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            severity = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe".asFHIR()
                    )
                )
            ),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            bodySite = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)".asFHIR()
                        )
                    )
                )
            ),
            subject = localizeReferenceTest(mockReference), // check that this transforms
            encounter = Reference(
                reference = "Encounter/roninEncounterExample01".asFHIR()
            ),
            onset = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")),
            abatement = DynamicValue(
                DynamicValueType.PERIOD,
                Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))
            ),
            recordedDate = DateTime("2022-01-01"),
            recorder = Reference(
                reference = "Practitioner/roninPractitionerExample01".asFHIR()
            ),
            asserter = Reference(
                reference = "Practitioner/roninPractitionerExample01".asFHIR()
            ),
            stage = listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC".asFHIR()
                            )
                        )
                    )
                )
            ),
            evidence = listOf(
                ConditionEvidence(
                    detail = listOf(
                        Reference(
                            reference = "DiagnosticReport/Test01".asFHIR()
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    author = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR())
                    ),
                    text = Markdown("Test")
                )
            )
        )

        val (transformResponse, validation) = profile.transform(condition, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)), source = Uri("source")),
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
                conditionCodeExtension
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
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            transformed.clinicalStatus
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed".asFHIR()
                    )
                )
            ),
            transformed.verificationStatus
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("371924009"),
                        display = "Moderate to severe".asFHIR()
                    )
                )
            ),
            transformed.severity
        )
        assertEquals(
            CodeableConcept(
                coding = problemCodingList
            ),
            transformed.code
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://snomed.info/sct"),
                            code = Code("39607008"),
                            display = "Lung structure (body structure)".asFHIR()
                        )
                    )
                )
            ),
            transformed.bodySite
        )
        assertEquals(
            Reference(
                display = "Patient".asFHIR(),
                reference = "Patient/test-123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(
            Reference(
                reference = "Encounter/roninEncounterExample01".asFHIR()
            ),
            transformed.encounter
        )
        assertEquals(DynamicValue(DynamicValueType.DATE_TIME, DateTime("2019-04-01")), transformed.onset)
        assertEquals(
            DynamicValue(
                DynamicValueType.PERIOD,
                Period(start = DateTime("2019-04-01"), end = DateTime("2022-04-01"))
            ),
            transformed.abatement
        )
        assertEquals(DateTime("2022-01-01"), transformed.recordedDate)
        assertEquals(
            Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR()),
            transformed.recorder
        )
        assertEquals(
            Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR()),
            transformed.asserter
        )
        assertEquals(
            listOf(
                ConditionStage(
                    summary = CodeableConcept(
                        coding = listOf(
                            Coding(
                                system = Uri("http://cancerstaging.org"),
                                code = Code("3C"),
                                display = "IIIC".asFHIR()
                            )
                        )
                    )
                )
            ),
            transformed.stage
        )
        assertEquals(
            listOf(
                ConditionEvidence(
                    detail = listOf(
                        Reference(
                            reference = "DiagnosticReport/Test01".asFHIR()
                        )
                    )
                )
            ),
            transformed.evidence
        )
        assertEquals(
            listOf(
                Annotation(
                    author = DynamicValue(
                        DynamicValueType.REFERENCE,
                        Reference(reference = "Practitioner/roninPractitionerExample01".asFHIR())
                    ),
                    text = Markdown("Test")
                )
            ),
            transformed.note
        )
    }

    @Test
    fun `transforms condition with only required attributes`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            identifier = listOf(
                Identifier(value = "id".asFHIR())
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = localizeReferenceTest(mockReference)
        )

        val (transformResponse, validation) = profile.transform(condition, tenant)
        validation.alertIfErrors()

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Condition", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf(conditionCodeExtension), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
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
        assertNull(transformed.clinicalStatus)
        assertNull(transformed.verificationStatus)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertNull(transformed.severity)
        assertEquals(
            CodeableConcept(
                coding = problemCodingList
            ),
            transformed.code
        )
        assertEquals(listOf<CodeableConcept>(), transformed.bodySite)
        assertEquals(
            Reference(
                display = "Patient".asFHIR(),
                reference = "Patient/test-123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertNull(transformed.encounter)
        assertNull(transformed.onset)
        assertNull(transformed.abatement)
        assertNull(transformed.recordedDate)
        assertNull(transformed.recorder)
        assertNull(transformed.asserter)
        assertEquals(listOf<ConditionStage>(), transformed.stage)
        assertEquals(listOf<ConditionEvidence>(), transformed.evidence)
        assertEquals(listOf<Annotation>(), transformed.note)
    }

    @Test
    fun `validate succeeds with multiple categories`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("problem-list-item")
                        ),
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        profile.validate(condition).alertIfErrors()
    }

    @Test
    fun `validate fails if no subject`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList,
                text = "code".asFHIR()
            ),
            subject = null,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR REQ_FIELD: subject is a required element @ Condition.subject",
            exception.message
        )
    }

    @Test
    fun `validate fails if subject reference is wrong type`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList,
                text = "code".asFHIR()
            ),
            subject = Reference(
                reference = "Group/12345".asFHIR(),
                type = Uri("Group", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            )
        )

        val validation = profile.validate(condition)

        println(validation.issues())
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ Condition.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if bad clinicalStatus`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList,
                text = "code".asFHIR()
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            ),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("bad"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("confirmed"),
                        display = "Confirmed".asFHIR()
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad' is outside of required value set @ Condition.clinicalStatus",
            exception.message
        )
    }

    @Test
    fun `validate fails if bad verificationStatus`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value)),
                source = Uri("source")
            ),
            extension = listOf(conditionCodeExtension),
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
            code = CodeableConcept(
                coding = problemCodingList,
                text = "code".asFHIR()
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                            code = Code("health-concern")
                        )
                    )
                )
            ),
            clinicalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-clinical"),
                        code = Code("inactive"),
                        display = "Inactive".asFHIR()
                    )
                )
            ),
            verificationStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/condition-ver-status"),
                        code = Code("bad"),
                        display = "Confirmed".asFHIR()
                    )
                )
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            profile.validate(condition).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR INV_VALUE_SET: 'bad' is outside of required value set @ Condition.verificationStatus",
            exception.message
        )
    }
}
