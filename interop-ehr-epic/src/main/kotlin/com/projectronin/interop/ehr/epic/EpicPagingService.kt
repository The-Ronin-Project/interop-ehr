package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.model.base.JSONBundle
import com.projectronin.interop.ehr.model.base.JSONResource
import com.projectronin.interop.fhir.r4.mergeBundles
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.receive
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Abstract class to simplify Epic services that need to retrieve bundles using paging.
 */
abstract class EpicPagingService(private val epicClient: EpicClient) {
    protected val logger = KotlinLogging.logger { }

    /**
     * When Epic returns a very large response, it limits the amount of resources returned in each request and provides a
     * follow-up link to retrieve the rest of them.  This function performs a GET to the url provided by the [tenant] +
     * [urlPart] and includes the query [parameters].  If there are more resources, it performs multiple GETs
     * until they've all been retrieved, and then combines them into a single bundle using [creator].
     */
    fun <R : JSONResource, B : JSONBundle<R, Bundle>> getBundleWithPaging(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?>,
        creator: (Bundle) -> B
    ): B {
        logger.info { "Get started for ${tenant.mnemonic}" }

        val responses: MutableList<B> = mutableListOf()
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
                httpResponse.receive<Bundle>()
            }

            val response = creator(bundle)

            responses.add(response)
            nextURL = response.getURL("next")
        } while (nextURL != null)
        logger.info { "Get completed for ${tenant.mnemonic}" }
        return mergeResponses(responses, creator)
    }

    protected fun <R : JSONResource, B : JSONBundle<R, Bundle>> mergeResponses(
        responses: List<B>,
        creator: (Bundle) -> B
    ): B {
        var bundle = responses.first().resource
        responses.subList(1, responses.size).forEach { bundle = mergeBundles(bundle, it.resource) }
        return creator(bundle)
    }
}
