package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.jackson.JacksonManager
import com.projectronin.interop.tenant.config.model.Tenant
import org.apache.commons.text.StringSubstitutor
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

/**
 * Service responsible for [Tenant]s loaded from a configuration located at [tenantYamlFile], defaulting to tenants.yaml on the classpath.
 */
@Service
class TenantServiceEmbedded(
    resourceLoader: ResourceLoader,
    @Value("\${interop.tenant.config:classpath:tenants.yaml}") private val tenantYamlFile: String
) : TenantService {
    private var tenants: Map<String, Tenant> = emptyMap()

    init {
        val tenantYaml =
            StringSubstitutor(System.getenv()).replace(String(resourceLoader.getResource(tenantYamlFile).inputStream.readAllBytes()))
        tenants = JacksonManager.yamlMapper.readerForListOf(Tenant::class.java).readValue<List<Tenant>?>(tenantYaml)
            .associateBy { it.mnemonic }
    }

    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    override fun getTenantForMnemonic(mnemonic: String): Tenant? {
        return tenants[mnemonic]
    }
}
