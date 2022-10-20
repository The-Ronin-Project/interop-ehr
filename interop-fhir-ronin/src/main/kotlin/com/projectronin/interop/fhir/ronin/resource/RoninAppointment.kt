package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.validate.resource.R4AppointmentValidator
import com.projectronin.interop.fhir.ronin.getFhirIdentifiers
import com.projectronin.interop.fhir.ronin.profile.RoninProfile
import com.projectronin.interop.fhir.ronin.resource.base.BaseRoninProfile
import com.projectronin.interop.fhir.ronin.util.toFhirIdentifier
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Validator and Transformer for the Ronin Appointment profile.
 */
object RoninAppointment :
    BaseRoninProfile<Appointment>(R4AppointmentValidator, RoninProfile.APPOINTMENT.value) {
    override fun validate(element: Appointment, parentContext: LocationContext, validation: Validation) {
        validation.apply {
            requireRoninIdentifiers(element.identifier, parentContext, this)

            // TODO: RoninNormalizedAppointmentStatus extension
        }
    }

    override fun transformInternal(
        normalized: Appointment,
        parentContext: LocationContext,
        tenant: Tenant
    ): Pair<Appointment?, Validation> {
        // TODO: RoninNormalizedAppointmentStatus extension

        val transformed = normalized.copy(
            meta = normalized.meta.transform(),
            identifier = normalized.identifier + normalized.getFhirIdentifiers() + tenant.toFhirIdentifier()
        )
        return Pair(transformed, Validation())
    }
}
