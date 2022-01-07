package com.projectronin.interop.fhir.r4.datatype

import com.fasterxml.jackson.core.JsonParser
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
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
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.ContributorType
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.r4.valueset.OperationParameterUse
import com.projectronin.interop.fhir.r4.valueset.RelatedArtifactType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DynamicValueTypeTest {
    @Test
    fun `can read all types`() {
        for (type in DynamicValueType.values()) {
            val testData = testDataByType[type]
            assertNotNull(testData, "testData not available for $type")

            testData?.let {
                val node = getJsonNode(testData.json)
                val parser: JsonParser = getJsonParser(testData.json)

                val value = type.readValue(node, parser)

                assertEquals(testData.expected, value)
            }
        }
    }

    private val testDataByType = mapOf(
        DynamicValueType.ADDRESS to TestData(
            """{"text":"Address"}""",
            Address(text = "Address")
        ),
        DynamicValueType.AGE to TestData(
            """{"value":18.0,"code":"d"}""",
            Age(value = 18.0, code = Code("d"))
        ),
        DynamicValueType.ANNOTATION to TestData(
            """{"text":"text"}""",
            Annotation(text = Markdown("text"))
        ),
        DynamicValueType.ATTACHMENT to TestData("""{"title":"Title"}""", Attachment(title = "Title")),
        DynamicValueType.BASE_64_BINARY to TestData(""""abcd"""", Base64Binary("abcd")),
        DynamicValueType.BOOLEAN to TestData("true", true),
        DynamicValueType.CANONICAL to TestData(""""canonical"""", Canonical("canonical")),
        DynamicValueType.CODE to TestData(""""code"""", Code("code")),
        DynamicValueType.CODEABLE_CONCEPT to TestData("""{"text":"text"}""", CodeableConcept(text = "text")),
        DynamicValueType.CODING to TestData("""{"userSelected":false}""", Coding(userSelected = false)),
        DynamicValueType.CONTACT_DETAIL to TestData("""{"name":"name"}""", ContactDetail(name = "name")),
        DynamicValueType.CONTACT_POINT to TestData("""{"use":"home"}""", ContactPoint(use = ContactPointUse.HOME)),
        DynamicValueType.CONTRIBUTOR to TestData(
            """{"type":"author","name":"name"}""",
            Contributor(type = ContributorType.AUTHOR, name = "name")
        ),
        DynamicValueType.COUNT to TestData("""{"value":24.0,"code":"1"}""", Count(value = 24.0, code = Code("1"))),
        DynamicValueType.DATA_REQUIREMENT to TestData("""{"type":"type"}""", DataRequirement(type = Code("type"))),
        DynamicValueType.DATE to TestData(""""2021-11-19"""", Date("2021-11-19")),
        DynamicValueType.DATE_TIME to TestData(
            """"2015-02-07T13:28:17-05:00"""",
            DateTime("2015-02-07T13:28:17-05:00")
        ),
        DynamicValueType.DECIMAL to TestData("24.0", 24.0),
        DynamicValueType.DISTANCE to TestData(
            """{"value":26.2,"code":"mi"}""",
            Distance(value = 26.2, code = Code("mi"))
        ),
        DynamicValueType.DOSAGE to TestData("""{"text":"text"}""", Dosage(text = "text")),
        DynamicValueType.DURATION to TestData(
            """{"value":"12","code":"h"}""",
            Duration(value = 12.0, code = Code("h"))
        ),
        DynamicValueType.EXPRESSION to TestData(
            """{"language":"en-US","expression":"expression"}""",
            Expression(language = Code("en-US"), expression = "expression")
        ),
        DynamicValueType.HUMAN_NAME to TestData("""{"use":"official"}""", HumanName(use = NameUse.OFFICIAL)),
        DynamicValueType.ID to TestData(""""12345"""", Id("12345")),
        DynamicValueType.IDENTIFIER to TestData("""{"use":"official"}""", Identifier(use = IdentifierUse.OFFICIAL)),
        DynamicValueType.INSTANT to TestData(""""2017-01-01T00:00:00Z"""", Instant("2017-01-01T00:00:00Z")),
        DynamicValueType.INTEGER to TestData("3", 3),
        DynamicValueType.MARKDOWN to TestData(""""markdown"""", Markdown("markdown")),
        DynamicValueType.META to TestData("""{"versionId":"12"}""", Meta(versionId = Id("12"))),
        DynamicValueType.MONEY to TestData("""{"value":1.25}""", Money(value = 1.25)),
        DynamicValueType.OID to TestData(""""urn:oid:1.2.3.4.5"""", Oid("urn:oid:1.2.3.4.5")),
        DynamicValueType.PARAMETER_DEFINITION to TestData(
            """{"use":"in","type":"type"}""",
            ParameterDefinition(use = OperationParameterUse.INPUT, type = Code("type"))
        ),
        DynamicValueType.PERIOD to TestData("""{"start":"2020"}""", Period(start = DateTime("2020"))),
        DynamicValueType.POSITIVE_INT to TestData("""24""", PositiveInt(24)),
        DynamicValueType.QUANTITY to TestData("""{"value":18.0}""", Quantity(value = 18.0)),
        DynamicValueType.RANGE to TestData("""{"low":{"value":0.0}}""", Range(low = SimpleQuantity(value = 0.0))),
        DynamicValueType.RATIO to TestData(
            """{"numerator":{"value":1.0},"denominator":{"value":2.0}}""",
            Ratio(numerator = Quantity(value = 1.0), denominator = Quantity(value = 2.0))
        ),
        DynamicValueType.REFERENCE to TestData("""{"display":"display"}""", Reference(display = "display")),
        DynamicValueType.RELATED_ARTIFACT to TestData(
            """{"type":"citation"}""",
            RelatedArtifact(type = RelatedArtifactType.CITATION)
        ),
        DynamicValueType.SAMPLED_DATA to TestData(
            """{"origin":{"value":1.0},"period":2.0,"dimensions":3}""",
            SampledData(origin = SimpleQuantity(value = 1.0), period = 2.0, dimensions = PositiveInt(3))
        ),
        DynamicValueType.SIGNATURE to TestData(
            """{"type":[{"userSelected":false}],"when":"2017-01-01T00:00:00Z","who":{"display":"who"}}""",
            Signature(
                type = listOf(Coding(userSelected = false)),
                `when` = Instant("2017-01-01T00:00:00Z"),
                who = Reference(display = "who")
            )
        ),
        DynamicValueType.STRING to TestData(""""string"""", "string"),
        DynamicValueType.TIME to TestData(""""08:30:15"""", Time("08:30:15")),
        DynamicValueType.TIMING to TestData(
            """{"code":{"text":"code"}}""",
            Timing(code = CodeableConcept(text = "code"))
        ),
        DynamicValueType.TRIGGER_DEFINITION to TestData("""{"type":"type"}""", TriggerDefinition(type = Code("type"))),
        DynamicValueType.UNSIGNED_INT to TestData(""""48"""", UnsignedInt(48)),
        DynamicValueType.URI to TestData(""""uri"""", Uri("uri")),
        DynamicValueType.URL to TestData(""""http://url"""", Url("http://url")),
        DynamicValueType.USAGE_CONTEXT to TestData(
            """{"code":{"userSelected":true},"valueQuantity":{"value":1.0}}""",
            UsageContext(
                code = Coding(userSelected = true),
                value = DynamicValue(DynamicValueType.QUANTITY, Quantity(value = 1.0))
            )
        ),
        DynamicValueType.UUID to TestData(
            """"urn:uuid:c757873d-ec9a-4326-a141-556f43239520"""",
            Uuid("urn:uuid:c757873d-ec9a-4326-a141-556f43239520")
        )
    )

    private fun getJsonNode(json: String) = objectMapper.readTree(json)
    private fun getJsonParser(json: String): JsonParser = objectMapper.createParser(json)
}

data class TestData(val json: String, val expected: Any)
