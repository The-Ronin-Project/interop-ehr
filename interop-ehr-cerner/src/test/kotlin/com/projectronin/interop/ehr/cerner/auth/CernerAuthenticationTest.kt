package com.projectronin.interop.ehr.cerner.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CernerAuthenticationTest {
    @Test
    fun `can be serialized and deserialized`() {
        val json =
            """
            |{
            |    "access_token": "eyJraWQiOiIyMDIyLTEwLTExVDIxOjAwOjI0LjU2OC5lYyIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2In0.eyJpc3MiOiJodHRwczpcL1wvYXV0aG9yaXphdGlvbi5jZXJuZXIuY29tXC8iLCJleHAiOjE2NjU2ODIyODEsImlhdCI6MTY2NTY4MTY4MSwianRpIjoiZDFiNjIxOWYtOGJmZi00ZWQ2LWJmN2MtYzZkNmEyMTAxNWJhIiwidXJuOmNlcm5lcjphdXRob3JpemF0aW9uOmNsYWltczp2ZXJzaW9uOjEiOnsidmVyIjoiMS4wIiwicHJvZmlsZXMiOnsic21hcnQtdjEiOnsiYXpzIjoic3lzdGVtXC9PYnNlcnZhdGlvbi5yZWFkIn19LCJjbGllbnQiOnsibmFtZSI6IlJvbmluIiwiaWQiOiI3OWU3OGYzZC1iYjEzLTQyYWMtOGEzOC03M2U3ZTBmMTVjZGQifSwidGVuYW50IjoiZWMyNDU4ZjItMWUyNC00MWM4LWI3MWItMGU3MDFhZjc1ODNkIn19.N5CEzEGpwOCFU8oxNJRCHFa_zZ0i8vvFjwwoS2TQpEFNv-tMmp4P-jIiAu2XCessmD7yJhloO6LvUTL4FRn3Dw",
            |    "scope": "system/Observation.read",
            |    "token_type": "Bearer",
            |    "expires_in": 570
            |}
            """.trimMargin()
        val authentication = jacksonObjectMapper().readValue(json, CernerAuthentication::class.java)

        assertEquals("Bearer", authentication.tokenType)
        assertEquals("system/Observation.read", authentication.scope)
        assertNull(authentication.refreshToken)

        @Suppress("ktlint:standard:max-line-length")
        assertEquals(
            "eyJraWQiOiIyMDIyLTEwLTExVDIxOjAwOjI0LjU2OC5lYyIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2In0.eyJpc3MiOiJodHRwczpcL1wvYXV0aG9yaXphdGlvbi5jZXJuZXIuY29tXC8iLCJleHAiOjE2NjU2ODIyODEsImlhdCI6MTY2NTY4MTY4MSwianRpIjoiZDFiNjIxOWYtOGJmZi00ZWQ2LWJmN2MtYzZkNmEyMTAxNWJhIiwidXJuOmNlcm5lcjphdXRob3JpemF0aW9uOmNsYWltczp2ZXJzaW9uOjEiOnsidmVyIjoiMS4wIiwicHJvZmlsZXMiOnsic21hcnQtdjEiOnsiYXpzIjoic3lzdGVtXC9PYnNlcnZhdGlvbi5yZWFkIn19LCJjbGllbnQiOnsibmFtZSI6IlJvbmluIiwiaWQiOiI3OWU3OGYzZC1iYjEzLTQyYWMtOGEzOC03M2U3ZTBmMTVjZGQifSwidGVuYW50IjoiZWMyNDU4ZjItMWUyNC00MWM4LWI3MWItMGU3MDFhZjc1ODNkIn19.N5CEzEGpwOCFU8oxNJRCHFa_zZ0i8vvFjwwoS2TQpEFNv-tMmp4P-jIiAu2XCessmD7yJhloO6LvUTL4FRn3Dw",
            authentication.accessToken,
        )
        // Generally validate the expiresAt as best as we can. So for this, we're actually going to subtract an extra second to help prevent very fast tests from failing.
        assertTrue(Instant.now().isAfter(authentication.expiresAt?.minusSeconds(571)))

        // Want some gap here, so using 5 seconds which should be way more than enough for this simple example
        assertTrue(Instant.now().isBefore(authentication.expiresAt?.minusSeconds(565)))
    }

    @Test
    fun `ensure toString is overwritten`() {
        val cernerAuthentication =
            CernerAuthentication(
                accessToken = "123",
                tokenType = "test token",
                expiresIn = 456,
                scope = "test scope",
                refreshToken = null,
            )
        assertEquals("CernerAuthentication", cernerAuthentication.toString())
    }
}
