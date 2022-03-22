package com.projectronin.interop.ehr.transform

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyAppointment
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines a Transformer capable of converting EHR [Appointment]s into [OncologyAppointment]s.
 */
interface AppointmentTransformer {
    /**
     * Transforms the [Appointment] into an [OncologyAppointment] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformAppointment(appointment: Appointment, tenant: Tenant): OncologyAppointment?

    /**
     * Transforms the [bundle] into a List of [OncologyAppointment]s based on the [tenant]. Only [Appointment]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformAppointments(bundle: Bundle<Appointment>, tenant: Tenant): List<OncologyAppointment>
}
