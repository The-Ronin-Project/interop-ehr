package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and transformer for the Ronin Medication profile
 */
object RoninMedication :
    USCoreBasedProfile<Medication>(
        R4MedicationValidator,
        RoninProfile.MEDICATION.value
    ) {
    private val requiredCodeError = RequiredFieldError(Medication::code)

    override fun validateRonin(element: Medication, parentContext: LocationContext, validation: Validation) {
        validation.apply {

            requireRoninIdentifiers(element.identifier, parentContext, this)

            requireCodeableConcept("code", element.code, parentContext, this)
        }
    }

    override fun validateUSCore(element: Medication, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.code, requiredCodeError, parentContext)

            // validation of status value set is inherited from R4

            // validation of ingredient.item is inherited from R4
        }
    }

    override fun transformInternal(
        normalized: Medication,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Medication?, Validation> {
        // TODO: RoninExtension.TENANT_SOURCE_MEDICATION_CODE, check Ronin IG re: extension, concept maps for code

        val tenantSourceCodeExtension = getExtensionOrEmptyList(
            RoninExtension.TENANT_SOURCE_MEDICATION_CODE,
            normalized.code
        )
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            extension = normalized.extension + tenantSourceCodeExtension,
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, Validation())
    }
}
