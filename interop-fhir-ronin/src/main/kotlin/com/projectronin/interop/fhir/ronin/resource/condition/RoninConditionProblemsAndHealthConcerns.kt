package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class RoninConditionProblemsAndHealthConcerns(
    normalizer: Normalizer,
    localizer: Localizer
) :
    BaseRoninCondition(
        R4ConditionValidator,
        RoninProfile.CONDITION_PROBLEMS_CONCERNS.value,
        normalizer,
        localizer
    ) {
    private val qualifyingCodeProblemListItem = Code("problem-list-item")
    private val qualifyingCodeHealthConcerns = Code("health-concern")

    // Subclasses may override - either with static values, or by calling getValueSet() on the DataNormalizationRegistry
    override val qualifyingCategories = listOf(
        Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = qualifyingCodeProblemListItem),
        Coding(system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri, code = qualifyingCodeHealthConcerns)
    )

    private val requiredIdError = RequiredFieldError(Condition::id)

    override fun transformInternal(
        normalized: Condition,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Condition?, Validation> {
        val validation = validation {
            checkNotNull(normalized.id, requiredIdError, parentContext)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, validation)
    }
}
