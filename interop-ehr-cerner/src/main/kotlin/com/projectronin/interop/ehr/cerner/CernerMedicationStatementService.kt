package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Service providing access to Medication Statement within Cerner
 */
@Component
class CernerMedicationStatementService(
    cernerClient: CernerClient
) : MedicationStatementService, CernerFHIRService<MedicationStatement>(cernerClient) {
    override val fhirURLSearchPart = "/MedicationStatement"
    override val fhirResourceType = MedicationStatement::class.java

    override fun getMedicationStatementsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String
    ): List<MedicationStatement> {
        var parameters = mapOf(
            "patient" to patientFHIRId
        )

        return getResourceListFromSearch(tenant, parameters)
    }
}
