package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.UsageContextDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.UsageContextSerializer

/**
 * The UsageContext structure defines the context of use for a module.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#UsageContext)
 */
@JsonDeserialize(using = UsageContextDeserializer::class)
@JsonSerialize(using = UsageContextSerializer::class)
data class UsageContext(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val code: Coding,
    val value: DynamicValue<Any>
) : Element {
    companion object {
        val acceptedDynamicTypes = listOf(
            DynamicValueType.CODEABLE_CONCEPT,
            DynamicValueType.QUANTITY,
            DynamicValueType.RANGE,
            DynamicValueType.REFERENCE
        )
    }

    init {
        require(acceptedDynamicTypes.contains(value.type)) { "value can only be one of the following: ${acceptedDynamicTypes.joinToString { it.code }}" }
    }
}
