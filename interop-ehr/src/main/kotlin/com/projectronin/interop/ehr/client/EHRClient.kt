package com.projectronin.interop.ehr.client

import com.projectronin.interop.common.http.NO_RETRY_HEADER
import com.projectronin.interop.common.http.request
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.outputs.EHRResponse
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import mu.KotlinLogging

abstract class EHRClient(
    private val client: HttpClient,
    private val authenticationBroker: EHRAuthenticationBroker,
    private val datalakeService: DatalakePublishService
) {
    private val logger = KotlinLogging.logger { }

    suspend fun get(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?> = mapOf(),
        disableRetry: Boolean = false,
        acceptTypeOverride: ContentType = ContentType.Application.Json
    ): EHRResponse {
        return publishAndReturn(
            getImpl(tenant, urlPart, parameters, disableRetry, acceptTypeOverride),
            tenant,
            disableRetry
        )
    }

    suspend fun post(
        tenant: Tenant,
        urlPart: String,
        requestBody: Any,
        parameters: Map<String, Any?> = mapOf()
    ): EHRResponse {
        return publishAndReturn(postImpl(tenant, urlPart, requestBody, parameters), tenant)
    }

    /**
     * Performs a json POST operation for the service url + [urlPart] and the [requestBody] specified for the [tenant]
     *  and returns the [HttpResponse].
     */
    protected open suspend fun postImpl(
        tenant: Tenant,
        urlPart: String,
        requestBody: Any,
        parameters: Map<String, Any?> = mapOf()
    ): HttpResponse {
        logger.debug { "Started POST call to tenant: ${tenant.mnemonic}" }

        // Authenticate
        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        // Make the call
        val response: HttpResponse =
            client.request("Epic Organization: ${tenant.name}", tenant.vendor.serviceEndpoint + urlPart) { urlToCall ->
                post(urlToCall) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
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

        logger.debug { "HTTP status, ${response.status}, returned for POST call to tenant: ${tenant.mnemonic}" }

        return response
    }

    /**
     * Performs a json GET operation for the service url + [urlPart] and query [parameters] specified for the [tenant]
     * and returns the [HttpResponse].  If [urlPart] is a full url, it doesn't prepend it with the service url and uses
     * as is.
     */
    protected open suspend fun getImpl(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?> = mapOf(),
        disableRetry: Boolean = false,
        acceptTypeOverride: ContentType = ContentType.Application.Json
    ): HttpResponse {
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

        val response: HttpResponse = client.request("Organization: ${tenant.name}", requestUrl) { urlToCall ->
            get(urlToCall) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    append(NO_RETRY_HEADER, "$disableRetry")
                }
                accept(acceptTypeOverride)
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

    private suspend fun publishAndReturn(
        response: HttpResponse,
        tenant: Tenant,
        disablePublication: Boolean = false
    ): EHRResponse {
        if (disablePublication) {
            return EHRResponse(response, "")
        }

        // publish all responses to datalake
        val transactionURL = datalakeService.publishRawData(
            tenant.mnemonic,
            response.bodyAsText(),
            response.request.url.toString()
        )
        return EHRResponse(response, transactionURL)
    }
}

data class RepeatingParameter(val values: List<String>)
