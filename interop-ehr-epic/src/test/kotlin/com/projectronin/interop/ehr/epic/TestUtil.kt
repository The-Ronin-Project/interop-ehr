package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
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
    hsi: String? = null
): Tenant {
    return Tenant(
        internalId,
        tenantMnemonic,
        BatchConfig(LocalTime.MIN, LocalTime.MAX),
        Epic(
            clientId,
            AuthenticationConfig("pubKey", privateKey),
            serviceEndpoint,
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
        install(JsonFeature) {
            serializer = JacksonSerializer() {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }
}
