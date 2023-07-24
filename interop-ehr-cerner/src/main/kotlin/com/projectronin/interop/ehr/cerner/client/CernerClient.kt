package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.common.http.NO_RETRY_HEADER
import com.projectronin.interop.common.http.request
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.client.EHRClient
import com.projectronin.interop.ehr.client.RepeatingParameter
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

@Component
class CernerClient(
    private val client: HttpClient,
    private val authenticationBroker: EHRAuthenticationBroker,
    datalakePublishService: DatalakePublishService
) : EHRClient(client, authenticationBroker, datalakePublishService) {
    private val logger = KotlinLogging.logger { }

    override suspend fun getImpl(
        tenant: Tenant,
        urlPart: String,
        parameters: Map<String, Any?>,
        disableRetry: Boolean,
        acceptTypeOverride: ContentType
    ): HttpResponse {
        logger.debug { "Started GET call to tenant: ${tenant.mnemonic}" }

        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")
        val requestUrl =
            if (urlPart.first() == '/') {
                tenant.vendor.serviceEndpoint + urlPart
            } else {
                urlPart
            }

        val response: HttpResponse = client.request("Cerner Organization: ${tenant.name}", requestUrl) { requestedUrl ->
            get(requestedUrl) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    append(NO_RETRY_HEADER, "$disableRetry")
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
        return response
    }

    override suspend fun postImpl(
        tenant: Tenant,
        urlPart: String,
        requestBody: Any,
        parameters: Map<String, Any?>
    ): HttpResponse {
        logger.debug { "Started POST call to tenant: ${tenant.mnemonic}" }

        val authentication = authenticationBroker.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")

        val response: HttpResponse =
            client.request("Cerner Organization: ${tenant.name}", tenant.vendor.serviceEndpoint + urlPart) { url ->
                post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.FhirJson)
                    contentType(ContentType.Application.FhirJson)
                    setBody(requestBody)
                }
            }

        logger.debug { "HTTP status, ${response.status}, returned for POST call to tenant: ${tenant.mnemonic}" }

        return response
    }
}
