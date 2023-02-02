package com.projectronin.interop.ehr.cerner.auth

import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.http.request
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.auth.AuthenticationService
import com.projectronin.interop.ehr.cerner.CernerFHIRService
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
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class CernerAuthenticationService(private val client: HttpClient, private val applicationContext: ApplicationContext) : AuthenticationService {
    private val logger = KotlinLogging.logger { }
    override val vendorType = VendorType.CERNER

    private val scope: String by lazy {
        val fhirServiceBeans = applicationContext.getBeansOfType<CernerFHIRService<*>>()

        // Beans are created eagerly at application launch, so this should never happen unless we really screwed up
        if (fhirServiceBeans.isEmpty()) {
            throw IllegalStateException("No CernerFHIRService beans found in SpringApplicationContext.  Cannot build Cerner auth scope.")
        }

        val scopes = mutableListOf<String>()
        fhirServiceBeans.values.map {
            if (it.readScope) scopes.add("system${it.fhirURLSearchPart}.read")
            if (it.writeScope) scopes.add("system${it.fhirURLSearchPart}.write")
        }

        logger.debug { "Cerner auth scope: $scopes" }
        scopes.joinToString(separator = " ")
    }

    override fun getAuthentication(tenant: Tenant): Authentication? {
        val vendor = tenant.vendorAs<Cerner>()
        val authURL = vendor.authenticationConfig.authEndpoint
        val clientIdWithSecret = "${vendor.authenticationConfig.accountId}:${vendor.authenticationConfig.secret}"
        val encodedSecret = Base64.getEncoder().encodeToString(clientIdWithSecret.toByteArray())

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
