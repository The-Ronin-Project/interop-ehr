package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Location
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.transform.LocationTransformer
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Location as R4Location

/**
 * Implementation of [LocationTransformer] suitable for all R4 FHIR Locations
 */
@Component
class R4LocationTransformer : LocationTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformLocations(
        bundle: Bundle<Location>,
        tenant: Tenant
    ): List<R4Location> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformLocation)
    }

    override fun transformLocation(location: Location, tenant: Tenant): R4Location? {
        require(location.dataSource == DataSource.FHIR_R4) { "Location is not an R4 FHIR resource" }

        val r4Location = location.resource as R4Location

        val id = r4Location.id
        if (id == null) {
            logger.warn { "Unable to transform Location due to missing ID" }
            return null
        }

        return R4Location(
            id = id.localize(tenant),
            meta = r4Location.meta?.localize(tenant),
            implicitRules = r4Location.implicitRules,
            language = r4Location.language,
            text = r4Location.text?.localize(tenant),
            contained = r4Location.contained,
            extension = r4Location.extension.map { it.localize(tenant) },
            modifierExtension = r4Location.modifierExtension.map { it.localize(tenant) },
            identifier = r4Location.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            status = r4Location.status,
            operationalStatus = r4Location.operationalStatus,
            name = r4Location.name,
            alias = r4Location.alias,
            description = r4Location.description,
            mode = r4Location.mode,
            type = r4Location.type,
            telecom = r4Location.telecom.map { it.localize(tenant) },
            address = r4Location.address?.localize(tenant),
            physicalType = r4Location.physicalType,
            position = r4Location.position,
            managingOrganization = r4Location.managingOrganization?.localize(tenant),
            partOf = r4Location.partOf?.localize(tenant),
            hoursOfOperation = r4Location.hoursOfOperation,
            availabilityExceptions = r4Location.availabilityExceptions,
            endpoint = r4Location.endpoint.map { it.localize(tenant) },
        )
    }
}
