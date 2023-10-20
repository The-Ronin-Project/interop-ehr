package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.tenant.config.model.Tenant

interface ServiceRequestService : FHIRService<ServiceRequest> {
    /**
     * Finds a list of patient [ServiceRequest]s for a date range.
     */
    fun getServiceRequestsForPatient(
        tenant: Tenant,
        patientFhirId: String
    ):
        List<ServiceRequest>
}
