package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RoninConditionProblemsAndHealthConcerns(
    normalizer: Normalizer,
    localizer: Localizer,
    registryClient: NormalizationRegistryClient,
    @Value("\${ronin.fhir.conditions.tenantsNotConditionMapped:mdaoc,1xrekpx5}")
    tenantsNotConditionMapped: String
) :
    BaseRoninCondition(
        R4ConditionValidator,
        RoninProfile.CONDITION_PROBLEMS_CONCERNS.value,
        normalizer,
        localizer,
        registryClient,
        tenantsNotConditionMapped
    ) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 3

    private val qualifyingCodeProblemListItem = Code("problem-list-item")
    private val qualifyingCodeHealthConcerns = Code("health-concern")

    override fun qualifyingCategories() = listOf(
        Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = qualifyingCodeProblemListItem),
        Coding(system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri, code = qualifyingCodeHealthConcerns)
    )
}
