package com.projectronin.interop.fhir.ronin.validation

import com.projectronin.interop.fhir.validate.IssueMetadata

data class ConceptMapMetadata(
    override val registryEntryType: String,
    val conceptMapName: String,
    val conceptMapUuid: String,
    val version: String
) : IssueMetadata
