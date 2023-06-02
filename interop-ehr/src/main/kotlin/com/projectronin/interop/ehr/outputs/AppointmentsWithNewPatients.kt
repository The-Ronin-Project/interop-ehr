package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient

/**
 * Convenience class to encapsulate the return values for Appointment Service.
 * The service may end up having to query the EHR to find patients that don't exist yet in EHRDA; [newPatients] caches those [Patient]s
 * so that we can add those to an EHRDA queue without duplicating the call to the EHR.
 */
data class AppointmentsWithNewPatients(val appointments: List<Appointment>, val newPatients: List<Patient>? = null)
