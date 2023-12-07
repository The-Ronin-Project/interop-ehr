package com.projectronin.interop.fhir.ronin.normalization

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConceptMapCodeableConceptTest {
    @Test
    fun `serialize and deserialize JSON`() {
        val conceptMapCodeableConcept =
            ConceptMapCodeableConcept(
                codeableConcept =
                    CodeableConcept(
                        id = FHIRString("12345"),
                        extension =
                            listOf(
                                Extension(
                                    url = Uri("http://localhost/extension"),
                                    value = DynamicValue(DynamicValueType.STRING, FHIRString("Value")),
                                ),
                            ),
                        coding = listOf(Coding(system = Uri("coding-system"))),
                        text = FHIRString("concept"),
                    ),
                extension =
                    Extension(
                        url = Uri("http://localhost/extension"),
                        value = DynamicValue(DynamicValueType.STRING, FHIRString("Value")),
                    ),
                metadata =
                    listOf(
                        ConceptMapMetadata(
                            registryEntryType = "concept-map",
                            conceptMapName = "test-concept-map",
                            conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
                            version = "1",
                        ),
                    ),
            )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conceptMapCodeableConcept)
        val expectedJson =
            """
            {
              "codeableConcept" : {
                "id" : "12345",
                "extension" : [ {
                  "url" : "http://localhost/extension",
                  "valueString" : "Value"
                } ],
                "coding" : [ {
                  "system" : "coding-system"
                } ],
                "text" : "concept"
              },
              "extension" : {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              },
              "metadata" : [ {
                "registryEntryType" : "concept-map",
                "conceptMapName" : "test-concept-map",
                "conceptMapUuid" : "573b456efca5-03d51d53-1a31-49a9-af74",
                "version" : "1"
              } ]
            }
            """.trimIndent()
        Assertions.assertEquals(expectedJson, json)
        val deserializedConceptMap = JacksonManager.objectMapper.readValue<ConceptMapCodeableConcept>(json)
        Assertions.assertEquals(deserializedConceptMap, conceptMapCodeableConcept)
    }
}
