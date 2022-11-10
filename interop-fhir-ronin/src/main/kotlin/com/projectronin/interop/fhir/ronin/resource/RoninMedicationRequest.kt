package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and transformer for the Ronin Medication Request profile
 */
object RoninMedicationRequest :
    USCoreBasedProfile<MedicationRequest>(R4MedicationRequestValidator, RoninProfile.MEDICATION_REQUEST.value) {
    override fun validateRonin(element: MedicationRequest, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    private val requiredRequesterError = RequiredFieldError(MedicationRequest::requester)

    override fun validateUSCore(element: MedicationRequest, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.requester, requiredRequesterError, parentContext)
        }
    }

    override fun transformInternal(
        normalized: MedicationRequest,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<MedicationRequest?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )

        return Pair(transformed, Validation())
    }
}
