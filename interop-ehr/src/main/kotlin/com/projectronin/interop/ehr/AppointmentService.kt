package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines the functionality for an EHR's appointment service.
 */
interface AppointmentService {
    /**
     * Finds the appointments at a given [tenant] for a patient identified by the [patientMRN] between
     * the [startDate] and [endDate] from an EHR tenant.
     */
    fun findAppointments(
        tenant: Tenant,
        patientMRN: String,
        startDate: String,
        endDate: String
    ): Bundle<Appointment>
}
