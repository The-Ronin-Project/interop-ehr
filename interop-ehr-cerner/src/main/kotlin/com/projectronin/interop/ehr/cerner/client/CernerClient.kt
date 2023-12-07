package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.common.http.request
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.ehr.auth.EHRAuthenticationBroker
import com.projectronin.interop.ehr.client.EHRClient
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class CernerClient(
    private val client: HttpClient,
    private val authenticationBroker: EHRAuthenticationBroker,
    datalakePublishService: DatalakePublishService,
) : EHRClient(client, authenticationBroker, datalakePublishService) {
    private val logger = KotlinLogging.logger { }

    override suspend fun postImpl(
        tenant: Tenant,
        urlPart: String,
        requestBody: Any,
        parameters: Map<String, Any?>,
    ): HttpResponse {
        logger.debug { "Started POST call to tenant: ${tenant.mnemonic}" }

        val authentication =
            authenticationBroker.getAuthentication(tenant)
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
