package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

const val RONIN_APPOINTMENT_PROFILE =
    "http://projectronin.io/fhir/ronin.common-fhir-model.uscore-r4/StructureDefinition/ronin-appointment"

/**
 * Validator and Transformer for the Ronin Appointment profile.
 */
object RoninAppointment :
    BaseRoninProfile<Appointment>(
        R4AppointmentValidator,
        RONIN_APPOINTMENT_PROFILE
    ) {
    override fun validate(resource: Appointment, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(resource.identifier, parentContext, this)

            // TODO: RoninNormalizedAppointmentStatus extension
        }
    }

    override fun transformInternal(
        original: Appointment,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Appointment?, Validation> {
        // TODO: RoninNormalizedAppointmentStatus extension

        val transformed = original.copy(
            id = original.id?.localize(tenant),
            meta = original.meta.transform(tenant),
            text = original.text?.localize(tenant),
            extension = original.extension.map { it.localize(tenant) },
            modifierExtension = original.modifierExtension.map { it.localize(tenant) },
            identifier = original.identifier.map { it.localize(tenant) } + original.getFhirIdentifiers() + tenant.toFhirIdentifier(),
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
        return Pair(transformed, Validation())
    }
}
