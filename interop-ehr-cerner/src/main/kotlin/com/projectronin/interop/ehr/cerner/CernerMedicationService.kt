package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerMedicationService(
    cernerClient: CernerClient
) : MedicationService, CernerFHIRService<Medication>(cernerClient) {
    override val fhirURLSearchPart = "/Medication"
    override val fhirResourceType = Medication::class.java

    @Trace
    override fun getMedicationsByFhirId(tenant: Tenant, medicationFhirIds: List<String>): List<Medication> {
        val parameters = mapOf("_id" to medicationFhirIds.joinToString(","))
        return getResourceListFromSearch(tenant, parameters)
    }
}
