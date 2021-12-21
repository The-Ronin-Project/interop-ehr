package com.projectronin.interop.ehr.epic.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicLinkTest {
    @Test
    fun `can build from JSON`() {
        val json = """
            |{
            |  "relation": "self",
            |  "url": "https://test.com"
            |}""".trimMargin()

        val link = EpicLink(json)
        assertEquals(json, link.raw)
        assertEquals("self", link.relation)
        assertEquals("https://test.com", link.url.value)
    }
}
