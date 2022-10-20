package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Location profile.
 */
object RoninLocation :
    USCoreBasedProfile<Location>(R4LocationValidator, RoninProfile.LOCATION.value) {
    override fun validateRonin(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)
        }
    }

    private val requiredNameError = RequiredFieldError(Location::name)

    override fun validateUSCore(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.name, requiredNameError, parentContext)
        }
    }

    private const val DEFAULT_NAME = "Unnamed Location"
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

        val name = if (normalized.name.isNullOrEmpty()) {
            validation.checkTrue(false, unnamedWarning, parentContext)
            DEFAULT_NAME
        } else {
            normalized.name
        }

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            name = name
        )
        return Pair(transformed, validation)
    }
}
