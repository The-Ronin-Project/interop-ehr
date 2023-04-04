package com.projectronin.interop.fhir.ronin.normalization

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDateTime

/**
 * A singleton object containing a cache of the current list of ConceptMap objects
 * shared by INFX in the OCI data store. Each item in the [registry] is a [NormalizationRegistryItem]
 * containing metadata and a squashed version of the ConceptMap or ValueSet for ease of use.
 * This object should only ever be directly accessed by an instance of [NormalizationRegistryClient].
 *
 * Note: A registry cache for a given tenant includes ALL registry items that apply to that tenant.
 */
internal object NormalizationRegistryCache {
    private val lastUpdated = mutableMapOf<String, LocalDateTime>()
    private var registry = listOf<NormalizationRegistryItem>()

    fun getCurrentRegistry(): List<NormalizationRegistryItem> {
        return registry
    }

    fun setNewRegistry(new: List<NormalizationRegistryItem>, mnemonic: String) {
        registry = new
        lastUpdated[mnemonic] = LocalDateTime.now()
    }

    fun reloadNeeded(mnemonic: String): Boolean {
        return lastUpdated[mnemonic]?.isBefore(LocalDateTime.now().minusHours(2)) ?: true
    }
}

internal data class NormalizationRegistryItem(
    val registry_uuid: String,
    val data_element: String, // i.e. 'Appointment.status'
    val filename: String,
    val version: String? = null,
    val source_extension_url: String? = null, // non-null for ConceptMap
    val resource_type: String, // i.e. 'Appointment' - repeated in data_element
    val tenant_id: String? = null, // null applies to all tenants
    val profile_url: String? = null,
    val concept_map_name: String? = null,
    val concept_map_uuid: String? = null,
    val value_set_name: String? = null,
    val value_set_uuid: String? = null,
    @JsonDeserialize(keyUsing = NullKey::class)
    @JsonIgnore
    var map: Map<SourceKey, TargetValue>? = null,
    @JsonDeserialize(keyUsing = NullSet::class)
    @JsonIgnore
    var set: List<TargetValue>? = null
)
internal data class SourceKey(val value: String, val system: String)
internal data class TargetValue(val value: String, val system: String, val display: String, val version: String)

// Because we added the 'map' field to NormalizationRegistry (despite it not being in the original JSON from OCI),
// Jackson needs to know 'how' to deserialize it, even though it's marked as JsonIgnore.
private class NullKey : KeyDeserializer() {
    override fun deserializeKey(p0: String?, p1: DeserializationContext?): Any {
        return SourceKey("", "")
    }
}

// Because we added the 'set' field to NormalizationRegistry (despite it not being in the original JSON from OCI),
// Jackson needs to know 'how' to deserialize it, even though it's marked as JsonIgnore.
private class NullSet : KeyDeserializer() {
    override fun deserializeKey(p0: String?, p1: DeserializationContext?): Any {
        return TargetValue("", "", "", "")
    }
}
