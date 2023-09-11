package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service providing access to Medication Request within Cerner.
 */
@Component
class CernerMedicationRequestService(
    cernerClient: CernerClient
) : MedicationRequestService, CernerFHIRService<MedicationRequest>(cernerClient) {
    override val fhirURLSearchPart = "/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    @Deprecated("Use getByID")
    override fun getMedicationRequestById(
        tenant: Tenant,
        medicationRequestId: String
    ): MedicationRequest {
        return getByID(tenant, medicationRequestId)
    }

    override fun getMedicationRequestByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate?,
        endDate: LocalDate? // ignored since cerner doesn't support multiple dates
    ): List<MedicationRequest> {
        val offset = tenant.timezone.rules.getOffset(LocalDateTime.now())
        val dateMap = startDate?.let { mapOf("-timing-boundsPeriod" to "ge${startDate}T00:00:00$offset") }
            ?: emptyMap()
        val parameters = mapOf(
            "patient" to patientFhirId
        ) + dateMap
        return getResourceListFromSearch(tenant, parameters)
    }
}
