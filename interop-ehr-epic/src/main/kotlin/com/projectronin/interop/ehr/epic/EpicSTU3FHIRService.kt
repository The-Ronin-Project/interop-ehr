package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.stu3.resource.STU3Resource
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking

abstract class EpicSTU3FHIRService<STU3 : STU3Resource<STU3>, R4 : Resource<R4>>(
    epicClient: EpicClient,
    private val stu3Class: Class<STU3>,
    batchSize: Int = 10
) : EpicFHIRService<R4>(epicClient, batchSize) {
    @Trace
    override fun getByID(tenant: Tenant, resourceFHIRId: String): R4 {
        val stu3 = runBlocking {
            epicClient.get(tenant, "$fhirURLSearchPart/$resourceFHIRId")
                .stu3Body(TypeInfo(stu3Class.kotlin, stu3Class))
        }

        @Suppress("UNCHECKED_CAST")
        return stu3.transformToR4() as R4
    }

    @Trace
    override fun getByIDs(tenant: Tenant, resourceFHIRIds: List<String>): Map<String, R4> {
        return runBlocking {
            val chunkedIds = resourceFHIRIds.toSet().chunked(batchSize)
            val resource = chunkedIds.map { idSubset ->
                val parameters = mapOf("_id" to idSubset)
                getResourceListFromSearchSTU3(tenant, parameters)
            }.flatten()
            resource.associateBy { it.id!!.value!! }
        }
    }
}
