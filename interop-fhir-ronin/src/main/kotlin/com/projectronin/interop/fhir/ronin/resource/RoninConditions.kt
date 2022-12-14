package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionEncounterDiagnosis
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionProblemsAndHealthConcerns

/**
 * Validator and Transformer for the group of active Ronin Condition profiles.
 */
object RoninConditions : MultipleProfileResource<Condition>() {
    override val potentialProfiles: List<BaseProfile<Condition>>
        get() = listOf(RoninConditionEncounterDiagnosis, RoninConditionProblemsAndHealthConcerns)
    override val defaultProfile: BaseProfile<Condition>?
        get() = null
}
