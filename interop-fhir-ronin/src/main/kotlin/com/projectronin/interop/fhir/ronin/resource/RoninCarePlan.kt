package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the Ronin Care Plan profile.
 */
@Component
class RoninCarePlan(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninProfile<CarePlan>(R4CarePlanValidator, RoninProfile.CARE_PLAN.value, normalizer, localizer) {

    private val requireCategoryError = RequiredFieldError(CarePlan::category)
    override fun validate(element: CarePlan, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            checkTrue(element.category.isNotEmpty(), requireCategoryError, parentContext)

            // subject required is validated in R4
            validateReference(element.subject, listOf("Patient"), LocationContext(CarePlan::subject), validation)

            // status required, and the required value set, is validated in R4
            // intent required, and the required value set, is validated in R4
        }
    }

    override fun transformInternal(
        normalized: CarePlan,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<CarePlan?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )

        return Pair(transformed, Validation())
    }
}
