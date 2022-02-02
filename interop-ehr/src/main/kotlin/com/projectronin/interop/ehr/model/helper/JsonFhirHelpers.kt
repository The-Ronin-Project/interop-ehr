package com.projectronin.interop.ehr.model.helper

import com.beust.klaxon.JsonObject
import com.projectronin.interop.common.enums.CodedEnum

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
