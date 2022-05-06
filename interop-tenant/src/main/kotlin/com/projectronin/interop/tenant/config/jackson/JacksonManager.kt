package com.projectronin.interop.tenant.config.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Manager for Jackson configuration.
 */
class JacksonManager {
    companion object {
        /**
         * The Yaml object mapper for Jackson.
         */
        val yamlMapper: ObjectMapper =
            ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
                .registerModule(SimpleModule())
    }
}
