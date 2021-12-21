package com.projectronin.interop.ehr.model.base

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.projectronin.interop.ehr.model.EHRResource

/**
 * Base implementation suitable for FHIR resources that want to provide lazy access to specific getters based on the raw response. It is expected that all raw responses are associated to a FHIR element containing a "resource" object that can be processed.
 */
abstract class FHIRResource(override val raw: String) : EHRResource {
    /**
     * A lazy-loaded [JsonObject] of the resource contained within the raw JSON String that was provided.
     */
    protected val jsonObject: JsonObject by lazy {
        Parser.default().parse(StringBuilder(raw)) as JsonObject
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FHIRResource

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }
}
