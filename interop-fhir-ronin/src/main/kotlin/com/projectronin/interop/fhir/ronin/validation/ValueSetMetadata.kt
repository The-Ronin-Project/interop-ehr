package com.projectronin.interop.fhir.ronin.validation

import com.projectronin.interop.fhir.validate.IssueMetadata

data class ValueSetMetadata(
    override val registryEntryType: String,
    val valueSetName: String,
    val valueSetUuid: String,
    val version: String
) : IssueMetadata
