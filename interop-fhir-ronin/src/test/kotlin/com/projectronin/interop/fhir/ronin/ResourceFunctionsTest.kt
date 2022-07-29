package com.projectronin.interop.fhir.ronin

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ResourceFunctionsTest {
    @Test
    fun validates() {
        val validator = mockk<ProfileValidator<SampleResource>>(relaxed = true)

        val resource = SampleResource()
        resource.validateAs(validator)

        verify { validator.validate(resource) }
    }

    @Test
    fun transforms() {
        val resource = SampleResource()
        val transformedResource = SampleResource(id = Id("1234"))

        val tenant = mockk<Tenant>()
        val transformer = mockk<ProfileTransformer<SampleResource>> {
            every { transform(resource, tenant) } returns transformedResource
        }

        val response = resource.transformTo(transformer, tenant)
        Assertions.assertEquals(transformedResource, response)

        verify { transformer.transform(resource, tenant) }
    }

    private data class SampleResource(
        override val id: Id? = null,
        override val implicitRules: Uri? = null,
        override val language: Code? = null,
        override val meta: Meta? = null,
        override val resourceType: String = "Sample"
    ) : Resource<SampleResource>
}
