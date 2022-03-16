package com.projectronin.interop.ehr.epic.transform

import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.ExtensionMeanings
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyAppointment
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.AppointmentTransformer
import com.projectronin.interop.transform.fhir.r4.transformResources
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.projectronin.interop.ehr.epic.apporchard.model.Appointment as AppOrchardAppointment
import com.projectronin.interop.ehr.model.Identifier as EHRIdentifier
import com.projectronin.interop.ehr.model.Participant as EHRParticipant
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant as R4Instant
/**
 * Implementation of [AppointmentTransformer] suitable for Epic Appointments
 */
@Component
class EpicAppointmentTransformer : AppointmentTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformAppointments(
        bundle: Bundle<Appointment>,
        tenant: Tenant
    ): List<OncologyAppointment> {
        require(bundle.dataSource == DataSource.EPIC_APPORCHARD) { "Bundle is not an Epic AppOrchard resource" }

        return bundle.transformResources(tenant, this::transformAppointment)
    }

    override fun transformAppointment(appointment: Appointment, tenant: Tenant): OncologyAppointment? {
        return transformAppointment(appointment, tenant, null, emptyMap())
    }

    fun transformAppointment(appointment: Appointment, tenant: Tenant, patientFhirID: String?, practitionerIdentifierMap: Map<EHRIdentifier, String?>): OncologyAppointment? {
        require(appointment.dataSource == DataSource.EPIC_APPORCHARD) { "Appointment is not an Epic AppOrchard resource" }

        val appOrchardAppointment = appointment.resource as AppOrchardAppointment

        val (startInstant, endInstant) = getStartAndEndInstants(
            appOrchardAppointment.date,
            appOrchardAppointment.appointmentStartTime,
            appOrchardAppointment.appointmentDuration
        )

        // participants include the patient, and all the providers
        // See [DataPlatform](https://github.com/projectronin/dp-databricks-jobs/blob/01b6ba76dc43046d29359783304b7d1ec7259213/jobs/gold/mdaoc/fhir/appointment.py#L151)
        val participants = appointment.participants.map { buildParticipant(it, patientFhirID, practitionerIdentifierMap) }

        try {
            return OncologyAppointment(
                id = Id(appOrchardAppointment.id).localize(tenant),

                // Extension is mapped to a list of [Provider] departments
                extension = appOrchardAppointment.providers.filter { it.departmentId != null }.map {
                    Extension(
                        url = ExtensionMeanings.PARTNER_DEPARTMENT.uri,
                        value = DynamicValue(
                            DynamicValueType.REFERENCE,
                            Reference(reference = "Organization/" + it.departmentId?.trim())
                        )
                    ).localize(tenant)
                },

                // Identifier includes the tenant and ASN.
                // Data Platform uses the ASN identifier as a Reference, so we need to localize it manually.
                // Source [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L98)
                identifier = listOf(
                    Identifier(
                        system = CodeSystem.RONIN_ID.uri,
                        type = CodeableConcepts.RONIN_ID,
                        value = appOrchardAppointment.id.localize(tenant)
                    ).localize(tenant),
                    tenant.toFhirIdentifier()
                ),

                status = appOrchardAppointment.status,

                // Appointment type mapped to visit type name
                // [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/01b6ba76dc43046d29359783304b7d1ec7259213/jobs/gold/mdaoc/fhir/appointment.py#L117)
                appointmentType = CodeableConcept(text = appOrchardAppointment.visitTypeName),

                start = R4Instant(startInstant.toString()),
                end = R4Instant(endInstant.toString()),
                minutesDuration = appOrchardAppointment.appointmentDuration.toInt(),
                comment = appOrchardAppointment.appointmentNotes.joinToString(separator = "\n")
                    .let { if (it == "") null else it },
                participant = participants.map { it.localize(tenant) }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Unable to transform Appointment: ${e.message}" }
            return null
        }
    }

    /**
     * Takes a string representation of the date, start time and duration of an appointment and returns a pair of
     * [Instant]s representing the start and end times of the appointment.
     *
     * Note: We're assuming the [date] and [startTime] are in CST
     * see [DataPlatform](https://github.com/projectronin/dp-databricks-jobs/blob/01b6ba76dc43046d29359783304b7d1ec7259213/jobs/gold/mdaoc/fhir/appointment.py#L231)
     * We should probably look into adding timezone to the tenant and use that instead.
     *
     * [date] should be of the format M/d/yyyy.
     * [startTime] should be of the format h:mm.
     * [duration] is the number of minutes the appointment should last.
     */
    fun getStartAndEndInstants(date: String, startTime: String, duration: String): Pair<Instant, Instant> {
        val startDateTime = LocalDateTime.parse(
            "${date.trim()} ${startTime.trim()}",
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a")
        )
        val endDateTime = startDateTime.plusMinutes(duration.toLong())

        val zoneId = ZoneId.of("America/Chicago")

        return Pair(
            startDateTime.atZone(zoneId).toInstant(),
            endDateTime.atZone(zoneId).toInstant()
        )
    }

    fun buildParticipant(ehrParticipant: EHRParticipant, patientFhirID: String?, practitionerIdentifierMap: Map<EHRIdentifier, String?>): Participant {
        val participantType = ehrParticipant.actor.type
        var reference: String? = null
        var identifier: Identifier? = null
        if (participantType == "Patient" && patientFhirID != null) {
            reference = "Patient/$patientFhirID"
        } else if (participantType == "Practitioner" && practitionerIdentifierMap[ehrParticipant.actor.identifier] != null) {
            val practitionerFhirId = practitionerIdentifierMap[ehrParticipant.actor.identifier]
            reference = "Practitioner/$practitionerFhirId"
        } else {
            identifier = Identifier(
                value = ehrParticipant.actor.identifier?.value,
                type = ehrParticipant.actor.identifier?.type?.text.let { CodeableConcept(text = it) }
            )
        }
        return Participant(
            actor =
            Reference(
                display = ehrParticipant.actor.display,
                identifier = identifier,
                reference = reference,
                type = ehrParticipant.actor.type?.let { Uri(it) }
            ),
            status = ParticipationStatus.ACCEPTED,

        )
    }
}
