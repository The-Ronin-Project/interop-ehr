package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.FHIRService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Abstract class to simplify Epic services that need to retrieve bundles using paging.
 */
abstract class EpicFHIRService<T : Resource<T>>(val epicClient: EpicClient) : FHIRService<T> {
    private val logger = KotlinLogging.logger { }
    abstract val fhirURLSearchPart: String
    private val standardParameters: Map<String, Any> = mapOf("_count" to 50)

    @Trace
    override fun getByID(tenant: Tenant, resourceFHIRId: String): T {
        return runBlocking {
            epicClient.get(tenant, "$fhirURLSearchPart/$resourceFHIRId")
                .body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
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

    internal fun getResourceListFromSearchSTU3(tenant: Tenant, parameters: Map<String, Any?>): List<T> {
        return getBundleWithPagingSTU3(tenant, parameters).entry.mapNotNull { it.resource }
            .filterIsInstance(fhirResourceType)
    }

    /**
     * When Epic returns a very large response, it limits the amount of resources returned in each request and provides a
     * follow-up link to retrieve the rest of them.  This function performs a GET to the url provided by the [tenant]
     * and includes the query [parameters].  If there are more resources, it performs multiple GETs
     * until they've all been retrieved, and then combines them into a single [Bundle].
     */
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
                        epicClient.get(tenant, fhirURLSearchPart, standardizedParameters, disableRetry)
                    } else {
                        epicClient.get(tenant, nextURL!!, disableRetry = disableRetry)
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
     * STU3 version of 'getBundleWithPaging'. Still returns an R4 [Bundle], but adds a step to call the
     * transform function for STU3 to R4 provided by interop-fhir. There may be a way to make these functions less
     * redundant, but this is functional and simple.
     */
    internal fun getBundleWithPagingSTU3(
        tenant: Tenant,
        parameters: Map<String, Any?>
    ): Bundle {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val standardizedParameters = standardizeParameters(parameters)

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, fhirURLSearchPart, standardizedParameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }
                // TODO: update to use EHRResponse + transaction ID
                httpResponse.body<STU3Bundle>()
            }

            responses.add(bundle.transformToR4())
            nextURL = bundle.link.firstOrNull { it.relation?.value == "next" }?.url?.value
        } while (nextURL != null)
        logger.info { "Get completed for ${tenant.mnemonic}" }
        return mergeResponses(responses)
    }

    protected fun mergeResponses(
        responses: List<Bundle>
    ): Bundle {
        var bundle = responses.first()
        responses.subList(1, responses.size).forEach { bundle = mergeBundles(bundle, it) }
        return bundle
    }

    /**
     * Standardizes the [parameters], including any standard parameters that have not already been included and returning the combined map.
     */
    private fun standardizeParameters(parameters: Map<String, Any?>): Map<String, Any?> {
        val parametersToAdd = standardParameters.mapNotNull {
            if (parameters.containsKey(it.key)) {
                null
            } else {
                it.toPair()
            }
        }
        return parameters + parametersToAdd
    }
}
