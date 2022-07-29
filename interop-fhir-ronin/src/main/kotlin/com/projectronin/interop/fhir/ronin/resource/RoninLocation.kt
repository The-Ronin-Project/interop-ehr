package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin Location profile.
 */
object RoninLocation : BaseRoninProfile<Location>(KotlinLogging.logger { }) {
    override fun validate(resource: Location) {
        requireTenantIdentifier(resource.identifier)
    }

    override fun transformInternal(original: Location, tenant: Tenant): Location? {
        val id = original.id
        if (id == null) {
            logger.warn { "Unable to transform Location due to missing ID" }
            return null
        }

        return original.copy(
            id = id.localize(tenant),
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
    }
}
