package com.projectronin.interop.ehr.model.helper

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.ehr.model.Link
import com.projectronin.interop.ehr.model.base.FHIRBundle
import com.projectronin.interop.ehr.model.base.FHIRElement
import com.projectronin.interop.ehr.model.base.FHIRResource
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.r4.valueset.NameUse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JsonFhirHelpersTest {
    @Test
    fun `enum when field not found`() {
        val jsonObject = jsonObject("{}")
        val enum = jsonObject.enum<NameUse>("name")
        assertNull(enum)
    }

    @Test
    fun `enum when field has non valid code`() {
        val jsonObject = jsonObject("""{"name": "unknown"}""")
        val enum = jsonObject.enum<NameUse>("name")
        assertNull(enum)
    }

    @Test
    fun `enum when field has mapped code`() {
        val jsonObject = jsonObject("""{"name": "official"}""")
        val enum = jsonObject.enum<NameUse>("name")
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `fhir element list when field not found`() {
        val jsonObject = jsonObject("""{}""")
        val elements = jsonObject.fhirElementList("elements", ::Element)
        assertEquals(listOf<Element>(), elements)
    }

    @Test
    fun `fhir element list when field has no elements`() {
        val jsonObject = jsonObject("""{"elements": []}""")
        val elements = jsonObject.fhirElementList("elements", ::Element)
        assertEquals(listOf<Element>(), elements)
    }

    @Test
    fun `fhir element list when field has single element`() {
        val elementJson1 = """{"id":1,"name":"Value 1"}"""
        val jsonObject = jsonObject("""{"elements": [ $elementJson1 ]}""")
        val elements = jsonObject.fhirElementList("elements", ::Element)

        val element1 = Element(elementJson1)
        assertEquals(listOf(element1), elements)
    }

    @Test
    fun `fhir element list when field has multiple elements`() {
        val elementJson1 = """{"id":1,"name":"Value 1"}"""
        val elementJson2 = """{"id":2,"name":"Value 2"}"""
        val elementJson3 = """{"id":3,"name":"Value 3"}"""
        val jsonObject = jsonObject("""{"elements": [ $elementJson1, $elementJson2, $elementJson3 ]}""")
        val elements = jsonObject.fhirElementList("elements", ::Element)

        val element1 = Element(elementJson1)
        val element2 = Element(elementJson2)
        val element3 = Element(elementJson3)
        assertEquals(listOf(element1, element2, element3), elements)
    }

    @Test
    fun `fhir resource list when field not found`() {
        val jsonObject = jsonObject("""{}""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)
        assertEquals(listOf<Resource>(), resources)
    }

    @Test
    fun `fhir resource list when field has no elements`() {
        val jsonObject = jsonObject("""{ "resources": [] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)
        assertEquals(listOf<Resource>(), resources)
    }

    @Test
    fun `fhir resource list when field has no elements with a resource`() {
        val jsonObject = jsonObject("""{ "resources": [ { "id": 1 } ] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)
        assertEquals(listOf<Resource>(), resources)
    }

    @Test
    fun `fhir resource list when resource of wrong type`() {
        val resourceJson1 = """{"resourceType":"Unknown","id":1}"""
        val jsonObject = jsonObject("""{ "resources": [ { "resource": $resourceJson1 } ] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)
        assertEquals(listOf<Resource>(), resources)
    }

    @Test
    fun `fhir resource list when resource of correct type`() {
        val resourceJson1 = """{"resourceType":"Patient","id":1}"""
        val jsonObject = jsonObject("""{ "resources": [ { "resource": $resourceJson1 } ] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)

        val resource1 = Resource(resourceJson1)
        assertEquals(listOf(resource1), resources)
    }

    @Test
    fun `fhir resource list when resources of correct and incorrect type`() {
        val resourceJson1 = """{"resourceType":"Patient","id":1}"""
        val resourceJson2 = """{"resourceType":"Unknown","id":2}"""
        val jsonObject =
            jsonObject("""{ "resources": [ { "resource": $resourceJson1 }, { "resource": $resourceJson2 } ] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)

        val resource1 = Resource(resourceJson1)
        assertEquals(listOf(resource1), resources)
    }

    @Test
    fun `fhir resource list when multiple resources of correct type`() {
        val resourceJson1 = """{"resourceType":"Patient","id":1}"""
        val resourceJson2 = """{"resourceType":"Patient","id":2}"""
        val jsonObject =
            jsonObject("""{ "resources": [ { "resource": $resourceJson1 }, { "resource": $resourceJson2 } ] }""")
        val resources = jsonObject.fhirResourceList("resources", "Patient", ::Resource)

        val resource1 = Resource(resourceJson1)
        val resource2 = Resource(resourceJson2)
        assertEquals(listOf(resource1, resource2), resources)
    }

    @Test
    fun `enum when field has supplemental mapped code`() {
        val jsonObject = jsonObject("""{"name": "offi"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `enum when field has supplemental miss but default match`() {
        val jsonObject = jsonObject("""{"name": "official"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertEquals(NameUse.OFFICIAL, enum)
    }

    @Test
    fun `enum when field with mapping has default value`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL), NameUse.OLD)
        assertEquals(NameUse.OLD, enum)
    }

    @Test
    fun `enum when field has default value`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", NameUse.OLD)
        assertEquals(NameUse.OLD, enum)
    }

    @Test
    fun `enum when field no default`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL))
        assertNull(enum)
    }

    @Test
    fun `enum when field null default`() {
        val jsonObject = jsonObject("""{"name": "self"}""")
        val enum = jsonObject.enum("name", mapOf("offi" to NameUse.OFFICIAL), null)
        assertNull(enum)
    }

    @Test
    fun `merge bundles`() {
        val bundleJson1 = """{"entry":[{"resource":{"resourceType":"Patient","id":1}}]}"""
        val bundleJson2 = """{"entry":[{"resource":{"resourceType":"Patient","id":1}}]}"""

        val bundle1 = Bundle(bundleJson1)
        val bundle2 = Bundle(bundleJson2)

        val comboBundle = mergeBundles(bundle1, bundle2, ::Bundle)

        assertEquals(DataSource.FHIR_R4, comboBundle.dataSource)
        assertEquals(2, comboBundle.resources.size)
    }

    private fun jsonObject(json: String) = Parser.default().parse(StringBuilder(json)) as JsonObject
}

data class Element(override val raw: String) : FHIRElement(raw)

data class Resource(override val raw: String) : FHIRResource(raw) {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.PATIENT
}

data class Bundle(override val raw: String) : FHIRBundle<Resource>(raw) {
    override val dataSource: DataSource
        get() = DataSource.FHIR_R4

    override val resourceType: ResourceType
        get() = ResourceType.BUNDLE

    override val links: List<Link>
        get() = listOf()

    override val resources: List<Resource> by lazy {
        jsonObject.fhirResourceList("entry", "Patient", ::Resource)
    }
}
