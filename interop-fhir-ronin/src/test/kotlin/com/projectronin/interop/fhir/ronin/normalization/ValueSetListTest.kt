package com.projectronin.interop.fhir.ronin.normalization

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.validation.ValueSetMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValueSetListTest {
    @Test
    fun `serialize and deserialize JSON`() {
        val valueSet = listOf(
            Coding(
                system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                version = "2023".asFHIR(),
                code = Code("V72.9XXD"),
                display = "Unspecified occupant = bus injured in collision with two- or three-wheeled motor vehicle in traffic accident, subsequent encounter".asFHIR()
            ),
            Coding(
                system = CodeSystem.SNOMED_CT.uri,
                version = "2023-03-01".asFHIR(),
                code = Code("2065009"),
                display = "Dominant hereditary optic atrophy (disorder)".asFHIR()
            ),
            Coding(
                system = CodeSystem.SNOMED_CT.uri,
                version = "2023-03-01".asFHIR(),
                code = Code("66424002"),
                display = "Manual reduction = closed fracture = acetabulum and skeletal traction (procedure)".asFHIR()
            ),
            Coding(
                system = Uri("http://hl7.org/fhir/sid/icd-10-cm"),
                version = "2023".asFHIR(),
                code = Code("T22.361S"),
                display = "Burn = third degree = right scapular region, sequela".asFHIR()
            ),
            Coding(
                system = CodeSystem.SNOMED_CT.uri,
                version = "2023-03-01".asFHIR(),
                code = Code("33490001"),
                display = "Failed attempted abortion with fat embolism (disorder)".asFHIR()
            ),
            Coding(
                system = CodeSystem.SNOMED_CT.uri,
                version = "2023-03-01".asFHIR(),
                code = Code("442327001"),
                display = "Twin liveborn born in hospital (situation)".asFHIR()
            )
        )
        val valueSetMetadata = ValueSetMetadata(
            registryEntryType = "value-set",
            valueSetName = "RoninConditionCode",
            valueSetUuid = "201ad507-64f7-4429-810f-94bdbd51f80a",
            version = "4"
        )
        val valueSetList = ValueSetList(valueSet, valueSetMetadata)
        val json = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueSetList)
        val expectedJson = """
            {
              "codes" : [ {
                "system" : "http://hl7.org/fhir/sid/icd-10-cm",
                "version" : "2023",
                "code" : "V72.9XXD",
                "display" : "Unspecified occupant = bus injured in collision with two- or three-wheeled motor vehicle in traffic accident, subsequent encounter"
              }, {
                "system" : "http://snomed.info/sct",
                "version" : "2023-03-01",
                "code" : "2065009",
                "display" : "Dominant hereditary optic atrophy (disorder)"
              }, {
                "system" : "http://snomed.info/sct",
                "version" : "2023-03-01",
                "code" : "66424002",
                "display" : "Manual reduction = closed fracture = acetabulum and skeletal traction (procedure)"
              }, {
                "system" : "http://hl7.org/fhir/sid/icd-10-cm",
                "version" : "2023",
                "code" : "T22.361S",
                "display" : "Burn = third degree = right scapular region, sequela"
              }, {
                "system" : "http://snomed.info/sct",
                "version" : "2023-03-01",
                "code" : "33490001",
                "display" : "Failed attempted abortion with fat embolism (disorder)"
              }, {
                "system" : "http://snomed.info/sct",
                "version" : "2023-03-01",
                "code" : "442327001",
                "display" : "Twin liveborn born in hospital (situation)"
              } ],
              "metadata" : {
                "registryEntryType" : "value-set",
                "valueSetName" : "RoninConditionCode",
                "valueSetUuid" : "201ad507-64f7-4429-810f-94bdbd51f80a",
                "version" : "4"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
        val deserializedValueSet = JacksonManager.objectMapper.readValue<ValueSetList>(json)
        assertEquals(deserializedValueSet, valueSetList)
    }
}
