package com.projectronin.interop.fhir.r4.datatype.primitive

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Interface defining a primitive datatype.
 */
interface Primitive<T> {
    val value: T
        @JsonValue
        get() = value
}
