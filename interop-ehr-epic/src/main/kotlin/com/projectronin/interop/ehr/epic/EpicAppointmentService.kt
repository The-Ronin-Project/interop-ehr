package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.epic.apporchard.model.GetAppointmentsResponse
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetProviderAppointmentRequest
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProvider
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.epic.model.EpicAppointmentBundle
import com.projectronin.interop.ehr.model.Appointment
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.receive
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Service providing access to appointments within Epic.
 */
@Component
class EpicAppointmentService(private val epicClient: EpicClient) :
    AppointmentService {
    private val logger = KotlinLogging.logger { }
    private val patientAppointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments"
    private val providerAppointmentSearchUrlPart =
        "api/epic/2013/Scheduling/Provider/GetProviderAppointments/Scheduling/Provider/Appointments"

    override fun findPatientAppointments(
        tenant: Tenant,
        patientMRN: String,
        startDate: String, // Can be in MM/DD/YYYY format or t+x
        endDate: String,
    ): Bundle<Appointment> {
        logger.info { "Patient appointment search started for ${tenant.mnemonic}" }

        val request =
            GetPatientAppointmentsRequest(
                userID = getEpicVendor(tenant).ehrUserId,
                startDate = startDate,
                endDate = endDate,
                patientId = patientMRN,
            )

        return findAppointments(tenant, patientAppointmentSearchUrlPart, request)
    }

    override fun findProviderAppointments(
        tenant: Tenant,
        providerIDs: List<String>, // Expecting the external Epic provider id
        startDate: String, // Can be in MM/DD/YYYY format or t+x
        endDate: String,
    ): Bundle<Appointment> {
        logger.info { "Provider appointment search started for ${tenant.mnemonic}" }

        val request =
            GetProviderAppointmentRequest(
                userID = getEpicVendor(tenant).ehrUserId,
                providers = providerIDs.map { ScheduleProvider(id = it) },
                startDate = startDate,
                endDate = endDate
            )

        return findAppointments(tenant, providerAppointmentSearchUrlPart, request)
    }

    /**
     * Returns the Vendor from [tenant] and makes sure it's [Epic].
     */
    private fun getEpicVendor(tenant: Tenant): Epic {
        val vendor = tenant.vendor
        if (vendor !is Epic) throw IllegalStateException("Tenant is not Epic vendor: ${tenant.mnemonic}")
        return vendor
    }

    private fun findAppointments(
        tenant: Tenant,
        urlPart: String,
        request: Any,
    ): Bundle<Appointment> {
        logger.info { "Appointment search started for ${tenant.mnemonic}" }

        val getAppointments = runBlocking {
            val httpResponse = epicClient.post(tenant, urlPart, request)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Appointment search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<GetAppointmentsResponse>()
        }

        logger.info { "Appointment search completed for ${tenant.mnemonic}" }
        return EpicAppointmentBundle(getAppointments)
    }
}
