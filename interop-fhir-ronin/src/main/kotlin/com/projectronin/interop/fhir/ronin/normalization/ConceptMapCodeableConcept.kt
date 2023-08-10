package com.projectronin.interop.fhir.ronin.normalization

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata

data class ConceptMapCodeableConcept(
    val codeableConcept: CodeableConcept,
    val extension: Extension,
    val metadata: List<ConceptMapMetadata>
)
