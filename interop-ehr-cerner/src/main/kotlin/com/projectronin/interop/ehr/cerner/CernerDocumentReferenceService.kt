package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.DocumentReferenceService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to DocumentReferences within Cerner.
 */
@Component
class CernerDocumentReferenceService(
    cernerClient: CernerClient,
    @Value("\${cerner.docref.category:LP29684-5,LP29708-2,LP75011-4,LP7819-8,LP7839-6,clinical-note}")
    categoryString: String,
    @Value("\${cerner.batch.docref:0}")
    private val batchOverride: Int = 0,
) : DocumentReferenceService, CernerFHIRService<DocumentReference>(cernerClient) {
    override val fhirURLSearchPart = "/DocumentReference"
    override val fhirResourceType = DocumentReference::class.java

    private val categories: List<String> = if (categoryString.isBlank()) emptyList() else categoryString.split(",")

    @Trace
    override fun findPatientDocuments(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DocumentReference> {
        val categoryParameters = if (categories.isNotEmpty()) mapOf("category" to categories) else emptyMap()
        val batchParameters = if (batchOverride > 0) mapOf("_count" to batchOverride) else emptyMap()
        val parameters =
            mapOf(
                "patient" to patientFhirId,
                "date" to getDateParam(startDate, endDate, tenant),
            ) + categoryParameters + batchParameters
        return getResourceListFromSearch(tenant, parameters)
    }
}
