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
    override val fhirURLSearchPart = ""
    override val fhirResourceType = MedicationStatement::class.java

    /**
     * Cerner does not support medication statement, so we return empty objects
     */
    override fun getMedicationStatementsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String
    ): List<MedicationStatement> {
        return emptyList()
    }

    override fun getByID(
        tenant: Tenant,
        resourceFHIRId: String
    ): MedicationStatement {
        return MedicationStatement()
    }

    override fun getByIDs(
        tenant: Tenant,
        resourceFHIRIds: List<String>
    ): Map<String, MedicationStatement> {
        return emptyMap()
    }
}
