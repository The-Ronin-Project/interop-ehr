package com.projectronin.interop.fhir.ronin.conceptmap

import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime

/**
 * A singleton object containing a cache of the current list of ConceptMap objects
 * shared by INFX in the OCI data store. Each item in the [registry] is a [ConceptMapRegistry],
 * which contains metadata about the ConceptMap and a squashed version of it in the form
 * of Map<SourceKey, TargetValue> for ease of use.
 *
 * This object should only ever be directly accessed by an instance of [ConceptMapClient].
 */
internal object ConceptMapCache {
    private val lastUpdated = mutableMapOf<Tenant, LocalDateTime>()
    private var registry = listOf<ConceptMapRegistry>()

    fun getCurrentRegistry(): List<ConceptMapRegistry> {
        return registry
    }

    fun setNewRegistry(new: List<ConceptMapRegistry>, tenantReloaded: Tenant) {
        registry = new
        lastUpdated[tenantReloaded] = LocalDateTime.now()
    }

    fun reloadNeeded(tenant: Tenant): Boolean {
        return lastUpdated[tenant]?.isBefore(LocalDateTime.now().minusHours(2)) ?: true
    }
}

internal data class ConceptMapRegistry(
    val registry_uuid: String,
    val data_element: String,
    val filename: String,
    val version: String,
    val source_extension_url: String,
    val resource_type: String, // i.e. 'Appointment'
    val tenant_id: String?, // potentially null
    var map: Map<SourceKey, TargetValue>? = null
)

internal data class SourceKey(val value: String, val system: String)
internal data class TargetValue(val value: String, val system: String)
