package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.model.EHRResource

/**
 * Base implementation suitable for resources based on serializable JSON objects.
 */
abstract class JSONResource(override val resource: Any) : EHRResource {
    override val raw: String
        get() = JacksonManager.objectMapper.writeValueAsString(resource)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JSONResource

        if (resource != other.resource) return false

        return true
    }

    override fun hashCode(): Int {
        return resource.hashCode()
    }
}
