package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationAdministrationService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Encounters within Cerner.
 */
@Component
class CernerMedicationAdministrationService(cernerClient: CernerClient) : MedicationAdministrationService, CernerFHIRService<MedicationAdministration>(cernerClient) {
    override val fhirURLSearchPart = "/MedicationAdministration"
    override val fhirResourceType = MedicationAdministration::class.java

    @Trace
    override fun findMedicationAdministrationsByPatient(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<MedicationAdministration> {
        val parameters = mapOf(
            "patient" to patientFHIRId,
            "effective-time" to getDateParam(startDate, endDate, tenant)
        )
        return getResourceListFromSearch(tenant, parameters)
    }

    override fun findMedicationAdministrationsByRequest(
        tenant: Tenant,
        medicationRequest: MedicationRequest
    ): List<MedicationAdministration> {
        return listOf() // Not implemented for Cerner
    }
}
