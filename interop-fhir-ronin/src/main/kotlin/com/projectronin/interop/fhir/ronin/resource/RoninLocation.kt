package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin Location profile.
 */
object RoninLocation : BaseRoninProfile<Location>(KotlinLogging.logger { }) {
    override fun validateInternal(resource: Location, validation: Validation) {
        validation.apply {
            requireTenantIdentifier(resource.identifier, this)
        }
    }

    override fun transformInternal(original: Location, tenant: Tenant): Pair<Location, Validation> {
        val validation = validation {
            notNull(original.id) { "no FHIR id" }
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            telecom = original.telecom.map { it.localize(tenant) },
            address = original.address?.localize(tenant),
            managingOrganization = original.managingOrganization?.localize(tenant),
            partOf = original.partOf?.localize(tenant),
            endpoint = original.endpoint.map { it.localize(tenant) },
        )
        return Pair(transformed, validation)
    }
}
