package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyCondition](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-condition.html) profile.
 */
object OncologyCondition : BaseRoninProfile<Condition>(KotlinLogging.logger { }) {
    override fun validateInternal(resource: Condition, validation: Validation) {
        validation.apply {
            requireTenantIdentifier(resource.identifier, this)

            check(resource.category.isNotEmpty()) { "At least one category must be provided" }
        }
    }

    override fun transformInternal(original: Condition, tenant: Tenant): Pair<Condition, Validation> {
        val validation = validation {
            notNull(original.id) { "no FHIR id" }
            notNull(original.code) { "No code" }
        }

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            clinicalStatus = original.clinicalStatus?.localize(tenant),
            verificationStatus = original.verificationStatus?.localize(tenant),
            category = original.category.map { it.localize(tenant) },
            severity = original.severity?.localize(tenant),
            code = original.code?.localize(tenant),
            bodySite = original.bodySite.map { it.localize(tenant) },
            subject = original.subject.localize(tenant),
            encounter = original.encounter?.localize(tenant),
            recorder = original.recorder?.localize(tenant),
            asserter = original.asserter?.localize(tenant),
            stage = original.stage.map { it.localize(tenant) },
            evidence = original.evidence.map { it.localize(tenant) },
            note = original.note.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
