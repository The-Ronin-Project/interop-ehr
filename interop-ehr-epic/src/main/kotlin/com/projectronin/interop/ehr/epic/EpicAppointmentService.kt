package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.common.http.ktor.throwExceptionFromHttpStatus
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.FindPractitionerAppointmentsResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service providing access to appointments within Epic.
 */
@Component
class EpicAppointmentService(
    private val epicClient: EpicClient,
    private val patientService: EpicPatientService,
    private val identifierService: EpicIdentifierService,
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int
) : AppointmentService, EpicPagingService(epicClient) {
    private val logger = KotlinLogging.logger { }
    private val patientAppointmentSearchUrlPart = "/api/FHIR/STU3/Appointment"
    private val providerAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"
    private val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    override fun findPatientAppointments(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Appointment> {
        logger.info { "Patient appointment search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "patient" to patientFHIRId,
            "status" to "booked",
            "date" to listOf("ge$startDate", "le$endDate")
        )
        return getBundleWithPagingSTU3(tenant, patientAppointmentSearchUrlPart, parameters).toListOfType()
    }

    override fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate
    ): FindPractitionerAppointmentsResponse {
        logger.info { "Provider appointment search started for ${tenant.mnemonic}" }
        // Setup list of appointments to return
        val appointmentsToReturn: MutableList<Appointment> = mutableListOf()

        // Get correct provider IDs for request
        val selectedIdentifiers = providerIDs.map {
            val selectedIdentifier = identifierService.getPractitionerProviderIdentifier(tenant, it)
            selectedIdentifier.value
                ?: throw VendorIdentifierNotFoundException("Unable to find a value on identifier: $selectedIdentifier")
        }

        // Call GetProviderAppointments
        val request = GetProviderAppointmentRequest(
            userID = tenant.vendorAs<Epic>().ehrUserId,
            providers = selectedIdentifiers.map { ScheduleProvider(id = it) },
            startDate = dateFormat.format(startDate),
            endDate = dateFormat.format(endDate)
        )
        val getAppointmentsResponse = runBlocking {
            val httpResponse = epicClient.post(tenant, providerAppointmentSearchUrlPart, request)
            httpResponse.throwExceptionFromHttpStatus("GetProviderAppointments", providerAppointmentSearchUrlPart)
            httpResponse.body<GetAppointmentsResponse>()
        }

        // Get list of patient FHIR IDs
        val patientFhirIdResponse = patientService.getPatientsFHIRIds(
            tenant,
            tenant.vendorAs<Epic>().patientInternalSystem,
            getAppointmentsResponse.appointments.map { it.patientId!! }.toSet().toList() // Make Id list unique
        )

        // Build a map of patient FHIR IDs to their appointments
        val patientFhirIdToAppointments = getAppointmentsResponse.appointments.groupBy {
            patientFhirIdResponse[it.patientId]?.fhirID ?: throw VendorIdentifierNotFoundException("FHIR ID not found for patient ${it.patientId}") // This shouldn't be possible
        }

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
                        patientAppointmentSearchUrlPart,
                        parameters
                    ).toListOfType()
                )
            }
        }

        return FindPractitionerAppointmentsResponse(
            appointmentsToReturn,
            patientFhirIdResponse.mapNotNull { it.value.newPatientObject }
        )
    }
}
