package com.projectronin.interop.fhir.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Manager for Jackson configuration.
 */
class JacksonManager {
    companion object {
        /**
         * The JSON object mapper for Jackson.
         */
        val objectMapper = jsonMapper {
            addModule(kotlinModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            serializationInclusion(JsonInclude.Include.NON_EMPTY)
        }
    }
}
