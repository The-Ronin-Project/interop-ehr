package com.projectronin.interop.ehr.client

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
        parameters: Map<String, Any?> = mapOf()
    ): EHRResponse {
        return publishAndReturn(getImpl(tenant, urlPart, parameters), tenant)
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
            client.request("Epic Organization: ${tenant.name}", tenant.vendor.serviceEndpoint + urlPart) { url ->
                post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                    parameters.map {
                        val key = it.key
                        val value = it.value
                        if (value is List<*>) {
                            value.forEach { repetition ->
                                parameter(key, repetition)
                            }
                        } else {
                            value?.let { parameter(key, value) }
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
        parameters: Map<String, Any?> = mapOf()
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

        val response: HttpResponse = client.request("Organization: ${tenant.name}", requestUrl) { url ->
            get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                parameters.map {
                    val key = it.key
                    val value = it.value
                    if (value is List<*>) {
                        value.forEach { repetition ->
                            parameter(key, repetition)
                        }
                    } else {
                        value?.let { parameter(key, value) }
                    }
                }
            }
        }

        logger.debug { "HTTP status, ${response.status}, returned for GET call to tenant: ${tenant.mnemonic}" }

        return response
    }

    private suspend fun publishAndReturn(response: HttpResponse, tenant: Tenant): EHRResponse {
        // publish all responses to datalake
        val transactionURL = datalakeService.publishRawData(
            tenant.mnemonic,
            response.bodyAsText(),
            response.request.url.toString()
        )
        return EHRResponse(response, transactionURL)
    }
}
