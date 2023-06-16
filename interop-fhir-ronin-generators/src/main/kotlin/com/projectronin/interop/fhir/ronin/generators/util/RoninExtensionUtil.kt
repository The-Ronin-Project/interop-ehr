package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.Extension

fun generateExtension(
    extension: List<Extension>,
    possibleExtension: List<Extension>
): List<Extension> {
    return extension.ifEmpty {
        possibleExtension
    }
}
