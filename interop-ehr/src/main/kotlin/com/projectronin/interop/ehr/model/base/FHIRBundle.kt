package com.projectronin.interop.ehr.model.base

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.EHRResource

/**
 * Base implementation suitable for FHIR bundles.
 */
abstract class FHIRBundle<out R : EHRResource>(override val raw: String) : Bundle<R> {
    /**
     * A lazy-loaded [JsonObject] of the raw JSON bundle String that was provided.
     */
    protected val jsonObject: JsonObject by lazy {
        Parser.default().parse(StringBuilder(raw)) as JsonObject
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FHIRBundle<*>

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
