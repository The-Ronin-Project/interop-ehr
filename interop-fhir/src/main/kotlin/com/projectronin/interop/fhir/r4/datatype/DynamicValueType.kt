package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.common.jackson.readValueAs
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Oid
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Time
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.datatype.primitive.Uuid
import kotlin.reflect.KClass

/**
 * Enumeration of the available types of [DynamicValue]s.
 */
enum class DynamicValueType(override val code: String, private val kClass: KClass<*>? = null) :
    CodedEnum<DynamicValueType>, DynamicValueReader {
    ADDRESS("Address", Address::class),
    AGE("Age", Age::class),
    ANNOTATION("Annotation", Annotation::class),
    ATTACHMENT("Attachment", Attachment::class),
    BASE_64_BINARY("Base64Binary", Base64Binary::class),
    BOOLEAN("Boolean") {
        override fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any = jsonNode.asBoolean()
    },
    CANONICAL("Canonical", Canonical::class),
    CODE("Code", Code::class),
    CODEABLE_CONCEPT("CodeableConcept", CodeableConcept::class),
    CODING("Coding", Coding::class),
    CONTACT_DETAIL("ContactDetail", ContactDetail::class),
    CONTACT_POINT("ContactPoint", ContactPoint::class),
    CONTRIBUTOR("Contributor", Contributor::class),
    COUNT("Count", Count::class),
    DATA_REQUIREMENT("DataRequirement", DataRequirement::class),
    DATE("Date", Date::class),
    DATE_TIME("DateTime", DateTime::class),
    DECIMAL("Decimal") {
        override fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any = jsonNode.asDouble()
    },
    DISTANCE("Distance", Distance::class),
    DOSAGE("Dosage", Dosage::class),
    DURATION("Duration", Duration::class),
    EXPRESSION("Expression", Expression::class),
    HUMAN_NAME("HumanName", HumanName::class),
    ID("Id", Id::class),
    IDENTIFIER("Identifier", Identifier::class),
    INSTANT("Instant", Instant::class),
    INTEGER("Integer") {
        override fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any = jsonNode.asInt()
    },
    MARKDOWN("Markdown", Markdown::class),
    META("Meta", Meta::class),
    MONEY("Money", Money::class),
    OID("Oid", Oid::class),
    PARAMETER_DEFINITION("ParameterDefinition", ParameterDefinition::class),
    PERIOD("Period", Period::class),
    POSITIVE_INT("PositiveInt", PositiveInt::class),
    QUANTITY("Quantity", Quantity::class),
    RANGE("Range", Range::class),
    RATIO("Ratio", Ratio::class),
    REFERENCE("Reference", Reference::class),
    RELATED_ARTIFACT("RelatedArtifact", RelatedArtifact::class),
    SAMPLED_DATA("SampledData", SampledData::class),
    SIGNATURE("Signature", Signature::class),
    STRING("String") {
        override fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any = jsonNode.asText()
    },
    TIME("Time", Time::class),
    TIMING("Timing", Timing::class),
    TRIGGER_DEFINITION("TriggerDefinition", TriggerDefinition::class),
    UNSIGNED_INT("UnsignedInt", UnsignedInt::class),
    URI("Uri", Uri::class),
    URL("Url", Url::class),
    USAGE_CONTEXT("UsageContext", UsageContext::class),
    UUID("Uuid", Uuid::class);

    override fun readValue(jsonNode: JsonNode, jsonParser: JsonParser): Any {
        kClass ?: throw IllegalStateException("No class provided for enum")
        return jsonNode.readValueAs(jsonParser, kClass.java)
    }
}
