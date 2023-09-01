package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.util.dataAuthorityExtension
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BaseRoninConditionTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val unmappedTenant = mockk<Tenant> {
        every { mnemonic } returns "unmappedTenant"
    }
    private val normalizer = mockk<Normalizer> {
        every { normalize(any(), tenant) } answers { firstArg() }
        every { normalize(any(), unmappedTenant) } answers { firstArg() }
    }
    private val localizer = mockk<Localizer> {
        every { localize(any(), tenant) } answers { firstArg() }
        every { localize(any(), unmappedTenant) } answers { firstArg() }
    }
    private val apnea = "1023001"
    private val diagnosisCode = Code(apnea)
    private val diagnosisCoding =
        Coding(system = CodeSystem.SNOMED_CT.uri, code = diagnosisCode, display = "Apnea".asFHIR())
    private val diagnosisCodingList = listOf(diagnosisCoding)
    private val tenantDiagnosisCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Diagnosis".asFHIR(),
        code = Code("bad-diagnosis")
    )
    private val tenantDiagnosisConcept = CodeableConcept(
        text = "Tenant Diagnosis".asFHIR(),
        coding = listOf(tenantDiagnosisCoding)
    )
    private val tenantDiagnosisSourceExtension = Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            tenantDiagnosisConcept
        )
    )
    private val registry = mockk<NormalizationRegistryClient> {}
    private val roninCondition = RoninConditionEncounterDiagnosis(normalizer, localizer, registry, "unmappedTenant,unmappedTenant2")

    @Test
    fun `validate fails if category is invalid`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)),
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
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("not-a-good-category")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = diagnosisCodingList
            ),
            extension = listOf(tenantDiagnosisSourceExtension),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninCondition.validate(condition, LocationContext(Condition::class)).alertIfErrors()
        }

        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CND_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis " +
                "@ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate fails if no category`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)),
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
            category = emptyList(),
            code = CodeableConcept(
                coding = diagnosisCodingList
            ),
            extension = listOf(tenantDiagnosisSourceExtension),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            roninCondition.validate(condition, LocationContext(Condition::class)).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CND_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis " +
                "@ Condition.category",
            exception.message
        )
    }

    @Test
    fun `validate succeeds when a category list is defined and the supplied category matches`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)),
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
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("any")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = diagnosisCodingList
            ),
            extension = listOf(tenantDiagnosisSourceExtension),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val testRoninCondition = TestRoninCondition(normalizer, localizer, registry)
        testRoninCondition.validate(condition).alertIfErrors()
    }

    @Test
    fun `validate succeeds when qualifying category list is defined as empty - everything validates`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)),
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
            code = CodeableConcept(
                coding = diagnosisCodingList
            ),
            extension = listOf(tenantDiagnosisSourceExtension),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val testRoninCondition = TestRoninConditionOpenCategory(normalizer, localizer, registry)
        testRoninCondition.validate(condition).alertIfErrors()
    }

    @Test
    fun `validate fails when a non-empty category list is defined and the supplied category does not match`() {
        val condition = Condition(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value)),
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
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CONDITION_CATEGORY.uri,
                            code = Code("encounter-diagnosis")
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = diagnosisCodingList
            ),
            extension = listOf(tenantDiagnosisSourceExtension),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )

        val testRoninCondition = TestRoninCondition(normalizer, localizer, registry)
        val exception = assertThrows<IllegalArgumentException> {
            testRoninCondition.validate(condition, LocationContext(Condition::class)).alertIfErrors()
        }
        assertEquals(
            "Encountered validation error(s):\n" +
                "ERROR RONIN_CND_001: Must match this system|code: " +
                "http://terminology.hl7.org/CodeSystem/condition-category|any, " +
                "http://terminology.hl7.org/CodeSystem/condition-category|bae " +
                "@ Condition.category",
            exception.message
        )
    }

    class TestRoninCondition(
        normalizer: Normalizer,
        localizer: Localizer,
        registryClient: NormalizationRegistryClient
    ) :
        RoninConditionEncounterDiagnosis(
            normalizer,
            localizer,
            registryClient,
            ""
        ) {
        override fun qualifyingCategories() = listOf(
            Coding(
                system = CodeSystem.CONDITION_CATEGORY.uri,
                code = Code("any")
            ),
            Coding(
                system = CodeSystem.CONDITION_CATEGORY.uri,
                code = Code("bae")
            )
        )
    }

    class TestRoninConditionOpenCategory(
        normalizer: Normalizer,
        localizer: Localizer,
        registryClient: NormalizationRegistryClient
    ) :
        RoninConditionEncounterDiagnosis(
            normalizer,
            localizer,
            registryClient,
            ""
        ) {
        override fun qualifyingCategories() = emptyList<Coding>()
    }
}
