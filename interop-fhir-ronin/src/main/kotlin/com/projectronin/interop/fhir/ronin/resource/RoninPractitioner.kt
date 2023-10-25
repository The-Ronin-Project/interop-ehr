package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
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
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Practitioner profile.
 */
@Component
class RoninPractitioner(
    normalizer: Normalizer,
    localizer: Localizer,
    private val roninContactPoint: RoninContactPoint
) :
    USCoreBasedProfile<Practitioner>(R4PractitionerValidator, RoninProfile.PRACTITIONER.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun validateRonin(element: Practitioner, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, this)
            containedResourcePresent(element.contained, parentContext, validation)

            if (element.telecom.isNotEmpty()) {
                roninContactPoint.validateRonin(element.telecom, parentContext, validation)
            }
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

            if (element.telecom.isNotEmpty()) {
                roninContactPoint.validateUSCore(element.telecom, parentContext, validation)
            }
        }
    }

    override fun transformInternal(
        normalized: Practitioner,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Practitioner>?, Validation> {
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
            logger.info { "${normalized.telecom.size - telecoms.size} telecoms removed from Practitioner ${normalized.id?.value} due to failed transformations" }
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant),
            telecom = telecoms
        )
        return Pair(TransformResponse(transformed), validation)
    }
}
