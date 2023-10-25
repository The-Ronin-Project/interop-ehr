package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.RCDMVersion
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.getRoninIdentifiersForResource
import com.projectronin.interop.fhir.ronin.hasDataAbsentReason
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.transform.TransformResponse
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Validator and Transformer for the Ronin Location profile.
 */
@Component
class RoninLocation(
    normalizer: Normalizer,
    localizer: Localizer,
    private val contactPoint: RoninContactPoint
) : USCoreBasedProfile<Location>(R4LocationValidator, RoninProfile.LOCATION.value, normalizer, localizer) {
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun validateRonin(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireMeta(element.meta, parentContext, this)
            requireRoninIdentifiers(element.identifier, parentContext, validation)
            containedResourcePresent(element.contained, parentContext, validation)

            if (element.telecom.isNotEmpty()) {
                contactPoint.validateRonin(element.telecom, parentContext, validation)
            }

            // status value set is checked by R4
            // mode value set is checked by R4
        }
    }

    private val requiredNameError = RequiredFieldError(Location::name)
    private val nameInvariantError = FHIRError(
        code = "RONIN_LOC_001",
        severity = ValidationIssueSeverity.ERROR,
        description = "Either Location.name SHALL be present or a Data Absent Reason Extension SHALL be present.",
        location = LocationContext(Location::name)
    )

    override fun validateUSCore(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.name, requiredNameError, parentContext)
            element.name?.let {
                checkTrue(
                    (it.value != null) xor it.hasDataAbsentReason(),
                    nameInvariantError,
                    parentContext
                )
            }

            if (element.telecom.isNotEmpty()) {
                contactPoint.validateUSCore(element.telecom, parentContext, validation)
            }
        }
    }

    private val DEFAULT_NAME = "Unnamed Location"

    private val unnamedWarning = FHIRError(
        "RONIN_LOC_001",
        ValidationIssueSeverity.WARNING,
        "no name was provided, so the default name, $DEFAULT_NAME, has been used instead ",
        LocationContext(Location::name)
    )

    override fun transformInternal(
        normalized: Location,
        parentContext: LocationContext,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): Pair<TransformResponse<Location>?, Validation> {
        val validation = Validation()

        val normalizedName = normalized.name
        val name = if (normalizedName == null) {
            validation.checkTrue(false, unnamedWarning, parentContext)
            FHIRString(DEFAULT_NAME)
        } else if (normalizedName.value.isNullOrEmpty()) {
            validation.checkTrue(false, unnamedWarning, parentContext)
            FHIRString(DEFAULT_NAME, normalizedName.id, normalizedName.extension)
        } else {
            normalized.name
        }

        val contactPointTransformed = if (normalized.telecom.isNotEmpty()) {
            contactPoint.transform(
                normalized.telecom,
                normalized,
                tenant,
                LocationContext(Location::class),
                validation,
                forceCacheReloadTS
            )
        } else {
            Pair(normalized.telecom, validation)
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.getRoninIdentifiersForResource(tenant),
            name = name,
            telecom = contactPointTransformed.first ?: emptyList()
        )
        return Pair(TransformResponse(transformed), contactPointTransformed.second)
    }
}
