package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.file.FileLoader
import com.projectronin.interop.tenant.config.jackson.JacksonManager
import com.projectronin.interop.tenant.config.model.Tenant
import org.apache.commons.text.StringSubstitutor
import org.springframework.beans.factory.annotation.Value

/**
 * Service responsible for [Tenant]s loaded from a configuration located at [tenantYamlFile], defaulting to tenants.yaml on the classpath.
 */
class TenantServiceEmbedded(
    @Value("\${interop.tenant.config:classpath:tenants.yaml}") private val tenantYamlFile: String
) : TenantService {
    private var tenants: Map<String, Tenant> = emptyMap()

    init {
        val tenantYaml = FileLoader.loadFile(tenantYamlFile)
        val substitutedYaml = StringSubstitutor(System.getenv()).replace(tenantYaml)
        tenants =
            JacksonManager.yamlMapper.readerForListOf(Tenant::class.java).readValue<List<Tenant>?>(substitutedYaml)
                .associateBy { it.mnemonic }
    }

    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    override fun getTenantForMnemonic(mnemonic: String): Tenant? {
        return tenants[mnemonic]
    }

    override fun getPoolsForProviders(tenant: Tenant, providerIds: List<String>): Map<String, String> {
        TODO("Not implemented since this class will soon be removed")
    }
}
