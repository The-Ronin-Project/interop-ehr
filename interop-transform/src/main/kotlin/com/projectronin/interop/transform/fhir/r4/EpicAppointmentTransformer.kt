package com.projectronin.interop.transform.fhir.r4

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyAppointment
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.AppointmentTransformer
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.projectronin.interop.fhir.epic.Appointment as EpicAppointment
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
        require(appointment.dataSource == DataSource.EPIC_APPORCHARD) { "Appointment is not an Epic AppOrchard resource" }

        val appOrchardAppointment = try {
            JacksonManager.objectMapper.readValue<EpicAppointment>(appointment.raw)
        } catch (e: Exception) {
            logger.warn { "Unable to read Epic AppOrchard Appointment: ${e.message}" }
            return null
        }

        val (startInstant, endInstant) = getStartAndEndInstants(
            appOrchardAppointment.date,
            appOrchardAppointment.appointmentStartTime,
            appOrchardAppointment.appointmentDuration
        )

        // participants include the patient, and all the providers
        // See [DataPlatform](https://github.com/projectronin/dp-databricks-jobs/blob/01b6ba76dc43046d29359783304b7d1ec7259213/jobs/gold/mdaoc/fhir/appointment.py#L151)
        val participants: MutableList<Participant> = mutableListOf()

        appOrchardAppointment.patientId?.let {
            participants.add(
                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Patient/" + appOrchardAppointment.patientId?.trim(),
                            display = appOrchardAppointment.patientName
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED
                )
            )
        }

        participants.addAll(
            appOrchardAppointment.providers.filter { it.providerId != null }.map {
                val (participantStartInstant, participantEndInstant) =
                    getStartAndEndInstants(appOrchardAppointment.date, it.time, it.duration)

                Participant(
                    actor = listOf(
                        Reference(
                            reference = "Practitioner/" + it.providerId?.trim(),
                            display = it.providerName
                        ),
                    ),
                    status = ParticipationStatus.ACCEPTED,
                    period = Period(
                        start = DateTime(participantStartInstant.toString()),
                        end = DateTime(participantEndInstant.toString())
                    )
                )
            }
        )

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

                // Default to entered-in-error to agree with [Data Platform](https://github.com/projectronin/dp-databricks-jobs/blob/513cd599955f5905dd20623b2714de2ab4d9c3c0/jobs/gold/mdaoc/fhir/appointment.py#L114)
                status = when (appOrchardAppointment.appointmentStatus.lowercase()) {
                    "completed" -> AppointmentStatus.FULFILLED
                    "scheduled" -> AppointmentStatus.PENDING
                    "no show" -> AppointmentStatus.NOSHOW
                    "arrived" -> AppointmentStatus.ARRIVED
                    else -> AppointmentStatus.ENTERED_IN_ERROR
                },

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
            logger.warn { "Unable to transform Appointment: ${e.message}" }
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
}
