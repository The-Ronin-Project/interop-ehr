package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.tenant.config.model.BatchConfig
import com.projectronin.interop.tenant.config.model.EpicAuthenticationConfig
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.time.LocalTime
import java.time.ZoneId

inline fun <reified T> readResource(resource: String): T = objectMapper.readValue(T::class.java.getResource(resource)!!.readText())

/**
 * Removes all formatting from a supplied JSON string.
 */
fun deformat(json: String): String = objectMapper.writeValueAsString(objectMapper.readValue<Any>(json))

fun createTestTenant(
    clientId: String = "clientId",
    serviceEndpoint: String = "http://no.endpo.int",
    privateKey: String = "privateKey",
    tenantMnemonic: String = "mnemonic",
    tenantName: String = "Memorial National Eastern Masonic Oncology Naturopathic Institute, Consolidated",
    ehrUserId: String = "ehrUserId",
    messageType: String = "messageType",
    internalId: Int = 1,
    practitionerProviderSystem: String = "providerSystem",
    practitionerUserSystem: String = "userSystem",
    mrnSystem: String = "mrnSystem",
    csnSystem: String = "csnSystem",
    orderSystem: String = "orderSystem",
    mrnTypeText: String = "MRN",
    internalSystem: String = "internalSystem",
    hsi: String? = null,
    authEndpoint: String? = null,
    departmentInternalSystem: String = "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980",
    patientOnboardedFlagId: String? = null,
    timezone: String = "Etc/UTC",
    monitoredIndicator: Boolean? = null,
): Tenant {
    return Tenant(
        internalId,
        tenantMnemonic,
        tenantName,
        ZoneId.of(timezone),
        BatchConfig(LocalTime.MIN, LocalTime.MAX),
        Epic(
            clientId,
            EpicAuthenticationConfig(authEndpoint ?: serviceEndpoint, "pubKey", privateKey),
            serviceEndpoint,
            "instanceName",
            "release",
            ehrUserId,
            messageType,
            practitionerProviderSystem,
            practitionerUserSystem,
            mrnSystem,
            internalSystem,
            csnSystem,
            mrnTypeText,
            hsi,
            departmentInternalSystem,
            patientOnboardedFlagId,
            orderSystem,
        ),
        monitoredIndicator,
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
