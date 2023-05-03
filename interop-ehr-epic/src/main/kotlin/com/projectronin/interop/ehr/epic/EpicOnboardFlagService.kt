package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.OnboardFlagService
import com.projectronin.interop.ehr.epic.apporchard.model.PatientFlag
import com.projectronin.interop.ehr.epic.apporchard.model.SetPatientFlagRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetPatientFlagResponse
import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.aidbox.PatientService as AidboxPatientService

@Component
class EpicOnboardFlagService(
    private val epicClient: EpicClient,
    private val identifierService: EpicIdentifierService,
    private val aidboxPatientService: AidboxPatientService
) : OnboardFlagService {
    private val apiEndpoint = "/api/epic/2011/Billing/Patient/SetPatientFlag/Billing/Patient/Flag"
    private val logger = KotlinLogging.logger { }

    override fun setOnboardedFlag(tenant: Tenant, patientFhirID: String): Boolean {
        logger.info { "Setting new flag for ${tenant.mnemonic} and patient $patientFhirID" }
        val epicTenant = tenant.vendorAs<Epic>()
        val flagType = epicTenant.patientOnboardedFlagId
            ?: throw IllegalStateException("Tenant ${tenant.mnemonic} is missing patient onboarding flag configuration")
        val patient = aidboxPatientService.getPatientByFHIRId(tenant.mnemonic, patientFhirID)
        val patientMRNIdentifier = identifierService.getMRNIdentifier(tenant, patient.identifier)

        val flag = PatientFlag(
            type = flagType
        )
        val postParameters = mapOf(
            "PatientID" to patientMRNIdentifier.value?.value,
            "PatientIDType" to epicTenant.patientMRNTypeText,
            "UserID" to epicTenant.ehrUserId,
            "UserIDType" to "External"
        )
        val response: SetPatientFlagResponse = runBlocking {
            epicClient.post(
                tenant = tenant,
                urlPart = apiEndpoint,
                requestBody = SetPatientFlagRequest(flag),
                parameters = postParameters
            ).body()
        }
        // if Epic returns a non-2XX status code, our ktor wrapper will handle throwing an exception,
        // but a cool thing Epic does it return a 2XX code with an "Error" present, we should still treat that
        // as an exception
        if (response.error.isNotEmpty()) {
            throw AppOrchardError(response.error)
        }
        return response.success
    }
}
