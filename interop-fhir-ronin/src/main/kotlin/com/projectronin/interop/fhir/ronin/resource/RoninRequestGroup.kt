package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.validate.resource.R4RequestGroupValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.validateReference
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RoninRequestGroup(normalizer: Normalizer, localizer: Localizer) :
    BaseRoninProfile<RequestGroup>(R4RequestGroupValidator, RoninProfile.REQUEST_GROUP.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_22_1
    override val profileVersion = 1

    val requiredSubject = RequiredFieldError(RequestGroup::subject)

    override fun validateRonin(element: RequestGroup, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            checkNotNull(element.subject, requiredSubject, parentContext)
            ifNotNull(element.subject) {
                requireDataAuthorityExtensionIdentifier(
                    element.subject,
                    LocationContext(RequestGroup::subject),
                    validation
                )
            }
            validateReference(
                element.subject,
                listOf("Group", "Patient"),
                LocationContext(RequestGroup::subject),
                validation
            )
        }
        // required value sets for status, intent, and priority (when present) are validated in R4
    }

    override fun transformInternal(
        normalized: RequestGroup,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<RequestGroup?, Validation> {
        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getRoninIdentifiersForResource(tenant)
        )

        return Pair(transformed, Validation())
    }
}
