package com.projectronin.interop.fhir.r4.resource.base

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus

/**
 * Base class representing a FHIR R4 Appointment.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/R4/appointment.html)
 */
abstract class BaseAppointment {
    val resourceType: String = "Appointment"

    abstract val id: Id?
    abstract val meta: Meta?
    abstract val implicitRules: Uri?
    abstract val language: Code?
    abstract val text: Narrative?
    abstract val contained: List<ContainedResource>
    abstract val extension: List<Extension>
    abstract val modifierExtension: List<Extension>
    abstract val identifier: List<Identifier>
    abstract val status: AppointmentStatus
    abstract val cancellationReason: CodeableConcept?
    abstract val serviceCategory: List<CodeableConcept>
    abstract val serviceType: List<CodeableConcept>
    abstract val specialty: List<CodeableConcept>
    abstract val appointmentType: CodeableConcept?
    abstract val reasonCode: List<CodeableConcept>
    abstract val reasonReference: List<Reference>
    abstract val priority: Int?
    abstract val description: String?
    abstract val supportingInformation: List<Reference>
    abstract val start: Instant?
    abstract val end: Instant?
    abstract val minutesDuration: Int?
    abstract val slot: List<Reference>
    abstract val created: DateTime?
    abstract val comment: String?
    abstract val patientInstruction: String?
    abstract val basedOn: List<Reference>
    abstract val participant: List<Participant>
    abstract val requestedPeriod: List<Period>

    protected fun validate() {
        require(((start != null) == (end != null))) { "[app-2](https://www.hl7.org/fhir/R4/appointment.html#invs): Either start and end are specified, or neither" }

        if ((start == null) || (end == null)) {
            require(
                listOf(
                    AppointmentStatus.PROPOSED,
                    AppointmentStatus.CANCELLED,
                    AppointmentStatus.WAITLIST
                ).contains(status)
            ) { "[app-3](https://www.hl7.org/fhir/R4/appointment.html#invs): Only proposed or cancelled appointments can be missing start/end dates" }
        }

        cancellationReason?.let {
            require(listOf(AppointmentStatus.CANCELLED, AppointmentStatus.NOSHOW).contains(status)) {
                "[app-4](https://www.hl7.org/fhir/R4/appointment.html#invs): Cancellation reason is only used for appointments that have been cancelled, or no-show"
            }
        }

        minutesDuration?.let {
            require(it > 0) { "Appointment duration must be positive" }
        }

        priority?.let {
            require(it >= 0) { "Priority cannot be negative" }
        }

        require(participant.isNotEmpty()) { "At least one participant must be provided" }
        require(participant.all { it.type.isNotEmpty() || it.actor.isNotEmpty() }) {
            "[app-1](https://www.hl7.org/fhir/R4/appointment.html#invs): Either the type or actor on the participant SHALL be specified"
        }
    }
}
