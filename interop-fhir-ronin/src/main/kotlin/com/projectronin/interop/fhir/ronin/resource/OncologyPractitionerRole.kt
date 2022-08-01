package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyPractitionerRole](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitionerrole.html) profile.
 */
object OncologyPractitionerRole : BaseRoninProfile<PractitionerRole>(KotlinLogging.logger { }) {
    override fun validateInternal(resource: PractitionerRole, validation: Validation) {
        validation.apply {
            requireTenantIdentifier(resource.identifier, this)

            check(resource.telecom.all { it.system != null && it.value != null }) {
                "All PractitionerRole telecoms require a value and system"
            }
        }
    }

    override fun transformInternal(original: PractitionerRole, tenant: Tenant): Pair<PractitionerRole, Validation> {
        val validation = validation {
            notNull(original.id) { "no FHIR id" }
            notNull(original.practitioner) { "no practitioner" }
        }

        val telecoms = original.telecom.filter { it.system != null && it.value != null }
        val telecomDifference = original.telecom.size - telecoms.size
        if (telecomDifference > 0) {
            logger.info { "$telecomDifference telecoms removed from PractitionerRole ${original.id} due to missing system and/or value" }
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            period = original.period?.localize(tenant),
            practitioner = original.practitioner?.localize(tenant),
            organization = original.organization?.localize(tenant),
            code = original.code.map { it.localize(tenant) },
            specialty = original.specialty.map { it.localize(tenant) },
            location = original.location.map { it.localize(tenant) },
            healthcareService = original.healthcareService.map { it.localize(tenant) },
            telecom = telecoms.map { it.localize(tenant) },
            availableTime = original.availableTime.map { it.localize(tenant) },
            notAvailable = original.notAvailable.map { it.localize(tenant) },
            endpoint = original.endpoint.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
