package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.http.FhirJson
import com.projectronin.interop.ehr.BinaryService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.http.ContentType
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * Service providing access to [Binary]s within Epic.
 */
@Component
class EpicBinaryService(epicClient: EpicClient) : BinaryService, EpicFHIRService<Binary>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Binary"
    override val fhirResourceType = Binary::class.java

    // Binary requires a different Accept header
    override fun getByID(tenant: Tenant, resourceFHIRId: String): Binary {
        return runBlocking {
            epicClient.get(
                tenant = tenant,
                urlPart = "$fhirURLSearchPart/$resourceFHIRId",
                acceptTypeOverride = ContentType.Application.FhirJson,
                disableRetry = true
            ).body(TypeInfo(fhirResourceType.kotlin, fhirResourceType))
        }
    }
}
