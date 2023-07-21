package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicMedicationService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:10}") private val batchSize: Int
) : MedicationService, EpicFHIRService<Medication>(epicClient, batchSize) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Medication"
    override val fhirResourceType = Medication::class.java

    @Deprecated("Use getByIDs")
    @Trace
    override fun getMedicationsByFhirId(tenant: Tenant, medicationFhirIds: List<String>): List<Medication> {
        return getByIDs(tenant, medicationFhirIds).values.toList()
    }
}
