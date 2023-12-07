package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.datatype.Extension

fun includeExtensions(
    extensions: List<Extension>,
    newExtensions: List<Extension>,
): List<Extension> {
    val extensionsToAdd = newExtensions.filter { new -> extensions.none { it.url == new.url } }
    return extensions + extensionsToAdd
}
