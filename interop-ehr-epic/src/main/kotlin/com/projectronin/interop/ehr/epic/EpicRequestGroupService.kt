package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.RequestGroupService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EpicRequestGroupService(
    epicClient: EpicClient,
    @Value("\${epic.fhir.batchSize:10}") batchSize: Int,
) : RequestGroupService, EpicFHIRService<RequestGroup>(epicClient, batchSize) {
    override val fhirURLSearchPart = "/api/FHIR/R4/RequestGroup"
    override val fhirResourceType = RequestGroup::class.java

    @Deprecated("Use getByIDs")
    @Trace
    override fun getRequestGroupByFHIRId(
        tenant: Tenant,
        requestGroupIds: List<String>,
    ): Map<String, RequestGroup> {
        return getByIDs(tenant, requestGroupIds)
    }
}
