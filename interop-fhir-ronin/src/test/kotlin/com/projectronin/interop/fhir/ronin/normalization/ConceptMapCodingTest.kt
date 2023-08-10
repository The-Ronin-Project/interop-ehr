package com.projectronin.interop.fhir.ronin.normalization

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.validation.ConceptMapMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConceptMapCodingTest {
    @Test
    fun `serialize and deserialize JSON`() {
        val conceptMapCoding = ConceptMapCoding(
            coding = Coding(
                system = CodeSystem.SNOMED_CT.uri,
                version = "2023-03-01".asFHIR(),
                code = Code("442327001"),
                display = "Twin liveborn born in hospital (situation)".asFHIR()
            ),
            extension = Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, FHIRString("Value"))
            ),
            metadata = listOf(
                ConceptMapMetadata(
                    registryEntryType = "concept-map",
                    conceptMapName = "test-concept-map",
                    conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
                    version = "1"
                )
            )
        )
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conceptMapCoding)
        val expectedJson = """
            {
              "coding" : {
                "system" : "http://snomed.info/sct",
                "version" : "2023-03-01",
                "code" : "442327001",
                "display" : "Twin liveborn born in hospital (situation)"
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
        val deserializedConceptMap = JacksonManager.objectMapper.readValue<ConceptMapCoding>(json)
        Assertions.assertEquals(deserializedConceptMap, conceptMapCoding)
    }
}
