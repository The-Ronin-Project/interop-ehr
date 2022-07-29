package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyPractitionerRole](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html) profile.
 */
object OncologyPractitionerRole : BaseRoninProfile<PractitionerRole>(KotlinLogging.logger { }) {
    override fun validate(resource: PractitionerRole) {
        requireTenantIdentifier(resource.identifier)

        require(resource.telecom.all { it.system != null && it.value != null }) {
            "All PractitionerRole telecoms require a value and system"
        }
    }

    override fun transformInternal(original: PractitionerRole, tenant: Tenant): PractitionerRole? {
        val id = original.id
        if (id == null) {
            logger.warn { "Unable to transform PractitionerRole due to missing ID" }
            return null
        }

        val practitionerReference = original.practitioner
        if (practitionerReference == null) {
            logger.warn { "Unable to transform PractitionerRole $id due to missing practitioner reference" }
            return null
        }

        val organizationReference = original.organization

        val telecoms = original.telecom.filter { it.system != null && it.value != null }
        val telecomDifference = original.telecom.size - telecoms.size
        if (telecomDifference > 0) {
            logger.info { "$telecomDifference telecoms removed from PractitionerRole $id due to missing system and/or value" }
        }

        return original.copy(
            id = id.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            period = original.period?.localize(tenant),
            practitioner = practitionerReference.localize(tenant),
            organization = organizationReference?.localize(tenant),
            code = original.code.map { it.localize(tenant) },
            specialty = original.specialty.map { it.localize(tenant) },
            location = original.location.map { it.localize(tenant) },
            healthcareService = original.healthcareService.map { it.localize(tenant) },
            telecom = telecoms.map { it.localize(tenant) },
            availableTime = original.availableTime.map { it.localize(tenant) },
            notAvailable = original.notAvailable.map { it.localize(tenant) },
            endpoint = original.endpoint.map { it.localize(tenant) }
        )
    }
}
