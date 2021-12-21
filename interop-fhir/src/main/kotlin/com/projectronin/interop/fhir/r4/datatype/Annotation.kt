package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.AnnotationDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.AnnotationSerializer
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown

/**
 * A text note which also contains information about who made the statement and when.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/datatypes.html#Annotation)
 */
@JsonDeserialize(using = AnnotationDeserializer::class)
@JsonSerialize(using = AnnotationSerializer::class)
data class Annotation(
    override val id: String? = null,
    override val extension: List<com.projectronin.interop.fhir.r4.datatype.Extension> = listOf(),
    val author: DynamicValue<Any>? = null,
    val time: DateTime? = null,
    val text: Markdown
) : com.projectronin.interop.fhir.r4.datatype.Element {
    companion object {
        val acceptedDynamicTypes = listOf(DynamicValueType.REFERENCE, DynamicValueType.STRING)
    }

    init {
        author?.let {
            require(com.projectronin.interop.fhir.r4.datatype.Annotation.Companion.acceptedDynamicTypes.contains(author.type)) { "author can only be one of the following: ${com.projectronin.interop.fhir.r4.datatype.Annotation.Companion.acceptedDynamicTypes.joinToString { it.code }}" }
        }
    }
}
