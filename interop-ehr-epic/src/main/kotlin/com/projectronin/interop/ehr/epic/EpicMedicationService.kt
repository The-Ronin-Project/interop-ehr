package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicMedicationService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int
) : MedicationService, EpicPagingService(epicClient) {
    private val locationSearchUrlPart = "/api/FHIR/R4/Medication"

    override fun getMedicationsByFhirId(tenant: Tenant, medicationFhirIds: List<String>): List<Medication> {
        val medicationBundles = medicationFhirIds.toSet().chunked(batchSize) {
            val parameters = mapOf("_id" to medicationFhirIds.joinToString(","))
            getBundleWithPaging(tenant, locationSearchUrlPart, parameters).toListOfType<Medication>()
        }
        return medicationBundles.flatten()
    }
}
