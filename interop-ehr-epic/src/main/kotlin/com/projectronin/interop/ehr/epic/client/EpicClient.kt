package com.projectronin.interop.ehr.epic.client

import com.projectronin.interop.common.http.request
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Client for Epic based EHR systems.
 */
@Component
class EpicClient(
    private val client: HttpClient,
    private val authenticationBroker: EHRAuthenticationBroker,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Performs a json POST operation for the service url + [urlPart] and the [requestBody] specified for the [tenant]
     *  and returns the [HttpResponse].
     */
    suspend fun post(tenant: Tenant, urlPart: String, requestBody: Any): HttpResponse {
        logger.debug { "Started POST call to tenant: ${tenant.mnemonic}" }

        // Authenticate
        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        // Make the call
        val response: HttpResponse =
            client.request("Epic Organization: ${tenant.name}", tenant.vendor.serviceEndpoint + urlPart) { url ->
                post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
            }

        logger.debug { "HTTP status, ${response.status}, returned for POST call to tenant: ${tenant.mnemonic}" }

        return response
    }

    /**
     * Performs a json GET operation for the service url + [urlPart] and query [parameters] specified for the [tenant]
     * and returns the [HttpResponse].  If [urlPart] is a full url, it doesn't prepend it with the service url and uses
     * as is.
     */
    suspend fun get(tenant: Tenant, urlPart: String, parameters: Map<String, Any?> = mapOf()): HttpResponse {
        logger.debug { "Started GET call to tenant: ${tenant.mnemonic}" }

        // Authenticate
        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        val requestUrl =
            if (urlPart.first() == '/') {
                tenant.vendor.serviceEndpoint + urlPart
            } else {
                urlPart
            }

        val response: HttpResponse = client.request("Epic Organization: ${tenant.name}", requestUrl) { requestedUrl ->
            get(requestedUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                url {
                    parameters.map { parameterEntry ->
                        val key = parameterEntry.key
                        when (val value = parameterEntry.value) {
                            is List<*> -> {
                                encodedParameters.append(
                                    key,
                                    // tricky, but this takes a list of any objects, converts, them to string, encodes them
                                    // and then combines this in a comma separated list
                                    value.joinToString(separator = ",") { parameterValue ->
                                        parameterValue.toString().encodeURLParameter(spaceToPlus = true)
                                    }
                                )
                            }
                            is RepeatingParameter -> url.parameters.appendAll(key, value.values)
                            else -> parameter(key, value)
                        }
                    }
                }
            }
        }

        logger.debug { "HTTP status, ${response.status}, returned for GET call to tenant: ${tenant.mnemonic}" }

        return response
    }
}
data class RepeatingParameter(val values: List<String>)
