package com.projectronin.interop.fhir.ronin.util

import com.projectronin.interop.fhir.r4.datatype.Reference

fun Reference.isForType(type: String): Boolean =
    this.type?.value == type || (reference?.contains("$type/") ?: false)
