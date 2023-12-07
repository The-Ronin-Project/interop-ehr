package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.normalization.NormalizationRegistryClient
import com.projectronin.interop.fhir.ronin.resource.RoninServiceRequest
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoninServiceRequestGeneratorTest {
    private lateinit var roninServiceRequest: RoninServiceRequest
    private lateinit var registry: NormalizationRegistryClient
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @BeforeEach
    fun setup() {
        registry = mockk()
        val normalizer: Normalizer =
            mockk {
                every { normalize(any(), tenant) } answers { firstArg() }
            }
        val localizer: Localizer =
            mockk {
                every { localize(any(), tenant) } answers { firstArg() }
            }
        roninServiceRequest =
            RoninServiceRequest(registry, normalizer, localizer)
    }

    @Test
    fun `example use for roninServiceRequest`() {
        // create serviceRequest resource with attributes you need, provide the tenant
        val roninServiceRequest =
            rcdmServiceRequest("test") {
                // to test an attribute like status - provide the value
                status of Code("testing-this-status")
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes will be generated
        val roninServiceRequestJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(roninServiceRequest)

        // Uncomment to take a peek at the JSON
        // println(roninServiceRequestJSON)
        assertNotNull(roninServiceRequestJSON)
    }

    @Test
    fun `validates rcdm service request`() {
        val serviceRequest = rcdmServiceRequest("test") {}
        val validation = roninServiceRequest.validate(serviceRequest, null).hasErrors()
        println(roninServiceRequest.validate(serviceRequest, null).issues())
        assertFalse(validation)
    }

    @Test
    fun `validates with identifier added`() {
        val serviceRequest =
            rcdmServiceRequest("test") {
                identifier of listOf(Identifier(id = "ID-Id".asFHIR()))
            }
        val validation = roninServiceRequest.validate(serviceRequest, null).hasErrors()
        assertEquals(false, validation)
        assertEquals(4, serviceRequest.identifier.size)
        val ids = serviceRequest.identifier.map { it.id }.toSet()
        assertTrue(ids.contains("ID-Id".asFHIR()))
    }

    @Test
    fun `generates rcdmServiceRequest with given status but fails validation because status is bad`() {
        val serviceRequest =
            rcdmServiceRequest("test") {
                status of Code("this is a bad status")
            }

        assertEquals(Code("this is a bad status"), serviceRequest.status)

        // validate should fail
        val validation = roninServiceRequest.validate(serviceRequest, null)
        assertTrue(validation.hasErrors())
        assertEquals("INV_VALUE_SET", validation.issues()[0].code)
        assertEquals(
            "'this is a bad status' is outside of required value set",
            validation.issues()[0].description,
        )
        assertEquals(
            LocationContext(element = "ServiceRequest", field = "status"),
            validation.issues()[0].location,
        )
    }

    @Test
    fun `rcdmPatient rcdmServiceRequest validates`() {
        val rcdmPatient = rcdmPatient("test") {}
        val serviceRequest = rcdmPatient.rcdmServiceRequest {}
        assertEquals("Patient/${rcdmPatient.id?.value}", serviceRequest.subject?.reference?.value)
        val validation = roninServiceRequest.validate(serviceRequest, null)
        assertEquals(false, validation.hasErrors())
    }
}
