package com.projectronin.interop.ehr.cerner.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.http.request
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.auth.AuthenticationService
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class CernerAuthenticationService(private val client: HttpClient) : AuthenticationService {
    private val logger = KotlinLogging.logger { }

    override val vendorType = VendorType.CERNER

    override fun getAuthentication(tenant: Tenant): Authentication? {
        val vendor = tenant.vendorAs<Cerner>()
        val authURL = vendor.authenticationConfig.authEndpoint
        val clientIdWithSecret = "${vendor.authenticationConfig.accountId}:${vendor.authenticationConfig.secret}"
        val encodedSecret = Base64.getEncoder().encodeToString(clientIdWithSecret.toByteArray())
        val scope = "system/Appointment.read system/Patient.read"

        val httpResponse = runBlocking {
            client.request("Cerner Auth for ${tenant.name}", authURL) { authURL ->
                post(authURL) {
                    headers {
                        append(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                        append(HttpHeaders.Authorization, "Basic $encodedSecret")
                    }
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("grant_type", "client_credentials")
                                append("scope", scope)
                            }
                        )
                    )
                }
            }
        }
        val response = runBlocking { httpResponse.body<CernerAuthentication>() }

        logger.debug { "Completed authentication for $authURL" }
        return response
    }
}
