package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.fhir.jackson.inbound.r4.DataRequirementDeserializer
import com.projectronin.interop.fhir.jackson.inbound.r4.DateFilterDeserializer
import com.projectronin.interop.fhir.jackson.outbound.r4.DataRequirementSerializer
import com.projectronin.interop.fhir.jackson.outbound.r4.DateFilterSerializer
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.valueset.SortDirection

/**
 * The DataRequirement structure defines a general data requirement for a knowledge asset such as a decision support rule or quality measure.
 *
 * See [FHIR Documentation](http://hl7.org/fhir/R4/metadatatypes.html#DataRequirement)
 */
@JsonDeserialize(using = DataRequirementDeserializer::class)
@JsonSerialize(using = DataRequirementSerializer::class)
data class DataRequirement(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val type: Code,
    val profile: List<Canonical> = listOf(),
    val subject: DynamicValue<Any>? = null,
    val mustSupport: List<String> = listOf(),
    val codeFilter: List<CodeFilter> = listOf(),
    val dateFilter: List<DateFilter> = listOf(),
    val limit: PositiveInt? = null,
    val sort: List<Sort> = listOf()
) : Element {
    companion object {
        val acceptedDynamicTypes = listOf(DynamicValueType.CODEABLE_CONCEPT, DynamicValueType.REFERENCE)
    }

    init {
        subject?.let {
            require(acceptedDynamicTypes.contains(subject.type)) { "subject can only be one of the following: ${acceptedDynamicTypes.joinToString { it.code }}" }
        }
    }
}

data class CodeFilter(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val path: String? = null,
    val searchParam: String? = null,
    val valueSet: Canonical? = null,
    val code: List<Coding> = listOf()
) : Element

@JsonDeserialize(using = DateFilterDeserializer::class)
@JsonSerialize(using = DateFilterSerializer::class)
data class DateFilter(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val path: String? = null,
    val searchParam: String? = null,
    val value: DynamicValue<Any>? = null
) : Element {
    companion object {
        val acceptedDynamicTypes =
            listOf(DynamicValueType.DATE_TIME, DynamicValueType.PERIOD, DynamicValueType.DURATION)
    }

    init {
        value?.let {
            require(acceptedDynamicTypes.contains(value.type)) { "value can only be one of the following: ${acceptedDynamicTypes.joinToString { it.code }}" }
        }
    }
}

data class Sort(
    override val id: String? = null,
    override val extension: List<Extension> = listOf(),
    val path: String,
    val direction: SortDirection
) : Element
