package com.projectronin.interop.ehr.model.helper

import com.beust.klaxon.JsonObject
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.base.FHIRResource

/**
 * Returns an enum from the supplied [fieldName]  directly. If no such field exists, or it can not be found with the enum, then null will be returned.
 *
 * Note: Coverage requires inline functions to not included default argument values
 */
inline fun <reified T> JsonObject.enum(
    fieldName: String
): T? where T : CodedEnum<T>, T : Enum<T> = this.string(fieldName)?.let { CodedEnum.byCode<T>(it) }

/**
 * Returns an enum from the supplied [fieldName]  directly. If no such field exists, or it can not be found with the enum, then [defaultValue] will be returned.
 *
 * Note: Coverage requires inline functions to not included default argument values
 */
inline fun <reified T> JsonObject.enum(
    fieldName: String,
    defaultValue: T?
): T? where T : CodedEnum<T>, T : Enum<T> = this.string(fieldName)?.let { CodedEnum.byCode<T>(it) } ?: defaultValue

/**
 * Returns an enum from the supplied [fieldName] leveraging a provided [valueMap] or directly. If no such field exists, or it can not be found with the enum, then null will be returned.
 *
 * Note: Coverage requires inline functions to not included default argument values
 */
inline fun <reified T> JsonObject.enum(
    fieldName: String,
    valueMap: Map<String, T>
): T? where T : CodedEnum<T>, T : Enum<T> =
    this.string(fieldName)?.let { valueMap[it.lowercase()] ?: CodedEnum.byCode<T>(it) }

/**
 * Returns an enum from the supplied [fieldName] leveraging a provided [valueMap] or directly. If no such field exists, or it can not be found with the enum, then [defaultValue] will be returned.
 *
 * Note: Coverage requires inline functions to not included default argument values
 */
inline fun <reified T> JsonObject.enum(
    fieldName: String,
    valueMap: Map<String, T>,
    defaultValue: T?
): T? where T : CodedEnum<T>, T : Enum<T> =
    this.string(fieldName)?.let { valueMap[it.lowercase()] ?: CodedEnum.byCode<T>(it) } ?: defaultValue

/**
 * Builds [FHIRElement]s from the current JSON object. [fieldName] should represent an array of elements within the object and [creator] used to construct the [FHIRElement] from the raw JSON. If no matching elements are found, an empty List will be returned.
 */
fun <T : FHIRElement> JsonObject.fhirElementList(
    fieldName: String,
    creator: (String) -> T
): List<T> =
    this.array<JsonObject>(fieldName)
        ?.map { creator(it.toJsonString()) } ?: listOf()

/**
 * Builds [FHIRResource]s from the current JSON object. [fieldName] should represent an array of resources within the object, with [resourceType] as the expected "resourceType" for each resource, and [creator] used to construct the [FHIRResource] from the raw JSON. If no matching resources are found, an empty List will be returned.
 */
fun <T : FHIRResource> JsonObject.fhirResourceList(
    fieldName: String,
    resourceType: String,
    creator: (String) -> T
): List<T> =
    this.array<JsonObject>(fieldName)
        ?.map { it.obj("resource") }
        ?.filter { it?.string("resourceType") == resourceType }
        ?.map { creator(it!!.toJsonString()) } ?: listOf()

/**
 * Merges [bundle1] and [bundle2] into a new [FHIRBundle] of the same type.
 */
fun <R : FHIRResource, B : FHIRBundle<R>> mergeBundles(
    bundle1: B,
    bundle2: B,
    creator: (String) -> B
): B {
    val combinedJSON = (bundle1.resources + bundle2.resources).joinToString(
        separator = """},{"resource": """,
        prefix = """{"entry": [{"resource": """,
        postfix = """}]}"""
    ) { it.raw }

    return creator(combinedJSON)
}

/**
 * Merges a list of [FHIRBundle]s of the same resource into a single [FHIRBundle]
 */
fun <R : FHIRResource, B : FHIRBundle<R>> mergeBundles(bundles: List<B>, creator: (String) -> B): B {
    return bundles.reduce { acc, bundle -> mergeBundles(acc, bundle, creator) }
}
