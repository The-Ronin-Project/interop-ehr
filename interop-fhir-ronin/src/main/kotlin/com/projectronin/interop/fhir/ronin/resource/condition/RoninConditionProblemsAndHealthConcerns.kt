package com.projectronin.interop.fhir.ronin.resource.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
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

object RoninConditionProblemsAndHealthConcerns :
    USCoreBasedProfile<Condition>(
        R4ConditionValidator,
        RoninProfile.CONDITION_PROBLEMS_CONCERNS.value
    ) {
    private val qualifyingCodes = setOf(Code("problem-list-item"), Code("health-concern"))

    override fun qualifies(resource: Condition): Boolean {
        return resource.category.any { codeableConcept ->
            codeableConcept.coding.any { coding ->
                coding.system == CodeSystem.CONDITION_CATEGORY.uri && qualifyingCodes.contains(coding.code)
            }
        }
    }

    override fun validateRonin(element: Condition, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            requireCodeableConcept("code", element.code, parentContext, this)

            requireCodeCoding("code", element.code?.coding, parentContext, this)
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
                        coding.system == CodeSystem.CONDITION_CATEGORY.uri && qualifyingCodes.contains(coding.code)
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
