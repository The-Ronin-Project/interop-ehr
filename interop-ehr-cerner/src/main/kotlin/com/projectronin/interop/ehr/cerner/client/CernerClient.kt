package com.projectronin.interop.ehr.cerner.client

import com.projectronin.interop.common.http.request
import com.projectronin.interop.ehr.cerner.auth.CernerAuthenticationService
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class CernerClient(private val client: HttpClient, private val cernerAuthenticationService: CernerAuthenticationService) {
    private val logger = KotlinLogging.logger { }

    suspend fun get(tenant: Tenant, urlPart: String, fhirId: String): HttpResponse {
        val authentication = cernerAuthenticationService.getAuthentication(tenant) ?: throw IllegalStateException("Unable to retrieve authentication for ${tenant.mnemonic}")
        val url = tenant.vendor.serviceEndpoint + urlPart + fhirId
        val response = runBlocking {
            client.request(tenant.name, url) { url ->
                get(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                }
            }
        }
        return response
    }
}
