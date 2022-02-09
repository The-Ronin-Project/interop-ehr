package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.AppointmentService
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsRequest
import com.projectronin.interop.ehr.epic.apporchard.model.GetPatientAppointmentsResponse
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
    private val appointmentSearchUrlPart =
        "/api/epic/2013/Scheduling/Patient/GETPATIENTAPPOINTMENTS/GetPatientAppointments"

    override fun findAppointments(
        tenant: Tenant,
        patientMRN: String,
        startDate: String,
        endDate: String
    ): Bundle<Appointment> {
        logger.info { "Appointment search started for ${tenant.mnemonic}" }

        val vendor = tenant.vendor
        if (vendor !is Epic) throw IllegalStateException("Tenant is not Epic vendor: ${tenant.mnemonic}")

        val request =
            GetPatientAppointmentsRequest(
                userID = vendor.ehrUserId,
                startDate = startDate,
                endDate = endDate,
                patientId = patientMRN,
            )

        val getPatientAppointments = runBlocking {
            val httpResponse = epicClient.post(tenant, appointmentSearchUrlPart, request)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "Appointment search failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<GetPatientAppointmentsResponse>()
        }

        logger.info { "Appointment search completed for ${tenant.mnemonic}" }
        return EpicAppointmentBundle(getPatientAppointments)
    }
}
