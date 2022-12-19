package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.element.RoninContactPoint
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Validator and Transformer for the Ronin Location profile.
 */
@Component
class RoninLocation(
    normalizer: Normalizer,
    localizer: Localizer,
    private val contactPoint: RoninContactPoint
) : USCoreBasedProfile<Location>(R4LocationValidator, RoninProfile.LOCATION.value, normalizer, localizer) {

    override fun validateRonin(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)

            if (element.telecom.isNotEmpty()) {
                contactPoint.validateRonin(element.telecom, parentContext, validation)
            }
        }
    }

    private val requiredNameError = RequiredFieldError(Location::name)

    override fun validateUSCore(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.name, requiredNameError, parentContext)

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
        tenant: Tenant
    ): Pair<Location?, Validation> {
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
            contactPoint.transform(normalized.telecom, tenant, LocationContext(Location::class), validation)
        } else Pair(normalized.telecom, validation)

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            name = name,
            telecom = contactPointTransformed.first ?: emptyList(),
        )
        return Pair(transformed, contactPointTransformed.second)
    }
}
