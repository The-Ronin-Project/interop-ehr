package com.projectronin.interop.fhir.ronin.resource.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant

object RoninVitalSigns :
    USCoreBasedProfile<Observation>(USCoreVitalSignsValidator, RoninProfile.OBSERVATION_VITAL_SIGNS.value) {
    private val specificVitalSignsCodes = listOf(RoninBodyHeight.bodyHeightCode, RoninBodyWeight.bodyWeightCode)

    override fun qualifies(resource: Observation): Boolean {
        val specificVitalSigns =
            resource.code?.coding?.any { it.system == CodeSystem.LOINC.uri && specificVitalSignsCodes.contains(it.code) }
                ?: false
        // If we found a specific vital sign, then we should already be qualifying it against that type, and thus will not fall back to this generic profile.
        if (specificVitalSigns) {
            return false
        }

        return resource.category.any { category ->
            category.coding.any { it.system == CodeSystem.OBSERVATION_CATEGORY.uri && it.code == USCoreVitalSignsValidator.vitalSignsCode }
        }
    }

    override fun validateRonin(element: Observation, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
        }
    }

    override fun validateUSCore(element: Observation, parentContext: LocationContext, validation: Validation) {
        // This is validated by USCoreVitalSigns
    }

    private val requiredIdError = RequiredFieldError(Observation::id)

    override fun transformInternal(
        normalized: Observation,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Observation?, Validation> {
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
