package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.primitive.Code

fun generateCode(
    code: Code?,
    possibleCode: Code,
): Code {
    return code?.value?.let { code } ?: possibleCode
}
