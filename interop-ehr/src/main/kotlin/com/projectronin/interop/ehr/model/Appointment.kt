package com.projectronin.interop.ehr.model

import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus

/**
 * Representation of an Appointment
 */
interface Appointment : EHRResource {
    /**
     * Logical ID of this appointment.
     */
    val id: String

    /**
     * Identifiers for this appointment.
     */
    val identifier: List<Identifier>

    /**
     * Status of the appointment.
     */
    val status: AppointmentStatus?

    /**
     * The style of appointment or patient that has been booked in the slot (not service type).
     */
    val appointmentType: CodeableConcept?

    /**
     * The specific service that is to be performed during this appointment.
     */
    val serviceType: List<CodeableConcept>

    /**
     * When the appointment is to take place.
     */
    val start: String?

    /**
     * List of other resources participating in this appointment
     */
    val participants: List<Participant>
}
