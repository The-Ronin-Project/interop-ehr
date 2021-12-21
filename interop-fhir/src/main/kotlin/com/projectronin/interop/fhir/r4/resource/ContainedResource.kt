package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.projectronin.interop.fhir.jackson.inbound.r4.ContainedResourceDeserializer

/**
 * Representation of a Contained resource. This is simply the raw JSON associated to a resource in order to allow us to
 * handle all potential types of contained resources. If contained resources are ever explicitly needed, the JSON can
 * then be mapped to an explicitly defined [Resource].
 */
@JsonDeserialize(using = ContainedResourceDeserializer::class)
class ContainedResource(private val raw: String) {
    val value: String
        @JsonRawValue
        @JsonValue
        get() = raw

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContainedResource

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
