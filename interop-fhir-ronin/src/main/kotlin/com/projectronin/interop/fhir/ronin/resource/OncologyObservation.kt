package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyObservation](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-observation.html) profile.
 */
object OncologyObservation : BaseRoninProfile<Observation>(KotlinLogging.logger { }) {
    override fun validate(resource: Observation) {
        requireTenantIdentifier(resource.identifier)
    }

    override fun transformInternal(original: Observation, tenant: Tenant): Observation? {
        val id = original.id
        if (id == null) {
            logger.warn { "Unable to transform Observation due to missing ID" }
            return null
        }

        return original.copy(
            id = id.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            basedOn = original.basedOn.map { it.localize(tenant) },
            partOf = original.partOf.map { it.localize(tenant) },
            category = original.category.map { it.localize(tenant) },
            code = original.code.localize(tenant),
            subject = original.subject?.localize(tenant),
            focus = original.focus.map { it.localize(tenant) },
            encounter = original.encounter?.localize(tenant),
            performer = original.performer.map { it.localize(tenant) },
            dataAbsentReason = original.dataAbsentReason?.localize(tenant),
            interpretation = original.interpretation.map { it.localize(tenant) },
            bodySite = original.bodySite?.localize(tenant),
            method = original.method?.localize(tenant),
            specimen = original.specimen?.localize(tenant),
            device = original.device?.localize(tenant),
            referenceRange = original.referenceRange.map { it.localize(tenant) },
            hasMember = original.hasMember.map { it.localize(tenant) },
            derivedFrom = original.derivedFrom.map { it.localize(tenant) },
            component = original.component.map { it.localize(tenant) },
            note = original.note.map { it.localize(tenant) }
        )
    }
}
