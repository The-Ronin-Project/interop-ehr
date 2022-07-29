package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.ExtensionMeanings
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging

/**
 * Validator and Transformer for the Ronin [OncologyAppointment](https://crispy-carnival-61996e6e.pages.github.io/StructureDefinition-oncology-appointment.html) profile.
 */
object OncologyAppointment : BaseRoninProfile<Appointment>(KotlinLogging.logger { }) {
    override fun validate(resource: Appointment) {
        requireTenantIdentifier(resource.identifier)

        val partnerDepartment = resource.extension.find { it.url == ExtensionMeanings.PARTNER_DEPARTMENT.uri }
        partnerDepartment?.let {
            require(it.value?.type == DynamicValueType.REFERENCE) {
                "Partner department reference must be of type Reference"
            }
        }
    }

    override fun transformInternal(original: Appointment, tenant: Tenant): Appointment? {
        val id = original.id
        if (id == null) {
            logger.warn { "Unable to transform Appointment due to missing ID" }
            return null
        }

        return original.copy(
            id = id.localize(tenant),
            meta = original.meta?.localize(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
            cancelationReason = original.cancelationReason?.localize(tenant),
            serviceCategory = original.serviceCategory.map { it.localize(tenant) },
            serviceType = original.serviceType.map { it.localize(tenant) },
            specialty = original.specialty.map { it.localize(tenant) },
            appointmentType = original.appointmentType?.localize(tenant),
            reasonCode = original.reasonCode.map { it.localize(tenant) },
            reasonReference = original.reasonReference.map { it.localize(tenant) },
            supportingInformation = original.supportingInformation.map { it.localize(tenant) },
            slot = original.slot.map { it.localize(tenant) },
            basedOn = original.basedOn.map { it.localize(tenant) },
            participant = original.participant.map { it.localize(tenant) },
            requestedPeriod = original.requestedPeriod.map { it.localize(tenant) }
        )
    }
}
