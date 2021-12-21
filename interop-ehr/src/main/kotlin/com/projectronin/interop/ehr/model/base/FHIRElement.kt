package com.projectronin.interop.ehr.model.base

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.projectronin.interop.ehr.model.EHRElement

/**
 * Base implementation suitable for FHIR elements that want to provide lazy access to specific getters based on the raw response.
 */
abstract class FHIRElement(override val raw: String) : EHRElement {
    /**
     * A lazy-loaded [JsonObject] of the element contained within the raw JSON String that was provided.
     */
    protected val jsonObject: JsonObject by lazy {
        Parser.default().parse(StringBuilder(raw)) as JsonObject
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FHIRElement

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }
}
