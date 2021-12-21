package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.tenant.config.model.AuthenticationConfig
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import java.time.LocalTime

fun createTestTenant(
    clientId: String,
    serviceEndpoint: String,
    privateKey: String,
    tenantMnemonic: String = "mnemonic",
    ehrUserId: String = "ehrUserId",
    messageType: String = "messageType"
): Tenant {
    return Tenant(
        tenantMnemonic,
        BatchConfig(LocalTime.MIN, LocalTime.MAX),
        Epic(
            clientId,
            AuthenticationConfig("pubKey", privateKey),
            serviceEndpoint,
            "release",
            ehrUserId,
            messageType
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
