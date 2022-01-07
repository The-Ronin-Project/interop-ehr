package com.projectronin.interop.fhir.r4.resource

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
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus

/**
 * A booking of a healthcare event among patient(s), practitioner(s), related person(s) and/or device(s) for a specific
 * date/time. This may result in one or more Encounter(s).
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/R4/appointment.html)
 */
data class Appointment(
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val text: Narrative? = null,
    override val contained: List<ContainedResource> = listOf(),
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val identifier: List<Identifier> = listOf(),
    val status: AppointmentStatus,
    val cancellationReason: CodeableConcept? = null,
    val serviceCategory: List<CodeableConcept> = listOf(),
    val serviceType: List<CodeableConcept> = listOf(),
    val specialty: List<CodeableConcept> = listOf(),
    val appointmentType: CodeableConcept? = null,
    val reasonCode: List<CodeableConcept> = listOf(),
    val reasonReference: List<Reference> = listOf(),
    val priority: Int? = null,
    val description: String? = null,
    val supportingInformation: List<Reference> = listOf(),
    val start: Instant? = null,
    val end: Instant? = null,
    val minutesDuration: Int? = null,
    val slot: List<Reference> = listOf(),
    val created: DateTime? = null,
    val comment: String? = null,
    val patientInstruction: String? = null,
    val basedOn: List<Reference> = listOf(),
    val participant: List<Participant>,
    val requestedPeriod: List<Period> = listOf()

) : DomainResource {
    init {
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
            require(minutesDuration > 0) { "Appointment duration must be positive" }
        }

        priority?.let {
            require(priority >= 0) { "Priority cannot be negative" }
        }

        require(participant.isNotEmpty()) { "At least one participant must be provided" }
        require(participant.all { it.type.isNotEmpty() || it.actor.isNotEmpty() }) {
            "[app-1](https://www.hl7.org/fhir/R4/appointment.html#invs): Either the type or actor on the participant SHALL be specified"
        }
    }
}
