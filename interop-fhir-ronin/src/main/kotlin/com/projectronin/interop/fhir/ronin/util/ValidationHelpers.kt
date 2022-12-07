package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.common.enums.CodedEnum

/**
 * Confirms that the [targetValue] is a valid code for enum [T], returning the enum if valid, otherwise null.
 */
inline fun <reified T> getCodedEnumOrNull(targetValue: String?): T? where T : Enum<T>, T : CodedEnum<T> =
    runCatching { CodedEnum.byCode<T>(targetValue ?: "") }.getOrNull()
