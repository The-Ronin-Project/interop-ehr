package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.AppointmentsWithNewPatients
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's appointment service.
 */
interface AppointmentService : FHIRService<Appointment> {
    /**
     * Finds the appointments at a given [tenant] for a patient identified by the [patientFHIRId] between
     * the [startDate] and [endDate] from an EHR tenant. Optionally takes a [patientMRN] if available,
     * to save on unnecessary calls to EHRDA.
     */
    fun findPatientAppointments(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        patientMRN: String? = null,
        useEHRFallback: Boolean = true,
    ): List<Appointment>

    /**
     * Finds the appointments at a given [tenant] for the [providerIDs] between the [startDate] and [endDate].
     */
    fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): AppointmentsWithNewPatients

    /**
     * Finds the appointments at a given [tenant] for the [locationFHIRIds] between the [startDate] and [endDate].
     */
    fun findLocationAppointments(
        tenant: Tenant,
        locationFHIRIds: List<String>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): AppointmentsWithNewPatients
}
