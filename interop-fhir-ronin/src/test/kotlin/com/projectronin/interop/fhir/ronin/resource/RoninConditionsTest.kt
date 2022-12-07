package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninConditionsTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `always qualifies`() {
        assertTrue(RoninConditions.qualifies(Condition(subject = Reference(display = "reference".asFHIR()))))
    }

    @Test
    fun `can validate encounter diagnosis`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test".asFHIR()),
                Identifier(type = CodeableConcepts.RONIN_FHIR_ID, system = CodeSystem.RONIN_FHIR_ID.uri, value = "12345".asFHIR())
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
            subject = Reference(display = "reference".asFHIR()),
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

        RoninConditions.validate(condition, null).alertIfErrors()
    }

    @Test
    fun `can validate problem and health concern`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "test".asFHIR()),
                Identifier(type = CodeableConcepts.RONIN_FHIR_ID, system = CodeSystem.RONIN_FHIR_ID.uri, value = "12345".asFHIR())
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
            subject = Reference(display = "reference".asFHIR()),
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

        RoninConditions.validate(condition, null).alertIfErrors()
    }

    @Test
    fun `can transform encounter diagnosis`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(value = "id".asFHIR())
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
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            )
        )

        val transformed = RoninConditions.transform(condition, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS.value))),
            transformed.meta
        )
    }

    @Test
    fun `can transform problem and health concern`() {
        val condition = Condition(
            id = Id("12345"),
            identifier = listOf(
                Identifier(value = "id".asFHIR())
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
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://snomed.info/sct"),
                        code = Code("254637007"),
                        display = "Non-small cell lung cancer".asFHIR()
                    )
                )
            ),
            subject = Reference(
                reference = "Patient/roninPatientExample01".asFHIR()
            )
        )

        val transformed = RoninConditions.transform(condition, tenant)
        transformed!!
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CONDITION_PROBLEMS_CONCERNS.value))),
            transformed.meta
        )
    }
}
