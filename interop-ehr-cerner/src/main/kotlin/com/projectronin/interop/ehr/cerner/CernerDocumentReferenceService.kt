package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.DocumentReferenceService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to DocumentReferences within Cerner.
 */
@Component
class CernerDocumentReferenceService(cernerClient: CernerClient) : DocumentReferenceService, CernerFHIRService<DocumentReference>(cernerClient) {
    override val fhirURLSearchPart = "/DocumentReference"
    override val fhirResourceType = DocumentReference::class.java

    @Trace
    override fun findPatientDocuments(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ):
        List<DocumentReference> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            // Categories are hardcoded per INFX: INT-2014
            "category" to listOf("LP29684-5", "LP29708-2", "LP75011-4", "LP7819-8", "LP7839-6", "clinical-note"),
            "date" to getDateParam(startDate, endDate, tenant)
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
