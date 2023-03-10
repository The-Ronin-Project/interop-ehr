package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class RoninConditionProblemsAndHealthConcerns(normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<Condition>(
        R4ConditionValidator,
        RoninProfile.CONDITION_PROBLEMS_CONCERNS.value,
        normalizer,
        localizer
    ) {
    private val qualifyingCodeProblemListItem = Code("problem-list-item")
    private val qualifyingCodeHealthConcerns = Code("health-concern")
    private val qualifyingCodes = setOf(Code("problem-list-item"), Code("health-concern"))

    override fun qualifies(resource: Condition): Boolean {
        return resource.category.any { codeableConcept ->
            codeableConcept.coding.any { coding ->
                (
                    coding.system == CodeSystem.CONDITION_CATEGORY.uri && coding.code == qualifyingCodeProblemListItem ||
                        coding.system == CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri && coding.code == qualifyingCodeHealthConcerns
                    )
            }
        }
    }

    override fun validateRonin(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            // Note: Disabled coding validation due to lack of mappings.
            // requireCodeableConcept("code", element.code, parentContext, this)
        }
    }

    private val requiredCodeError = RequiredFieldError(Condition::code)
    private val noValidCategoryError = FHIRError(
        code = "USCORE_CNDPAHC_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "One of the following condition categories required for US Core Condition Problem and Health Concerns profile: ${
        qualifyingCodes.joinToString(
            ", "
        ) { it.value!! }
        }",
        location = LocationContext(Condition::category)
    )

    override fun validateUSCore(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)

            checkTrue(
                element.category.any { codeableConcept ->
                    codeableConcept.coding.any { coding ->
                        (
                            coding.system == CodeSystem.CONDITION_CATEGORY.uri && coding.code == qualifyingCodeProblemListItem ||
                                coding.system == CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri && coding.code == qualifyingCodeHealthConcerns
                            )
                    }
                },
                noValidCategoryError,
                parentContext
            )
        }
    }

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
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, validation)
    }
}
