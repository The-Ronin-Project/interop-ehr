package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.EHRResource

/**
 * Base implementation suitable for JSON bundles.
 */
abstract class JSONBundle<out R : EHRResource, out F : Any>(override val resource: F) : Bundle<R> {
    override val raw: String
        get() = JacksonManager.objectMapper.writeValueAsString(resource)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JSONBundle<*, *>

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }

    /**
     * Searches the [Bundle] level links for one with the given relation.  Returns the URL
     * of the first one found, or null if one doesn't exist.
     */
    fun getURL(relation: String): String? {
        // Chained ?. operators cause code coverage problems
        // return this.links.firstOrNull { it.relation == relation }?.url?.value
        return this.links.firstOrNull { it.relation == relation }?.let { it.url.value }
    }
}
