package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationRequestService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to MedicationRequestService within Epic.
 */
@Component
class EpicMedicationRequestService(
    epicClient: EpicClient
) : MedicationRequestService, EpicFHIRService<MedicationRequest>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/MedicationRequest"
    override val fhirResourceType = MedicationRequest::class.java

    @Trace
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
        endDate: LocalDate?
    ): List<MedicationRequest> {
        val dates = mutableListOf<String>()
        startDate?.let { dates += "ge$it" }
        endDate?.let { dates += "le$it" }

        /* if startDate and endDate are null only use patientFhirId otherwise use dates and patientFhirId */
        val parameters = buildMap {
            put("patient", patientFhirId)
            if (dates.isNotEmpty()) put("date", dates)
        }
        return getResourceListFromSearch(tenant, parameters)
    }
}
