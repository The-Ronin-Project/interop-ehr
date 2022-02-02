package com.projectronin.interop.fhir.r4.resource

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.UnknownResourceDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.UnknownResourceSerializer
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

@JsonDeserialize(using = UnknownResourceDeserializer::class)
@JsonSerialize(using = UnknownResourceSerializer::class)
data class UnknownResource(
    override val resourceType: String,
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    val otherData: Map<String, Any?>
) : Resource
