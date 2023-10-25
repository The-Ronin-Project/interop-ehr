package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Organization profile.
 */
@Component
class RoninOrganization(normalizer: Normalizer, localizer: Localizer) :
    USCoreBasedProfile<Organization>(R4OrganizationValidator, RoninProfile.ORGANIZATION.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredActiveFieldError = RequiredFieldError(Organization::active)
    private val requiredNameFieldError = RequiredFieldError(Organization::name)

    override fun validateRonin(element: Organization, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)
        }
    }

    override fun validateUSCore(element: Organization, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.active, requiredActiveFieldError, parentContext)

            checkNotNull(element.name, requiredNameFieldError, parentContext)

            // address.use and telecom.use not being type 'home' is checked by R4OrganizationValidator
        }
    }

    override fun transformInternal(
        normalized: Organization,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Organization>?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(TransformResponse(transformed), Validation())
    }
}
