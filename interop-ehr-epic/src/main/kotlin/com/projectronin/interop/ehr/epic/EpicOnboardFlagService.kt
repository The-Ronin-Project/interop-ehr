package com.projectronin.interop.ehr.epic

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.OnboardFlagService
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SetSmartDataValuesResult
import com.projectronin.interop.ehr.epic.apporchard.model.SmartDataValue
import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.util.localizeFhirId
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EpicOnboardFlagService(
    private val epicClient: EpicClient,
    private val identifierService: EpicIdentifierService,
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val epicPatientService: EpicPatientService,
) : OnboardFlagService {
    private val apiEndpoint = "/api/epic/2013/Clinical/Utility/SETSMARTDATAVALUES/SmartData/Values"
    private val logger = KotlinLogging.logger { }

    override fun setOnboardedFlag(
        tenant: Tenant,
        patientFhirID: String,
    ): Boolean {
        logger.info { "Setting new flag for ${tenant.mnemonic} and patient $patientFhirID" }
        val epicTenant = tenant.vendorAs<Epic>()
        val flagType =
            epicTenant.patientOnboardedFlagId
                ?: throw IllegalStateException("Tenant ${tenant.mnemonic} is missing patient onboarding flag configuration")
        val patient =
            runBlocking {
                ehrDataAuthorityClient.getResourceAs(
                    tenant.mnemonic,
                    "Patient",
                    patientFhirID.localizeFhirId(tenant.mnemonic),
                ) ?: run {
                    // attempt to get patient from EHR if not in EHRDA
                    runCatching { epicPatientService.getPatient(tenant, patientFhirID) }.getOrNull()
                } ?: throw VendorIdentifierNotFoundException("No Patient found for $patientFhirID")
            }

        val patientMRNIdentifier = identifierService.getMRNIdentifier(tenant, patient.identifier)

        val request =
            SetSmartDataValuesRequest(
                id = patientMRNIdentifier.value!!.value!!,
                idType = epicTenant.patientMRNTypeText,
                userID = epicTenant.ehrUserId,
                userIDType = "External",
                smartDataValues =
                    listOf(
                        SmartDataValue(
                            comments = listOf("Patient has been onboarded in Ronin."),
                            values = listOf("true"),
                            smartDataID = flagType,
                            smartDataIDType = "SDI",
                        ),
                    ),
            )
        val response: SetSmartDataValuesResult =
            runBlocking {
                epicClient.put(
                    tenant = tenant,
                    urlPart = apiEndpoint,
                    requestBody = request,
                ).body()
            }
        // if Epic returns a non-2XX status code, our ktor wrapper will handle throwing an exception,
        // but a cool thing Epic does it return a 2XX code with an "Error" present, we should still treat that
        // as an exception
        if (!response.success) {
            throw AppOrchardError(response.errors.toString())
        }
        return true
    }
}
