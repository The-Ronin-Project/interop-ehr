package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient

/**
 * Convenience class to encapsulate the return values from a call to 'findPractitionerAppointments'. The service may
 * end up having to query the EHR to find patients that don't exist yet in Aidbox; [newPatients] caches those [Patient]s
 * so that we can add those to an Aidbox queue without duplicating the call to the EHR.
 */
data class FindPractitionerAppointmentsResponse(val appointments: List<Appointment>, val newPatients: List<Patient>? = null)
