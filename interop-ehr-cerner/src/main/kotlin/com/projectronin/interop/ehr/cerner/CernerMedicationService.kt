package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CernerMedicationService(
    cernerClient: CernerClient,
    @Value("\${cerner.fhir.batchSize:10}") private val batchSize: Int,
) : MedicationService, CernerFHIRService<Medication>(cernerClient, batchSize) {
    override val fhirURLSearchPart = "/Medication"
    override val fhirResourceType = Medication::class.java

    @Deprecated("Use getByIDs")
    @Trace
    override fun getMedicationsByFhirId(
        tenant: Tenant,
        medicationFhirIds: List<String>,
    ): List<Medication> {
        return getByIDs(tenant, medicationFhirIds).values.toList()
    }
}
