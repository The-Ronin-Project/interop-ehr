package com.projectronin.interop.ehr.model.base

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.ehr.model.EHRElement

/**
 * Base implementation suitable for elements based on serializable JSON objects.
 */
abstract class JSONElement(override val element: Any) : EHRElement {
    override val raw: String
        get() = JacksonManager.objectMapper.writeValueAsString(element)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JSONElement

        if (element != other.element) return false

        return true
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }
}
