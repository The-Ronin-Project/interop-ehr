package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata

data class ValueSetList(
    val codes: List<Coding>,
    val metadata: ValueSetMetadata? = null
)
