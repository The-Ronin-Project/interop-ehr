package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.BaseFHIRService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime

abstract class CernerFHIRService<T : Resource<T>>(
    val cernerClient: CernerClient,
    private val batchSize: Int = 10
) : BaseFHIRService<T>() {
    private val logger = KotlinLogging.logger { }
    abstract val fhirURLSearchPart: String
    override val standardParameters: Map<String, Any> = mapOf("_count" to 20)

    // Auth scopes required for this service. By default we set read, but if a service needs something different
    // it can override these values.
    open val readScope: Boolean = true
    open val writeScope: Boolean = false

    @Trace
    override fun getByID(tenant: Tenant, resourceFHIRId: String): T {
        return runBlocking {
            cernerClient.get(tenant, "$fhirURLSearchPart/$resourceFHIRId")
                .body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
        }
    }

    @Trace
    override fun getByIDs(tenant: Tenant, resourceFHIRIds: List<String>): Map<String, T> {
        return runBlocking {
            val chunkedIds = resourceFHIRIds.toSet().chunked(batchSize)
            val resource = chunkedIds.map { idSubset ->
                val parameters = mapOf("_id" to idSubset)
                getResourceListFromSearch(tenant, parameters)
            }.flatten()
            resource.associateBy { it.id!!.value!! }
        }
    }

    internal fun getResourceListFromSearch(
        tenant: Tenant,
        parameters: Map<String, Any?>,
        disableRetry: Boolean = false
    ): List<T> {
        return getBundleWithPaging(tenant, parameters, disableRetry).entry.mapNotNull { it.resource }
            .filterIsInstance(fhirResourceType)
    }

    internal fun getBundleWithPaging(
        tenant: Tenant,
        parameters: Map<String, Any?>,
        disableRetry: Boolean = false
    ): Bundle {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val standardizedParameters = standardizeParameters(parameters)

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        cernerClient.get(tenant, fhirURLSearchPart, standardizedParameters, disableRetry)
                    } else {
                        cernerClient.get(tenant, nextURL!!, disableRetry = disableRetry)
                    }
                httpResponse.body<Bundle>()
            }
            responses.add(bundle)
            nextURL = bundle.link.firstOrNull { it.relation?.value == "next" }?.url?.value
        } while (nextURL != null)
        logger.info { "Get completed for ${tenant.mnemonic}" }
        return mergeResponses(responses)
    }

    /**
     * Cerner has some restrictive rules on date params. They allow only 'ge' and 'lt', and they require a timestamp.
     * This function formats the date params correctly. Some resources (only CarePlan) use 'le' instead of 'lt'. Those
     * should use getAltDateParam
     */
    protected fun getDateParam(startDate: LocalDate, endDate: LocalDate, tenant: Tenant): RepeatingParameter {
        val offset = tenant.timezone.rules.getOffset(LocalDateTime.now())
        return RepeatingParameter(
            listOf(
                "ge${startDate}T00:00:00$offset",
                "lt${endDate.plusDays(1)}T00:00:00$offset"
            )
        )
    }

    /**
     * For use on resources that 'le' rather than 'lt'. Only CarePlan for now
     */
    protected fun getAltDateParam(startDate: LocalDate, endDate: LocalDate, tenant: Tenant): RepeatingParameter {
        val offset = tenant.timezone.rules.getOffset(LocalDateTime.now())
        return RepeatingParameter(
            listOf(
                "ge${startDate}T00:00:00$offset",
                "le${endDate.plusDays(1)}T00:00:00$offset"
            )
        )
    }
}
