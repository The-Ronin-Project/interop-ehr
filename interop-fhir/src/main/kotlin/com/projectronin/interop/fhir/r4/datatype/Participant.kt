package com.projectronin.interop.fhir.r4.datatype

import com.projectronin.interop.fhir.r4.valueset.ParticipantRequired
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus

/**
 * List of participants involved in the appointment.
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/appointment-definitions.html#Appointment.participant)
 */
data class Participant(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    override val modifierExtension: List<Extension> = listOf(),
    val type: List<CodeableConcept> = listOf(),
    val actor: List<Reference> = listOf(),
    val required: ParticipantRequired? = null,
    val status: ParticipationStatus,
    val period: Period? = null
) : BackboneElement
