package com.projectronin.interop.ehr.cerner

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.CernerAuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.LocalTime
import java.time.ZoneId

fun createTestTenant(
    internalId: Int = 1,
    mnemonic: String = "mnemonic",
    name: String = "Test Name",
    timezone: String = "Etc/UTC",
    authEndpoint: String = "https://auth.endpoint.com/smart-v1/token",
    clientId: String = "client-id",
    secret: String = "secretsecretsecret",
    serviceEndpoint: String = "https://serviceendpoint.cerner.com/r4/",
    mrnSystem: String = "mrnSystem",
    practitioner: String = "practitioner",
    messageTopic: String? = "topic",
    messageCategory: String? = "category",
    messagePriority: String? = "priority"
): Tenant {
    return Tenant(
        internalId,
        mnemonic,
        name,
        ZoneId.of(timezone),
        BatchConfig(LocalTime.MIN, LocalTime.MAX),
        Cerner(
            "instanceName",
            "clientId",
            CernerAuthenticationConfig(authEndpoint, clientId, secret),
            serviceEndpoint,
            mrnSystem,
            practitioner,
            messageTopic,
            messageCategory,
            messagePriority
        )
    )
}

fun getClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            }
        }
    }
}

inline fun <reified T> readResource(resource: String): T =
    objectMapper.readValue(T::class.java.getResource(resource)!!.readText())
