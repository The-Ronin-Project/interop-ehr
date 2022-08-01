package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyPractitioner](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-practitioner.html) profile.
 */
object OncologyPractitioner : BaseRoninProfile<Practitioner>(KotlinLogging.logger { }) {
    override fun validateInternal(resource: Practitioner, validation: Validation) {
        validation.apply {
            requireTenantIdentifier(resource.identifier, this)

            resource.identifier.find { it.system == CodeSystem.SER.uri }?.let {
                check(it.type == CodeableConcepts.SER) { "SER provided without proper CodeableConcept defined" }
            }

            check(resource.name.isNotEmpty()) { "At least one name must be provided" }
            check(resource.name.all { it.family != null }) { "All names must have a family name provided" }
        }
    }

    override fun transformInternal(original: Practitioner, tenant: Tenant): Pair<Practitioner, Validation> {
        val validation = validation {
            notNull(original.id) { "no FHIR id" }
        }

        val fhirStu3IdIdentifier = Identifier(
            value = original.id?.value,
            system = CodeSystem.FHIR_STU3_ID.uri,
            type = CodeableConcepts.FHIR_STU3_ID
        )

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier() + fhirStu3IdIdentifier,
            name = original.name.map { it.localize(tenant) },
            telecom = original.telecom.map { it.localize(tenant) },
            address = original.address.map { it.localize(tenant) },
            photo = original.photo.map { it.localize(tenant) },
            qualification = original.qualification.map { it.localize(tenant) },
            communication = original.communication.map { it.localize(tenant) }
        )
        return Pair(transformed, validation)
    }
}
