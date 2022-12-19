package com.projectronin.interop.ehr.epic.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.http.request
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.auth.AuthenticationService
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * Epic implementation of [AuthenticationService]
 */
@Component
class EpicAuthenticationService(private val client: HttpClient) : AuthenticationService {
    private val logger = KotlinLogging.logger { }

    override val vendorType = VendorType.EPIC

    override fun getAuthentication(tenant: Tenant): Authentication? {
        val jti = UUID.randomUUID().toString()
        val vendor = tenant.vendorAs<Epic>()
        val authURL = vendor.authenticationConfig.authEndpoint
        val clientId = vendor.clientId

        logger.debug { "Setting up authentication for $authURL, JTI $jti" }

        // Default is now and in 5 minutes for valid & expiration
        val issueDate = Date()
        val expireAt = Date(issueDate.toInstant().toEpochMilli() + 300000)

        val privateKeySpec = PKCS8EncodedKeySpec(
            Base64.getDecoder().decode(
                // Remove any Key formatting before decoding
                vendor.authenticationConfig.privateKey?.replace("-----BEGIN PRIVATE KEY-----", "")
                    ?.replace("-----END PRIVATE KEY-----", "")
                    ?.replace(" ", "")
                    ?.replace(System.lineSeparator(), "")
            )
        )
        val privateKeyInstance = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec)

        // Determine the audience value (hsi for Tesseract endpoints or authURL for direct endpoints)
        val audience = vendor.hsi ?: authURL
        val token =
            JWT.create().withAudience(audience).withIssuer(clientId).withSubject(clientId).withExpiresAt(expireAt)
                .withNotBefore(issueDate).withIssuedAt(issueDate).withJWTId(jti)
                .sign(Algorithm.RSA384(null, privateKeyInstance as RSAPrivateKey))

        logger.debug { "Calling authentication for $authURL, JTI $jti" }
        val response = runBlocking {
            try {
                val httpResponse: HttpResponse = client.request("Epic Authentication: ${tenant.name}", authURL) { url ->
                    submitForm(
                        url = url,
                        formParameters = Parameters.build {
                            append("grant_type", "client_credentials")
                            append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                            append("client_assertion", token)
                        },
                        encodeInQuery = false
                    )
                }
                httpResponse.body<EpicAuthentication>()
            } catch (e: Exception) {
                logger.error(e) { "Authentication for $authURL, JTI $jti, failed with exception $e" }
                throw e
            }
        }
        logger.info { "Call for ${tenant.mnemonic} successfully authenticated" }
        logger.debug { "Completed authentication for $authURL, JTI $jti" }
        return response
    }
}
