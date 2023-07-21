package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.RequestGroupService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component

@Component
class CernerRequestGroupService(
    cernerClient: CernerClient
) : RequestGroupService,
    CernerFHIRService<RequestGroup>(cernerClient) {
    override val fhirURLSearchPart = ""
    override val fhirResourceType = RequestGroup::class.java

    /**
     * Cerner does not support Request Group, simply returning an empty-map
     */
    override fun getByIDs(
        tenant: Tenant,
        resourceFHIRIds: List<String>
    ): Map<String, RequestGroup> {
        return emptyMap()
    }

    /**
     * Cerner does not support Request Group, simply returning an empty-map
     */
    @Deprecated("Use getByIDs")
    @Trace
    override fun getRequestGroupByFHIRId(
        tenant: Tenant,
        requestGroupIds: List<String>
    ): Map<String, RequestGroup> {
        return emptyMap()
    }
}
