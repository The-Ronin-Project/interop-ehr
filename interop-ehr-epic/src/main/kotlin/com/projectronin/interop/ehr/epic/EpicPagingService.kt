package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Abstract class to simplify Epic services that need to retrieve bundles using paging.
 */
abstract class EpicPagingService(private val epicClient: EpicClient) {
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

                if (httpResponse.status != HttpStatusCode.OK) {
                    logger.error { "Get failed for ${tenant.mnemonic} with a ${httpResponse.status}" }
                    throw IOException("Get failed for ${tenant.mnemonic} with a ${httpResponse.status}")
                }
                httpResponse.body<Bundle>()
            }

            responses.add(bundle)
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
