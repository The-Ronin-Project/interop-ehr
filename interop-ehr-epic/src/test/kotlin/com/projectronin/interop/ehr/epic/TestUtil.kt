package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.LocalTime

inline fun <reified T> readResource(resource: String): T =
    objectMapper.readValue(T::class.java.getResource(resource)!!.readText())

/**
 * Removes all formatting from a supplied JSON string.
 */
fun deformat(json: String): String =
    objectMapper.writeValueAsString(objectMapper.readValue<Any>(json))

fun createTestTenant(
    clientId: String = "clientId",
    serviceEndpoint: String = "http://no.endpo.int",
    privateKey: String = "privateKey",
    tenantMnemonic: String = "mnemonic",
    ehrUserId: String = "ehrUserId",
    messageType: String = "messageType",
    internalId: Int = 1,
    practitionerProviderSystem: String = "providerSystem",
    practitionerUserSystem: String = "userSystem",
    mrnSystem: String = "mrnSystem",
    hsi: String? = null,
    authEndpoint: String? = null,
): Tenant {
    return Tenant(
        internalId,
        tenantMnemonic,
        BatchConfig(LocalTime.MIN, LocalTime.MAX),
        Epic(
            clientId,
            AuthenticationConfig(authEndpoint ?: serviceEndpoint, "pubKey", privateKey),
            serviceEndpoint,
            "instanceName",
            "release",
            ehrUserId,
            messageType,
            practitionerProviderSystem,
            practitionerUserSystem,
            mrnSystem,
            hsi
        )
    )
}

fun getClient(): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            }
        }
        expectSuccess = true
    }
}
