package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.ehr.BinaryService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.http.ContentType
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * Service providing access to [Binary]s within Cerner.
 */
@Component
class CernerBinaryService(cernerClient: CernerClient) : BinaryService, CernerFHIRService<Binary>(cernerClient) {
    override val fhirURLSearchPart = "/Binary"
    override val fhirResourceType = Binary::class.java

    // Binary requires a different Accept header
    override fun getByID(tenant: Tenant, resourceFHIRId: String): Binary {
        return runBlocking {
            cernerClient.get(
                tenant = tenant,
                urlPart = "$fhirURLSearchPart/$resourceFHIRId",
                acceptTypeOverride = ContentType.Application.FhirJson
            ).body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
        }
    }
}
