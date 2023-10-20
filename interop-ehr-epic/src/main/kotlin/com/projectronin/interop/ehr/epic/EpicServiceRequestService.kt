package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ServiceRequestService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicServiceRequestService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:10}") batchSize: Int
) : ServiceRequestService, EpicFHIRService<ServiceRequest>(epicClient, batchSize) {
    override val fhirURLSearchPart = "/api/FHIR/R4/ServiceRequest"
    override val fhirResourceType = ServiceRequest::class.java

    @Trace
    override fun getServiceRequestsForPatient(tenant: Tenant, patientFhirId: String): List<ServiceRequest> {
        val parameters = mapOf("patient" to patientFhirId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
