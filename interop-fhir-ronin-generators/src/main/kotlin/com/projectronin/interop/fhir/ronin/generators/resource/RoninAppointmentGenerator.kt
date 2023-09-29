package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.primitives.CodeGenerator
import com.projectronin.interop.fhir.generators.primitives.UriGenerator
import com.projectronin.interop.fhir.generators.primitives.instant
import com.projectronin.interop.fhir.generators.resources.AppointmentGenerator
import com.projectronin.interop.fhir.generators.resources.appointment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.generators.util.udpIdValue
import com.projectronin.interop.fhir.ronin.profile.RoninExtension
import com.projectronin.interop.fhir.ronin.profile.RoninProfile

fun rcdmAppointment(tenant: String, block: AppointmentGenerator.() -> Unit): Appointment {
    return appointment {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.APPOINTMENT, tenant) {}
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleAppointmentStatusCodes.random())
        // the tenantSourceAppointmentStatus must contain the status of the appointment as its code
        extension.plus(tenantSourceAppointmentStatus(status.generate()?.value!!))
        // if the status is not part of the acceptedForNullStartAndEnd a start and end must be provided
        if (status.generate()?.value!! !in acceptedForNullStartAndEnd) {
            start of instant { }
            end of instant { }
        }
        // if status is one of the cancelation codes, then a cancelation must be provided
        if (status.generate()?.value!! in canceledReasonsCodes) {
            val cancelationReasonToUse =
                if (cancelationReason.generate() == null) cancelationReasonCodeableConcept else cancelationReason.generate()
            cancelationReason of cancelationReasonToUse
        }
        participant of rcdmParticipant(participant.generate(), tenant, "Patient")
    }
}

fun Patient.rcdmAppointment(block: AppointmentGenerator.() -> Unit): Appointment {
    val data = this.referenceData()
    return rcdmAppointment(data.tenantId) {
        block.invoke(this)
        participant of rcdmParticipant(
            participant.generate(),
            data.tenantId,
            "Patient",
            data.udpId
        )
    }
}

val possibleAppointmentStatusCodes = listOf(
    Code("proposed"),
    Code("pending"),
    Code("booked"),
    Code("arrived"),
    Code("fulfilled"),
    Code("cancelled"),
    Code("noshow"),
    Code("entered-in-error"),
    Code("checked-in"),
    Code("waitlist")
)

val acceptedForNullStartAndEnd = listOf("proposed", "cancelled", "waitlist")
val canceledReasonsCodes = listOf("cancelled", "noshow")
val possibleParticipantStatus = listOf(
    Code("accepted"),
    Code("declined"),
    Code("tentative"),
    Code("needs-action")
)

val cancelationReasonCodeableConcept = CodeableConcept(
    coding = listOf(
        Coding(
            system = Uri("http://terminology.hl7.org/CodeSystem/appointment-cancellation-reason"),
            code = Code("oth-weath"),
            display = "Other: Weather".asFHIR()
        )
    )
)

val participantActorReferenceOptions = listOf(
    "Patient",
    "PractitionerRole",
    "Practitioner",
    "Location",
    "RelatedPerson",
    "Device",
    "HealthcareService"
)

fun tenantSourceAppointmentStatus(status: String): Extension {
    return Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS.value),
        value = DynamicValue(
            DynamicValueType.CODING,
            Coding(
                code = Code(status),
                system = Uri("http://appointmentStatus/localCodeSystem")
            )
        )
    )
}

/**
 * Generate an appointment participant list. If [participant] is empty,
 * generate an actor using [type] and [id] (if provided) or using generated
 * values, and return that as the list. Use [type]/[tenantId]-[id] as the
 * actor.reference, [type] as actor.type and an EHRDA actor.type.extension.
 * When [participant] is not empty and a [type] is provided, generate an actor
 * using [type] and [id] or a generated id, and append it to the [participant].
 */
fun rcdmParticipant(
    participant: List<Participant>,
    tenantId: String,
    type: String? = null,
    id: String? = null
): List<Participant> {
    val validList = participant.filter { it.actor?.decomposedType()?.isNotEmpty() == true }
    return if (!validList.isEmpty() && type.isNullOrEmpty()) {
        validList
    } else {
        validList + Participant(
            status = possibleParticipantStatus.random(),
            type = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = UriGenerator().generate(),
                            code = CodeGenerator().generate()
                        )
                    )
                )
            ),
            actor = rcdmReference(
                if (type.isNullOrEmpty()) participantActorReferenceOptions.random() else type,
                if (id.isNullOrEmpty()) udpIdValue(tenantId) else udpIdValue(tenantId, id)
            )
        )
    }
}
