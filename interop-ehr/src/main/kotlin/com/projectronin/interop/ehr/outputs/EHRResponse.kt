package com.projectronin.interop.ehr.outputs

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.fhir.stu3.resource.STU3Resource
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.TypeInfo

/**
 * A class representing an HTTP response and its source URL.
 *
 * @param httpResponse The HTTP response.
 * @param sourceURL The URL of the raw data stored in datalake.
 */
class EHRResponse(val httpResponse: HttpResponse, val sourceURL: String) {
    /**
     * Gets the response body as an object of type T.
     *
     * @return The response body as an object of type T.
     */
    suspend inline fun <reified T> body(): T {
        val body = this.httpResponse.body<T>()
        return if (body is Resource<*>) {
            body.addMetaSource(sourceURL) as T
        } else if (body is STU3Resource<*>) {
            body.addMetaSource(sourceURL) as T
        } else {
            body
        }
    }

    /**
     * Gets the response body as an object of type T, using the specified type information.
     *
     * @param typeInfo The type information for the response body.
     * @return The response body as an object of type T.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Resource<T>> body(typeInfo: TypeInfo): T {
        val resource = this.httpResponse.body(typeInfo) as T
        return resource.addMetaSource(sourceURL) as T
    }

    /**
     * Gets the response body as an object of type T, using the specified type information.
     *
     * @param typeInfo The type information for the response body.
     * @return The response body as an object of type T.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : STU3Resource<T>> stu3Body(typeInfo: TypeInfo): T {
        val resource = this.httpResponse.body(typeInfo) as T
        return resource.addMetaSource(sourceURL) as T
    }
}

/**
 * Adds the source URL as metadata to this R4 resource and any nested resources.
 *
 * @param sourceUrl The URL of the source.
 * @return The resource with the added metadata.
 */
fun Resource<*>.addMetaSource(sourceUrl: String): Resource<*> {
    val updatedMeta = this.meta ?: Meta()
    this.meta = updatedMeta.copy(source = Uri(sourceUrl))
    if (this is Bundle) {
        // recursion! scary
        this.entry.forEach { it.resource?.addMetaSource(sourceUrl) }
    }
    return this
}

/**
 * Adds the source URL as metadata to this STU3 resource and any nested resources.
 *
 * @param sourceUrl The URL of the source.
 * @return The resource with the added metadata.
 */
fun STU3Resource<*>.addMetaSource(sourceUrl: String): STU3Resource<*> {
    val updatedMeta = this.meta ?: Meta()
    this.meta = updatedMeta.copy(source = Uri(sourceUrl))
    if (this is STU3Bundle) {
        // recursion! scary
        this.entry.forEach { it.resource?.addMetaSource(sourceUrl) }
    }
    return this
}
