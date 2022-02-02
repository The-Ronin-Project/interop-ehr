package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.classgraph.ClassGraph
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

class ResourceTest {
    @Test
    fun `all subtypes are defined`() {
        // This test is not really running anything (Resource is an interface, after all), but it is verifying a critical
        // piece of information for our system. This test ensures that all final subtypes of this interface are included
        // in the JsonSubTypes. This ensures that Bundles can be built properly.
        val jsonSubTypes = Resource::class.findAnnotation<JsonSubTypes>()?.value?.map { it.value.jvmName }
            ?: fail { "No JsonSubTypes found" }

        val defaultType =
            Resource::class.findAnnotation<JsonTypeInfo>()?.defaultImpl?.jvmName
                ?: fail { "No JsonTypeInfo or default found" }

        val knownSubTypes =
            ClassGraph().acceptPackages("com.projectronin.interop.fhir").enableClassInfo().scan().use {
                it.getClassesImplementing(Resource::class.java).standardClasses.map { c -> c.name }
            }

        val missingSubTypes = knownSubTypes - jsonSubTypes - defaultType
        if (missingSubTypes.isNotEmpty()) {
            fail {
                "JsonSubType not specified for following defined types: ${
                missingSubTypes.joinToString(
                    "\n\t",
                    prefix = "\n\t"
                )
                }"
            }
        }
    }
}
