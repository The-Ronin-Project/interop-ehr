package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the Ronin Practitioner profile.
 */
@Component
class RoninPractitioner(normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<Practitioner>(R4PractitionerValidator, RoninProfile.PRACTITIONER.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun validateRonin(element: Practitioner, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM, check Ronin IG and consider requireCodeableConcept()
            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_USE, check Ronin IG and consider requireCodeableConcept()
        }
    }

    private val requiredNameError = RequiredFieldError(Practitioner::name)
    private val requiredNameFamilyError = RequiredFieldError(HumanName::family)

    override fun validateUSCore(element: Practitioner, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkTrue(element.name.isNotEmpty(), requiredNameError, parentContext)

            element.name.forEachIndexed { index, name ->
                val currentContext = parentContext.append(LocationContext("Practitioner", "name[$index]"))
                checkNotNull(name.family, requiredNameFamilyError, currentContext)
            }

            // A practitioner identifier is also required, but Ronin has already checked for identifiers we provide.

            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM, check Ronin IG re: requireCode() for telecom.system
            // TODO: RoninExtension.TENANT_SOURCE_TELECOM_USE, check Ronin IG re: requireCode() for telecom.use
        }
    }

    override fun transformInternal(
        normalized: Practitioner,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Practitioner?, Validation> {
        // TODO: RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM, check Ronin IG re: extension, concept maps for telecom.status
        // TODO: RoninExtension.TENANT_SOURCE_TELECOM_USE, check Ronin IG re: extension, concept maps for telecom.use

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )
        return Pair(transformed, Validation())
    }
}
