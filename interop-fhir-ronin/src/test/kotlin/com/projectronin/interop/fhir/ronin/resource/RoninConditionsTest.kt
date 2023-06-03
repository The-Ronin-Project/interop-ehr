package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionEncounterDiagnosis
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionProblemsAndHealthConcerns
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
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
    private val normalizer = mockk<Normalizer>()
    private val localizer = mockk<Localizer>()
    private val profile1 = mockk<RoninConditionEncounterDiagnosis>()
    private val profile2 = mockk<RoninConditionProblemsAndHealthConcerns>()
    private val roninConditions = RoninConditions(normalizer, localizer, profile1, profile2)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninConditions.qualifies(
                Condition(subject = Reference(display = "reference".asFHIR()))
            )
        )
    }

    @Test
    fun `can validate against a profile`() {
        val condition = mockk<Condition>()

        every { profile1.qualifies(condition) } returns true
        every { profile1.validate(condition, LocationContext(Condition::class)) } returns Validation()
        every { profile2.qualifies(condition) } returns false

        roninConditions.validate(condition).alertIfErrors()
    }

    @Test
    fun `can transform to profile`() {
        val original = mockk<Condition> {
            every { id } returns Id("1234")
        }
        every { normalizer.normalize(original, tenant) } returns original

        val roninCondition = mockk<Condition> {
            every { id } returns Id("test-1234")
        }
        every { localizer.localize(roninCondition, tenant) } returns roninCondition

        every { profile1.qualifies(original) } returns false
        every { profile2.qualifies(original) } returns true
        every { profile2.transformInternal(original, LocationContext(Condition::class), tenant) } returns Pair(
            roninCondition,
            Validation()
        )
        every { profile1.qualifies(roninCondition) } returns false
        every { profile2.qualifies(roninCondition) } returns true
        every { profile2.validate(roninCondition, LocationContext(Condition::class)) } returns Validation()

        val (transformed, validation) = roninConditions.transform(original, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(roninCondition, transformed)
    }
}
