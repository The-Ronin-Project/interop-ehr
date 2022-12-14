package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.FHIRService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import io.ktor.client.call.body
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

abstract class CernerFHIRService<T : Resource<T>>(val cernerClient: CernerClient) : FHIRService<T> {
    private val logger = KotlinLogging.logger { }
    abstract val fhirURLSearchPart: String
    private val standardParameters: Map<String, Any> = mapOf("_count" to 20)

    @Trace
    override fun getByID(tenant: Tenant, resourceFHIRId: String): T {
        return runBlocking {
            cernerClient.get(tenant, "$fhirURLSearchPart/$resourceFHIRId")
                .body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
        }
    }

    internal fun getResourceListFromSearch(tenant: Tenant, parameters: Map<String, Any?>): List<T> {
        return getBundleWithPaging(tenant, parameters).entry.mapNotNull { it.resource }
            .filterIsInstance(fhirResourceType)
    }

    internal fun getBundleWithPaging(
        tenant: Tenant,
        parameters: Map<String, Any?>,
    ): Bundle {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val standardizedParameters = standardizeParameters(parameters)

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        cernerClient.get(tenant, fhirURLSearchPart, standardizedParameters)
                    } else {
                        cernerClient.get(tenant, nextURL!!)
                    }
                httpResponse.body<Bundle>()
            }

            responses.add(bundle)
            nextURL = bundle.link.firstOrNull { it.relation?.value == "next" }?.url?.value
        } while (nextURL != null)
        logger.info { "Get completed for ${tenant.mnemonic}" }
        return mergeResponses(responses)
    }

    protected fun mergeResponses(
        responses: List<Bundle>,
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
