package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's DocumentReference service.
 */
interface DocumentReferenceService : FHIRService<DocumentReference> {
    /**
     * Finds a list of patient [DocumentReference]s for a date range.
     */
    fun findPatientDocuments(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DocumentReference>
}
