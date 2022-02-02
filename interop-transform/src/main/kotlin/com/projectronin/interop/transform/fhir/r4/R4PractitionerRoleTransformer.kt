package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitionerRole
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.PractitionerRoleTransformer
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.PractitionerRole as R4PractitionerRole

/**
 * Implementation of [PractitionerRoleTransformer] suitable for all R4 FHIR PractitionerRoles
 */
@Component
class R4PractitionerRoleTransformer : PractitionerRoleTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformPractitionerRoles(
        bundle: Bundle<PractitionerRole>,
        tenant: Tenant
    ): List<OncologyPractitionerRole> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformPractitionerRole)
    }

    override fun transformPractitionerRole(
        practitionerRole: PractitionerRole,
        tenant: Tenant
    ): OncologyPractitionerRole? {
        require(practitionerRole.dataSource == DataSource.FHIR_R4) { "PractitionerRole is not an R4 FHIR resource" }

        val r4PractitionerRole = practitionerRole.resource as R4PractitionerRole

        val id = r4PractitionerRole.id
        if (id == null) {
            logger.warn { "Unable to transform PractitionerRole due to missing ID" }
            return null
        }

        val practitionerReference = r4PractitionerRole.practitioner
        if (practitionerReference == null) {
            logger.warn { "Unable to transform PractitionerRole $id due to missing practitioner reference" }
            return null
        }

        val organizationReference = r4PractitionerRole.organization
        if (organizationReference == null) {
            logger.warn { "Unable to transform PractitionerRole $id due to missing organization reference" }
            return null
        }

        val telecoms = r4PractitionerRole.telecom.filter { it.system != null && it.value != null }
        val telecomDifference = r4PractitionerRole.telecom.size - telecoms.size
        if (telecomDifference > 0) {
            logger.info { "$telecomDifference telecoms removed from PractitionerRole $id due to missing system and/or value" }
        }

        return OncologyPractitionerRole(
            id = id.localize(tenant),
            meta = r4PractitionerRole.meta?.localize(tenant),
            implicitRules = r4PractitionerRole.implicitRules,
            language = r4PractitionerRole.language,
            text = r4PractitionerRole.text?.localize(tenant),
            contained = r4PractitionerRole.contained,
            extension = r4PractitionerRole.extension.map { it.localize(tenant) },
            modifierExtension = r4PractitionerRole.modifierExtension.map { it.localize(tenant) },
            identifier = r4PractitionerRole.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            active = r4PractitionerRole.active,
            period = r4PractitionerRole.period?.localize(tenant),
            practitioner = practitionerReference.localize(tenant),
            organization = organizationReference.localize(tenant),
            code = r4PractitionerRole.code.map { it.localize(tenant) },
            specialty = r4PractitionerRole.specialty.map { it.localize(tenant) },
            location = r4PractitionerRole.location.map { it.localize(tenant) },
            healthcareService = r4PractitionerRole.healthcareService.map { it.localize(tenant) },
            telecom = telecoms.map { it.localize(tenant) },
            availableTime = r4PractitionerRole.availableTime.map { it.localize(tenant) },
            notAvailable = r4PractitionerRole.notAvailable.map { it.localize(tenant) },
            availabilityExceptions = r4PractitionerRole.availabilityExceptions,
            endpoint = r4PractitionerRole.endpoint.map { it.localize(tenant) }
        )
    }
}
