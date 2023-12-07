package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.base.BaseProfile
import com.projectronin.interop.fhir.ronin.resource.base.MultipleProfileResource
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionEncounterDiagnosis
import com.projectronin.interop.fhir.ronin.resource.condition.RoninConditionProblemsAndHealthConcerns
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the group of active Ronin Condition profiles.
 */
@Component
class RoninConditions(
    normalizer: Normalizer,
    localizer: Localizer,
    roninConditionEncounterDiagnosis: RoninConditionEncounterDiagnosis,
    roninConditionProblemsAndHealthConcerns: RoninConditionProblemsAndHealthConcerns,
) :
    MultipleProfileResource<Condition>(normalizer, localizer) {
    override val potentialProfiles: List<BaseProfile<Condition>> =
        listOf(roninConditionEncounterDiagnosis, roninConditionProblemsAndHealthConcerns)
}
