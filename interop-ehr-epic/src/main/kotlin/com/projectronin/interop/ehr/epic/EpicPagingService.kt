package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Abstract class to simplify Epic services that need to retrieve bundles using paging.
 */
abstract class EpicPagingService(protected val epicClient: EpicClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * When Epic returns a very large response, it limits the amount of resources returned in each request and provides a
     * follow-up link to retrieve the rest of them.  This function performs a GET to the url provided by the [tenant] +
     * [urlPart] and includes the query [parameters].  If there are more resources, it performs multiple GETs
     * until they've all been retrieved, and then combines them into a single [Bundle].
     */
    fun getBundleWithPaging(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?>,
    ): Bundle {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, urlPart, parameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }
                httpResponse.body<Bundle>()
            }

            responses.add(bundle)
            nextURL = bundle.link.firstOrNull { it.relation == "next" }?.url?.value
        } while (nextURL != null)
        logger.info { "Get completed for ${tenant.mnemonic}" }
        return mergeResponses(responses)
    }

    /**
     * STU3 version of 'getBundleWithPaging'. Still returns an R4 [Bundle], but adds a step to call the
     * transform function for STU3 to R4 provided by interop-fhir. There may be a way to make these functions less
     * redundant, but this is functional and simple.
     */
    fun getBundleWithPagingSTU3(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?>,
    ): Bundle {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val responses: MutableList<Bundle> = mutableListOf()
        var nextURL: String? = null
        do {
            val bundle = runBlocking {
                val httpResponse =
                    if (nextURL == null) {
                        epicClient.get(tenant, urlPart, parameters)
                    } else {
                        epicClient.get(tenant, nextURL!!)
                    }
                httpResponse.body<STU3Bundle>()
            }

            responses.add(bundle.transformToR4())
            nextURL = bundle.link.firstOrNull { it.relation == "next" }?.url?.value
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
}
