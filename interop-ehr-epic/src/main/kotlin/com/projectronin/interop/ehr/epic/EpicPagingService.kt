package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.helper.mergeBundles
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
    fun <R : FHIRResource, B : FHIRBundle<R>> getBundleWithPaging(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?>,
        creator: (String) -> B
    ): B {
        logger.debug { "Get started for ${tenant.mnemonic}" }

        val responses: MutableList<B> = mutableListOf()
        var nextURL: String? = null

        do {
            val json = runBlocking {
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
                httpResponse.receive<String>()
            }

            val response = creator(json)

            responses.add(response)
            nextURL = response.getURL("next")
        } while (nextURL != null)

        logger.debug { "Get completed for ${tenant.mnemonic}" }
        return mergeBundles(responses, creator)
    }
}
