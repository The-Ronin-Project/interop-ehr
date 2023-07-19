package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.DocumentReferenceService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to DocumentReferences within Epic.
 */
@Component
class EpicDocumentReferenceService(epicClient: EpicClient) : DocumentReferenceService,
    EpicFHIRService<DocumentReference>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/DocumentReference"
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
            "category" to "clinical-note,imaging-result",
            "date" to listOf("ge$startDate", "le$endDate")
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
