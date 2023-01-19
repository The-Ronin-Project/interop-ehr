package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.request
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class CernerClient(
    private val client: HttpClient,
    private val cernerAuthenticationService: CernerAuthenticationService
) {
    private val logger = KotlinLogging.logger { }

    suspend fun get(tenant: Tenant, urlPart: String, parameters: Map<String, Any?> = mapOf()): HttpResponse {
        logger.debug { "Started GET call to tenant: ${tenant.mnemonic}" }

        val authentication = cernerAuthenticationService.getAuthentication(tenant)
            ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")
        val requestUrl = tenant.vendor.serviceEndpoint + urlPart

        val response: HttpResponse = client.request("Cerner Organization: ${tenant.name}", requestUrl) { requestedUrl ->
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
        return response
    }
}

data class RepeatingParameter(val values: List<String>)
