package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata

data class ConceptMapCoding(
    val coding: Coding,
    val extension: Extension,
    val metadata: List<ConceptMapMetadata>? = listOf()
)
