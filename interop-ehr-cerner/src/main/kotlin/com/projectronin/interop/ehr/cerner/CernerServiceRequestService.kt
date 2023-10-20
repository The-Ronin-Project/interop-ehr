package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ServiceRequestService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerServiceRequestService(
    cernerClient: CernerClient
) : ServiceRequestService, CernerFHIRService<ServiceRequest>(cernerClient) {
    override val fhirURLSearchPart = "/ServiceRequest"
    override val fhirResourceType = ServiceRequest::class.java

    @Trace
    override fun getServiceRequestsForPatient(tenant: Tenant, patientFhirId: String): List<ServiceRequest> {
        val parameters = mapOf("patient" to patientFhirId)
        return getResourceListFromSearch(tenant, parameters)
    }
}
