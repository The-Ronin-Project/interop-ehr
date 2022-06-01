package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's appointment service.
 */
interface AppointmentService {
    /**
     * Finds the appointments at a given [tenant] for a patient identified by the [patientMRN] between
     * the [startDate] and [endDate] from an EHR tenant.
     */
    fun findPatientAppointments(
        tenant: Tenant,
        patientMRN: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Bundle<Appointment>

    /**
     * Finds the appointments at a given [tenant] for the [providerIDs] between the [startDate] and [endDate].
     */
    fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Bundle<Appointment>
}
