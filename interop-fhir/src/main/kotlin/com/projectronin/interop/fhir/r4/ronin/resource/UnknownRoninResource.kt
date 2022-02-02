package com.projectronin.interop.fhir.r4.ronin.resource

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.ronin.UnknownRoninResourceDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.ronin.UnknownRoninResourceSerializer
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

@JsonDeserialize(using = UnknownRoninResourceDeserializer::class)
@JsonSerialize(using = UnknownRoninResourceSerializer::class)
data class UnknownRoninResource(
    override val resourceType: String,
    override val id: Id? = null,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    val otherData: Map<String, Any?>
) : RoninResource
