package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.LocationService
import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.PractitionerService
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.epic.apporchard.model.EpicAppointment
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.util.toIdentifier
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.FindPractitionerAppointmentsResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import com.projectronin.interop.fhir.ronin.util.localize
import com.projectronin.interop.fhir.ronin.util.unlocalize
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant as JavaInstant

/**
 * Service providing access to appointments within Epic.
 */
@Component
class EpicAppointmentService(
    epicClient: EpicClient,
    private val patientService: EpicPatientService,
    private val identifierService: EpicIdentifierService,
    private val aidboxPractitionerService: PractitionerService,
    private val aidboxLocationService: LocationService,
    private val aidboxPatientService: PatientService,
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int,
    @Value("\${epic.api.useFhirAPI:false}") private val useFhirAPI: Boolean
) : AppointmentService, EpicPagingService(epicClient) {
    private val logger = KotlinLogging.logger { }
    private val patientAppointmentFhirSearchUrlPart = "/api/FHIR/STU3/Appointment"
    private val patientAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments"
    private val providerAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"
    private val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    override fun findPatientAppointments(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        patientMRN: String? // leverage the MRN if we already have it to avoid unnecessary calls to external systems
    ): List<Appointment> {
        logger.info { "Patient appointment search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "patient" to patientFHIRId,
            "status" to "booked",
            "date" to listOf("ge$startDate", "le$endDate")
        )
        return if (useFhirAPI) {
            getBundleWithPagingSTU3(tenant, patientAppointmentFhirSearchUrlPart, parameters).toListOfType()
        } else {
            val mrnToUse = patientMRN ?: getPatientMRN(tenant, patientFHIRId)
            val appointments = mrnToUse?.let {
                findAppointments(
                    tenant, patientAppointmentSearchUrlPart,
                    GetPatientAppointmentsRequest(
                        userID = tenant.vendorAs<Epic>().ehrUserId,
                        startDate = dateFormat.format(startDate),
                        endDate = dateFormat.format(endDate),
                        patientId = it,
                        patientIdType = tenant.vendorAs<Epic>().patientMRNTypeText
                    )
                ).appointments
            }
            appointments?.let { transformEpicAppointments(tenant, mapOf(patientFHIRId to it)) } ?: emptyList()
        }
    }

    private fun getPatientMRN(tenant: Tenant, patientFHIRId: String): String? {
        // try aidbox first
        val patient = runCatching { aidboxPatientService.getPatient(tenant.mnemonic, patientFHIRId.localize(tenant)) }
            // try EHR next
            .getOrElse { patientService.getPatient(tenant, patientFHIRId) }
        return identifierService.getMRNIdentifier(tenant, patient.identifier).value
    }

    override fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate
    ): FindPractitionerAppointmentsResponse {
        logger.info { "Provider appointment search started for ${tenant.mnemonic}" }

        // Get correct provider IDs for request
        val selectedIdentifiers = providerIDs.mapNotNull {
            val selectedIdentifier = identifierService.getPractitionerProviderIdentifier(tenant, it)

            if (selectedIdentifier.value == null) {
                logger.warn { "Unable to find a value on identifier: $selectedIdentifier" }
                null
            } else {
                selectedIdentifier.value
            }
        }

        if (selectedIdentifiers.isEmpty()) {
            return FindPractitionerAppointmentsResponse(emptyList())
        }

        // Call GetProviderAppointments
        val request = GetProviderAppointmentRequest(
            userID = tenant.vendorAs<Epic>().ehrUserId,
            providers = selectedIdentifiers.map { ScheduleProvider(id = it) },
            startDate = dateFormat.format(startDate),
            endDate = dateFormat.format(endDate)
        )
        val getAppointmentsResponse = findAppointments(tenant, providerAppointmentSearchUrlPart, request)

        // Get list of patient FHIR IDs
        val patientFhirIdResponse = patientService.getPatientsFHIRIds(
            tenant,
            tenant.vendorAs<Epic>().patientInternalSystem,
            getAppointmentsResponse.appointments.map { it.patientId!! }.toSet().toList() // Make Id list unique
        )

        val patientFhirIdToAppointments = getAppointmentsResponse.appointments.filter {
            val patient = patientFhirIdResponse[it.patientId]
            if (patient == null) {
                logger.warn { "FHIR ID not found for patient ${it.patientId}" }
                false
            } else {
                true
            }
        }.groupBy { patientFhirIdResponse[it.patientId]!!.fhirID }

        val r4AppointmentsToReturn = if (useFhirAPI) {
            retrieveAppointmentFromEpic(tenant, patientFhirIdToAppointments)
        } else {
            transformEpicAppointments(tenant, patientFhirIdToAppointments)
        }

        return FindPractitionerAppointmentsResponse(
            r4AppointmentsToReturn,
            patientFhirIdResponse.mapNotNull { it.value.newPatientObject }
        )
    }

    /**
     * Calls an Epic proprietary and returns a [GetAppointmentsResponse]
     */
    private fun findAppointments(
        tenant: Tenant,
        urlPart: String,
        request: Any,
    ): GetAppointmentsResponse {
        logger.info { "Appointment search started for ${tenant.mnemonic}" }

        return runBlocking {
            val httpResponse = epicClient.post(tenant, urlPart, request)
            httpResponse.throwExceptionFromHttpStatus("GetAppointments", urlPart)
            httpResponse.body()
        }
    }

    private fun retrieveAppointmentFromEpic(
        tenant: Tenant,
        patientFhirIdToAppointments: Map<String, List<EpicAppointment>>
    ): List<Appointment> {
        // Setup list of appointments to return
        val appointmentsToReturn: MutableList<Appointment> = mutableListOf()
        // Loop over patients and query Epic for full FHIR object
        patientFhirIdToAppointments.forEach { patient ->
            patient.value.chunked(batchSize) { appointments ->
                val parameters = mapOf(
                    "patient" to patient.key,
                    "identifier" to appointments.joinToString(separator = ",") { appointment ->
                        SystemValue(appointment.id, tenant.vendorAs<Epic>().encounterCSNSystem).queryString
                    }
                )
                appointmentsToReturn.addAll(
                    getBundleWithPagingSTU3(
                        tenant,
                        patientAppointmentFhirSearchUrlPart,
                        parameters
                    ).toListOfType()
                )
            }
        }
        return appointmentsToReturn
    }

    // The following should be safely deleted when we can get FHIR objects directly from Epic

    /***
     *  Transforms the [EpicAppointment] in [patientFhirIdToAppointments] into R4 [Appointment]s.
     *  The Epic appointment provider participants (Epic department or Epic provider)
     *  are looked up in Aidbox to find the FHIR id (FHIR Location or FHIR Practitioner) for participant references.
     *  When a FHIR id is found, for Location or Practitioner, populate the R4 Appointment.participant.reference.
     *  For Practitioner only, if the FHIR id is not found, populate the R4 Appointment.participant.identifer.
     */
    private fun transformEpicAppointments(
        tenant: Tenant,
        patientFhirIdToAppointments: Map<String, List<EpicAppointment>>
    ): List<Appointment> {
        // grab all Epic schedule providers and departments
        val allProviders = patientFhirIdToAppointments.values.flatten().map { it.providers }.flatten()

        // get provider Identifiers
        val provToIdentifier = allProviders.associateWith { provider ->
            val provIdentifier =
                identifierService.getPractitionerIdentifier(tenant, provider.providerIDs.map { it.toIdentifier() })
            if (provIdentifier.system != null && provIdentifier.value != null) {
                provIdentifier
            } else {
                logger.info {
                    "Missing identifier data in Epic appointment provider with Name: ${provider.providerName}: " +
                        "System: ${provIdentifier.system?.value} Value: ${provIdentifier.value}"
                }
                null
            }
        }.filterNot { it.value == null }
        // associate Identifier with SystemValue for lookup
        val provToSystem = provToIdentifier.keys.associateWith { provider ->
            val provIdentifier = provToIdentifier[provider]
            SystemValue(provIdentifier?.value!!, provIdentifier.system!!.value)
        }
        // aidbox lookup for Practitioner FHIR id
        val provSystemToFhirID = aidboxPractitionerService.getPractitionerFHIRIds(
            tenant.mnemonic,
            provToSystem.values.distinct().associateWith { it }
        ) // strip off the tenant prefix found in aidbox
            .entries.associate { it.key to it.value.unlocalize(tenant) }
        // use FHIR id (if found), otherwise Identifier
        val provToParticipant = provToSystem.keys.associateWith { provider ->
            val system = provToSystem[provider]
            if (provSystemToFhirID.containsKey(system)) {
                provSystemToFhirID[system]
            } else {
                logger.info {
                    "Unable to find Practitioner FHIR ID in Ronin clinical data store, for Epic provider with " +
                        "Name: ${provider.providerName} and SystemValue: ${system?.queryString}"
                }
                provToIdentifier[provider]
            }
        }

        // get department Identifiers
        val deptToIdentifier = allProviders.associateWith { provider ->
            val deptIdentifier =
                identifierService.getLocationIdentifier(tenant, provider.departmentIDs.map { it.toIdentifier() })
            if (deptIdentifier.system != null && deptIdentifier.value != null) {
                deptIdentifier
            } else {
                logger.info {
                    "Missing identifier data in Epic appointment provider with Name: ${provider.departmentName}: " +
                        "System: ${deptIdentifier.system?.value} Value: ${deptIdentifier.value}"
                }
                null
            }
        }.filterNot { it.value == null }
        // associate Identifier with SystemValue for lookup in aidbox
        val deptToSystem = deptToIdentifier.keys.associateWith { provider ->
            val deptIdentifier = deptToIdentifier[provider]
            SystemValue(deptIdentifier?.value!!, deptIdentifier.system!!.value)
        }
        // aidbox lookup for Location FHIR id
        val deptSystemToFhirID = aidboxLocationService.getLocationFHIRIds(
            tenant.mnemonic,
            deptToSystem.values.distinct().associateWith { it }
        ) // strip off the tenant prefix found in aidbox
            .entries.associate { it.key to it.value.unlocalize(tenant) }
        // use FHIR id (if found), otherwise null to omit that Location
        val deptToParticipant = deptToSystem.keys.associateWith { provider ->
            val system = deptToSystem[provider]
            if (deptSystemToFhirID.containsKey(system)) {
                deptSystemToFhirID[system]
            } else {
                logger.info {
                    "Unable to find Location FHIR ID in Ronin clinical data store, for Epic department with " +
                        "Name: ${provider.departmentName} and SystemValue: ${system?.queryString}"
                }
                null
            }
        }

        // now transform all the appointments
        return patientFhirIdToAppointments.map { entries ->
            entries.value.map { appointment ->
                appointment.transform(
                    tenant = tenant,
                    patientFHIRId = entries.key,
                    providerToPractitionerFHIRIds = provToParticipant,
                    providerToLocationFHIRIds = deptToParticipant,
                )
            }
        }.flatten()
    }

    // This is PSJ-specific. We will need to update this with the ConceptMap logic once it is ready.
    private val psjStatusToFHIRStatus = mapOf(
        "arrived" to AppointmentStatus.ARRIVED,
        "canceled" to AppointmentStatus.CANCELLED,
        "completed" to AppointmentStatus.FULFILLED,
        "hh incomplete" to AppointmentStatus.CANCELLED,
        "hsp incomplete" to AppointmentStatus.CANCELLED,
        "left without seen" to AppointmentStatus.NOSHOW,
        "no show" to AppointmentStatus.NOSHOW,
        "phoned patient" to AppointmentStatus.BOOKED,
        "present" to AppointmentStatus.ARRIVED,
        "proposed" to AppointmentStatus.PROPOSED,
        "scheduled" to AppointmentStatus.BOOKED
    )

    /**
     * Transforms from an Epic status to a FHIR status. This is internal to simplify testing.
     */
    internal fun transformStatus(epicStatus: String): String {
        // TODO: Replace with ConceptMap
        val transformedStatus = psjStatusToFHIRStatus[epicStatus.lowercase()]
        return if (transformedStatus != null) {
            transformedStatus.code
        } else {
            logger.warn { "No mapping found for status $epicStatus" }
            // We leave the bad appointment status so that we can
            epicStatus
        }
    }

    /***
     * Given an [EpicAppointment], transform it into an [Appointment]. Expects a resolved [patientFHIRId] and
     * [providerToPractitionerFHIRIds] and [providerToLocationFHIRIds] maps containing data to complete the Appointment.participant
     * list for this Appointment. The data in the identifier maps may pertain to any number of EpicAppointments
     * retrieved at the same time; this function will filter these maps for providers for only this EpicAppointment.
     * @param tenant the Tenant
     * @param patientFHIRId is the Patient FHIR ID for the Appointment.participant reference to the patient.
     * @param providerToPractitionerFHIRIds Epic schedule providers mapped to a Practitioner FHIR ID or Identifier.
     *         A FHIR ID means: create an Appointment.participant.actor with a reference attribute using the FHIR ID.
     *         An Identifier means: create an Appointment.participant.actor with an identifier using the Identifier.
     * @param providerToLocationFHIRIds Epic schedule providers mapped to a Location FHIR ID, or to null if no id.
     *         Creates an Appointment.participant.actor with a reference attribute using the FHIR ID.
     */
    private fun EpicAppointment.transform(
        tenant: Tenant,
        patientFHIRId: String,
        providerToPractitionerFHIRIds: Map<ScheduleProviderReturnWithTime, Any?>,
        providerToLocationFHIRIds: Map<ScheduleProviderReturnWithTime, String?>,
    ): Appointment {
        // TODO: Replace with ConceptMap
        val transformedStatus = transformStatus(appointmentStatus)

        val (transformedStartInstant, transformedEndInstant) = getStartAndEndInstants(
            this.date,
            this.appointmentStartTime,
            this.appointmentDuration,
            tenant
        )

        val patientParticipant = Participant(
            actor = Reference(
                reference = "Patient/$patientFHIRId",
                display = this.patientName,
                type = Uri("Patient")
            ),
            status = Code(ParticipationStatus.ACCEPTED.code)
        )

        val practitionerParticipants: List<Participant> = providers.mapNotNull { provider ->
            when (val id = providerToPractitionerFHIRIds[provider]) {
                is Identifier -> {
                    Participant(
                        actor = Reference(
                            identifier = id,
                            display = provider.providerName,
                            type = Uri("Practitioner")
                        ),
                        status = Code(ParticipationStatus.ACCEPTED.code)
                    )
                }
                is String -> {
                    Participant(
                        actor = Reference(
                            reference = "Practitioner/$id",
                            display = provider.providerName,
                            type = Uri("Practitioner")
                        ),
                        status = Code(ParticipationStatus.ACCEPTED.code)
                    )
                }
                else -> {
                    null
                }
            }
        }

        val locationParticipants: List<Participant> = providers.mapNotNull { provider ->
            when (val id = providerToLocationFHIRIds[provider]) {
                null -> {
                    null
                }
                else -> {
                    Participant(
                        actor = Reference(
                            reference = "Location/$id",
                            display = provider.departmentName,
                            type = Uri("Location")
                        ),
                        status = Code(ParticipationStatus.ACCEPTED.code)
                    )
                }
            }
        }

        // even tho some of these nulls are automatically injected upon creation, the whole FHIR object is here,
        // so you can clearly see the logic we are and aren't running
        return Appointment(
            id = Id(this.id), // this is actually the CSN
            meta = null,
            implicitRules = null,
            language = null,
            text = null,
            contained = emptyList(),
            extension = emptyList(),
            modifierExtension = emptyList(),
            identifier = this.contactIDs.map { it.toIdentifier() } +
                // this is just so when we need to eventually convert these to FHIR-based, we have a reliable way
                // to find the old non-FHIR object
                Identifier(
                    value = this.id,
                    system = Uri(tenant.vendorAs<Epic>().encounterCSNSystem),
                    type = CodeableConcept(text = "CSN")
                ),
            status = Code(transformedStatus),
            cancelationReason = null,
            serviceCategory = emptyList(),
            serviceType = emptyList(),
            specialty = emptyList(),
            appointmentType = if (this.visitTypeName.isNotEmpty()) {
                CodeableConcept(text = this.visitTypeName)
            } else {
                null
            },
            reasonCode = emptyList(),
            reasonReference = emptyList(),
            priority = null,
            description = null,
            supportingInformation = emptyList(),
            start = Instant(transformedStartInstant.toString()),
            end = Instant(transformedEndInstant.toString()),
            minutesDuration = this.appointmentDuration.toInt(),
            slot = emptyList(),
            created = null,
            comment = this.appointmentNotes.joinToString(separator = "/n").let { if (it == "") null else it },
            patientInstruction = null,
            basedOn = emptyList(),
            participant = listOf(patientParticipant) + practitionerParticipants + locationParticipants,
            requestedPeriod = emptyList(),
        )
    }

    /**
     * Takes a string representation of the date, start time and duration of an appointment and returns a pair of
     * [Instant]s representing the start and end times of the appointment.
     *
     * Note: We're assuming the [date] and [startTime] are in PST for now. This should be changed to rely on a tenant-level configuration.
     *
     * [date] should be of the format M/d/yyyy.
     * [startTime] should be of the format h:mm.
     * [duration] is the number of minutes the appointment should last.
     */
    private fun getStartAndEndInstants(
        date: String,
        startTime: String,
        duration: String,
        tenant: Tenant
    ): Pair<JavaInstant, JavaInstant> {
        val startDateTime = LocalDateTime.parse(
            "${date.trim()} ${startTime.trim()}",
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a")
        )
        val endDateTime = startDateTime.plusMinutes(duration.toLong())
        val timezone = tenant.timezone

        return Pair(
            startDateTime.atZone(timezone).toInstant(),
            endDateTime.atZone(timezone).toInstant()
        )
    }
}
