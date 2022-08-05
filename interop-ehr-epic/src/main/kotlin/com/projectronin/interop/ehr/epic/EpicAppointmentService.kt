package com.projectronin.interop.ehr.epic

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.outputs.FindPractitionerAppointmentsResponse
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
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
    private val identifierService: EpicIdentifierService
) :
    AppointmentService, EpicPagingService(epicClient) {
    private val logger = KotlinLogging.logger { }
    private val patientAppointmentSearchUrlPart =
        "/api/FHIR/STU3/Appointment"
    private val providerAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"
    private val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    override fun findPatientAppointments(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Appointment> {
        logger.info { "Patient appointment search started for ${tenant.mnemonic}" }

        val parameters = mapOf(
            "patient" to patientFHIRId,
            "status" to "booked",
            "date" to "ge$startDate&date=le$endDate"
        )
        return getBundleWithPaging(tenant, patientAppointmentSearchUrlPart, parameters).toListOfType()
    }

    override fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<FHIRIdentifiers>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): FindPractitionerAppointmentsResponse {
        logger.info { "Provider appointment search started for ${tenant.mnemonic}" }
        // instantiate return arrays
        val appointments = mutableListOf<Appointment>()
        val newPatients = mutableListOf<Patient>()

        // get correct provider IDs for request
        val selectedIdentifiers = providerIDs.map {
            val selectedIdentifier = identifierService.getPractitionerProviderIdentifier(tenant, it)
            selectedIdentifier.value
                ?: throw VendorIdentifierNotFoundException("Unable to find a value on identifier: $selectedIdentifier")
        }

        // call GetProviderAppointments
        val request =
            GetProviderAppointmentRequest(
                userID = tenant.vendorAs<Epic>().ehrUserId,
                providers = selectedIdentifiers.map { ScheduleProvider(id = it) },
                startDate = dateFormat.format(startDate),
                endDate = dateFormat.format(endDate)
            )
        val getAppointments = runBlocking {
            val httpResponse = epicClient.post(tenant, providerAppointmentSearchUrlPart, request)
            httpResponse.body<GetAppointmentsResponse>()
        }

        // loop over provider appointment CSNs and query Epic for full FHIR object
        getAppointments.appointments.forEach { appointment ->
            // need to get patient FHIR ID for appointment query
            val patientFHIRIdResponse = patientService.getPatientFHIRId(
                tenant,
                appointment.patientId!!,
                tenant.vendorAs<Epic>().patientInternalSystem
            )
            val parameters = mapOf(
                "patient" to patientFHIRIdResponse.fhirID,
                "identifier" to SystemValue(appointment.id, tenant.vendorAs<Epic>().encounterCSNSystem).queryString
            )
            val fhirResponse =
                getBundleWithPaging(tenant, patientAppointmentSearchUrlPart, parameters).toListOfType<Appointment>()
                    .first() // assume only one appointment returned
            appointments.add(fhirResponse)
            // if we needed to query Epic for a patient, cache it to save performance later
            patientFHIRIdResponse.newPatientObject?.let { newPatients.add(it) }
        }
        return FindPractitionerAppointmentsResponse(appointments, newPatients)
    }
}
