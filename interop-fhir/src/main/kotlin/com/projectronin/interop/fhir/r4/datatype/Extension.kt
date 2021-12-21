package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.ExtensionDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.ExtensionSerializer
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * Every element in a resource or data type includes an optional "extension" child element that may be present any number of times.
 *
 * See [FHIR Documentation](https://hl7.org/fhir/R4/extensibility.html#Extension)
 */
@JsonDeserialize(using = ExtensionDeserializer::class)
@JsonSerialize(using = ExtensionSerializer::class)
data class Extension(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val url: Uri,
    val value: DynamicValue<Any>
) : Element
