package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
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
 * Validator and Transformer for the Ronin [OncologyPractitionerRole](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html) profile.
 */
@Component
class RoninPractitionerRole(
    normalizer: Normalizer,
    localizer: Localizer,
    private val roninContactPoint: RoninContactPoint
) : USCoreBasedProfile<PractitionerRole>(
    R4PractitionerRoleValidator,
    RoninProfile.PRACTITIONER_ROLE.value,
    normalizer,
    localizer
) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    private val requiredPractitionerError = RequiredFieldError(PractitionerRole::practitioner)

    override fun validateRonin(element: PractitionerRole, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            checkNotNull(element.practitioner, requiredPractitionerError, parentContext)

            if (element.telecom.isNotEmpty()) {
                roninContactPoint.validateRonin(element.telecom, parentContext, this)
            }
        }
    }

    override fun validateUSCore(element: PractitionerRole, parentContext: LocationContext, validation: Validation) {
        if (element.telecom.isNotEmpty()) {
            roninContactPoint.validateUSCore(element.telecom, parentContext, validation)
        }
    }

    override fun transformInternal(
        normalized: PractitionerRole,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<PractitionerRole>?, Validation> {
        val validation = Validation()

        val telecoms =
            roninContactPoint.transform(
                normalized.telecom,
                normalized,
                tenant,
                parentContext,
                validation,
                forceCacheReloadTS
            ).let {
                validation.merge(it.second)
                it.first
            }

        if (telecoms.size != normalized.telecom.size) {
            logger.info { "${normalized.telecom.size - telecoms.size} telecoms removed from PractitionerRole ${normalized.id?.value} due to failed transformations" }
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant),
            telecom = telecoms
        )
        return Pair(TransformResponse(transformed), validation)
    }
}
