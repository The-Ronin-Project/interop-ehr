package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Organization profile.
 */

object RoninOrganization :
    USCoreBasedProfile<Organization>(R4OrganizationValidator, RoninProfile.ORGANIZATION.value) {

    private val requiredActiveFieldError = RequiredFieldError(Organization::active)
    private val requiredNameFieldError = RequiredFieldError(Organization::name)

    override fun validateRonin(element: Organization, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)
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
        tenant: Tenant
    ): Pair<Organization?, Validation> {

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )

        return Pair(transformed, Validation())
    }
}
