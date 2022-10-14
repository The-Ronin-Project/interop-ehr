package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
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
        original: Location,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Location?, Validation> {
        val validation = Validation()

        val name = if (original.name.isNullOrEmpty()) {
            validation.checkTrue(false, unnamedWarning, parentContext)
            DEFAULT_NAME
        } else {
            original.name
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            name = name,
            telecom = original.telecom.map { it.localize(tenant) },
            address = original.address?.localize(tenant),
            managingOrganization = original.managingOrganization?.localize(tenant),
            partOf = original.partOf?.localize(tenant),
            endpoint = original.endpoint.map { it.localize(tenant) },
        )
        return Pair(transformed, validation)
    }
}
