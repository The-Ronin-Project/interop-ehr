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
    @Value("\${epic.fhir.batchSize:5}") private val batchSize: Int
) : RequestGroupService, EpicFHIRService<RequestGroup>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/RequestGroup"
    override val fhirResourceType = RequestGroup::class.java

    @Trace
    override fun getRequestGroupByFHIRId(tenant: Tenant, requestGroupIds: List<String>): Map<String, RequestGroup> {
        val chunkedIds = requestGroupIds.toSet().chunked(batchSize)
        val requestGroups = chunkedIds.map { ids ->
            val parameters = mapOf(
                "_id" to ids.joinToString(separator = ",")
            )
            getResourceListFromSearch(tenant, parameters)
        }.flatten()
        return requestGroups.associateBy { it.id!!.value!! }
    }
}
