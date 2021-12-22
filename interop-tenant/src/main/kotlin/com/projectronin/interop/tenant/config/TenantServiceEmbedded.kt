package com.projectronin.interop.tenant.config

import com.projectronin.interop.tenant.config.jackson.JacksonManager
import com.projectronin.interop.tenant.config.model.Tenant
import org.apache.commons.text.StringSubstitutor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException

/**
 * Service responsible for [Tenant]s loaded from a configuration file.
 */
@Service
class TenantServiceEmbedded(@Value("\${interop.tenant.config:tenants.yaml}") private val yamlFile: String) : TenantService {
    private var tenants: Map<String, Tenant> = emptyMap()

    init {
        val tenantYaml =
            StringSubstitutor(System.getenv()).replace(this.javaClass.classLoader.getResource(yamlFile)?.readText())
                ?: throw IOException("Clients configuration file $yamlFile not found.")
        tenants =
            JacksonManager.yamlMapper.readerForListOf(Tenant::class.java).readValue<List<Tenant>?>(tenantYaml)
                .associateBy { it.mnemonic }
    }

    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    override fun getTenantForMnemonic(mnemonic: String): Tenant? {
        return tenants[mnemonic]
    }
}
