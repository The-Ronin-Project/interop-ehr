package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.deformat
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicLinkTest {
    @Test
    fun `can build from object`() {
        val link = BundleLink(
            relation = "self",
            url = Uri("https://test.com")
        )

        val epicLink = EpicLink(link)
        assertEquals(link, epicLink.element)
        assertEquals("self", epicLink.relation)
        assertEquals("https://test.com", epicLink.url.value)
    }

    @Test
    fun `returns JSON as raw`() {
        val link = BundleLink(
            relation = "self",
            url = Uri("https://test.com")
        )
        val json = """
            |{
            |  "relation": "self",
            |  "url": "https://test.com"
            |}""".trimMargin()

        val epicLink = EpicLink(link)
        assertEquals(link, epicLink.element)
        assertEquals(deformat(json), epicLink.raw)
    }
}
