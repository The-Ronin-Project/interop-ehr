package com.projectronin.interop.ehr.epic.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class EpicAuthenticationTest {
    @Test
    fun `ensure correct json serialization formatting`() {
        val json = """{
          |  "access_token": "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
          |  "token_type": "bearer",
          |  "expires_in": 3600,
          |  "scope": "Patient.read Patient.search"
          |}
        """.trimMargin()

        val actualAuthentication = jacksonObjectMapper().readValue(json, EpicAuthentication::class.java)

        assertEquals(
            "i82fGhXNxmidCt0OdjYttm2x0cOKU1ZbN6Y_-zBvt2kw3xn-MY3gY4lOXPee6iKPw3JncYBT1Y-kdPpBYl-lsmUlA4x5dUVC1qbjEi1OHfe_Oa-VRUAeabnMLjYgKI7b",
            actualAuthentication.accessToken
        )
        assertEquals("bearer", actualAuthentication.tokenType)
        assertEquals("Patient.read Patient.search", actualAuthentication.scope)
        assertNull(actualAuthentication.refreshToken)

        // Generally validate the expiresAt as best as we can. So for this, we're actually going to subtract an extra second to help prevent very fast tests from failing.
        assertTrue(Instant.now().isAfter(actualAuthentication.expiresAt?.minusSeconds(3601)))

        // Want some gap here, so using 5 seconds which should be way more than enough for this simple example
        assertTrue(Instant.now().isBefore(actualAuthentication.expiresAt?.minusSeconds(3595)))
    }

    @Test
    fun `ensure toString is overwritten`() {
        val epicAuthentication = EpicAuthentication(
            accessToken = "123",
            tokenType = "test token",
            expiresIn = 456,
            scope = "test scope"
        )
        assertEquals("EpicAuthentication", epicAuthentication.toString())
    }
}
