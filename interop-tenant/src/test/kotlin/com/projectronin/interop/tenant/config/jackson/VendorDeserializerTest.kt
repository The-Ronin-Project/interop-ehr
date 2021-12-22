package com.projectronin.interop.tenant.config.jackson

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.projectronin.interop.tenant.config.jackson.JacksonManager.Companion.yamlMapper
import com.projectronin.interop.tenant.config.model.vendor.Vendor
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VendorDeserializerTest {
    @Test
    fun `ensure jackson manager can be created`() {
        assertNotNull(JacksonManager())
    }

    @Test
    fun `ensure valid vendor type require`() {
        val yaml = """
            |type: "INVALID"
        """.trimMargin()

        assertThrows<InvalidTypeIdException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure client id require`() {
        val yaml = """
            |type: "EPIC"
            |serviceEndpoint: "https://example.com"
            |authenticationConfig:
            |  publicKey: "pubkey"
            |  privateKey: "privkey"
            |release: "1.0"
            |ehrUserId: "1"
            |messageType: "Message Report"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure service endpoint required`() {
        val yaml = """
            |type: "EPIC"
            |clientId: "101"
            |authenticationConfig:
            |  publicKey: "pubkey"
            |  privateKey: "privkey"
            |release: "1.0"
            |ehrUserId: "1"
            |messageType: "Message Report"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure authentication config is required`() {
        val yaml = """
            |type: "EPIC"
            |clientId: "101"
            |serviceEndpoint: "https://example.com"
            |release: "1.0"
            |ehrUserId: "1"
            |messageType: "Message Report"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure release is required`() {
        val yaml = """
            |type: "EPIC"
            |clientId: "101"
            |serviceEndpoint: "https://example.com"
            |authenticationConfig:
            |  publicKey: "pubkey"
            |  privateKey: "privkey"
            |ehrUserId: "1"
            |messageType: "Message Report"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure ehr user id is required`() {
        val yaml = """
            |type: "EPIC"
            |clientId: "101"
            |serviceEndpoint: "https://example.com"
            |authenticationConfig:
            |  publicKey: "pubkey"
            |  privateKey: "privkey"
            |release: "1.0"
            |messageType: "Message Report"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }

    @Test
    fun `ensure message type is required`() {
        val yaml = """
            |type: "EPIC"
            |clientId: "101"
            |serviceEndpoint: "https://example.com"
            |authenticationConfig:
            |  publicKey: "pubkey"
            |  privateKey: "privkey"
            |release: "1.0"
            |ehrUserId: "1"
        """.trimMargin()

        assertThrows<MissingKotlinParameterException> {
            yamlMapper.readValue(yaml, Vendor::class.java)
        }
    }
}
