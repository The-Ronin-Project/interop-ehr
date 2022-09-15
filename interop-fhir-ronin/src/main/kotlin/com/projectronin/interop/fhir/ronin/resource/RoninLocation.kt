package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.resource.base.USCoreBasedProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.tenant.config.model.Tenant

const val RONIN_LOCATION_PROFILE =
    "http://projectronin.io/fhir/ronin.common-fhir-model.uscore-r4/StructureDefinition/ronin-location"

/**
 * Validator and Transformer for the Ronin Location profile.
 */
object RoninLocation :
    USCoreBasedProfile<Location>(R4LocationValidator, RONIN_LOCATION_PROFILE) {
    override fun validateRonin(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, validation)
        }
    }
    private val requiredNameError = FHIRError(
        "REQ_FIELD",
        ValidationIssueSeverity.WARNING,
        "name is a required element, using \"Unnamed Location\"",
        LocationContext(Location::name)
    )

    override fun validateUSCore(element: Location, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            checkNotNull(element.name, requiredNameError, parentContext)
        }
    }

    override fun transformInternal(
        original: Location,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Location?, Validation> {
        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
            name = ensureName(original),
            telecom = original.telecom.map { it.localize(tenant) },
            address = original.address?.localize(tenant),
            managingOrganization = original.managingOrganization?.localize(tenant),
            partOf = original.partOf?.localize(tenant),
            endpoint = original.endpoint.map { it.localize(tenant) },
        )
        return Pair(transformed, Validation())
    }

    /**
     * Locations within the Epic Organization model are the Locations IDs that Interops uses in tenant configuration.
     * These Locations within the Epic Organization model provide Location.name values so do not need ensureName().
     * This utility is for sparse "contact info" Location objects that some Epic APIs, like PractitionerRole, return.
     * Epic does not give these "contact info" Location objects a Location.name. This utility fills that requirement.
     * @return In order of preference: Location.name or "Unnamed Location"
     */
    private fun ensureName(original: Location, alternate: String = "Unnamed Location"): String? {
        if (original.name.isNullOrEmpty()) {
            return alternate
        }
        return original.name
    }
}
