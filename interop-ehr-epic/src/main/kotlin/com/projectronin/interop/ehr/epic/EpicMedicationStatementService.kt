package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.stu3.resource.STU3MedicationStatement
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * [Epic MedicationStatement.Search (STU3)](https://appmarket.epic.com/Sandbox?api=493)
 */
@Component
class EpicMedicationStatementService(epicClient: EpicClient) : MedicationStatementService,
    EpicSTU3FHIRService<STU3MedicationStatement, MedicationStatement>(epicClient, STU3MedicationStatement::class.java) {
    override val fhirURLSearchPart = "/api/FHIR/STU3/MedicationStatement"
    override val fhirResourceType = MedicationStatement::class.java

    /**
     * Requires a Patient FHIR ID as input. Returns a List of R4 [MedicationStatement]s.
     */
    @Trace
    override fun getMedicationStatementsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<MedicationStatement> {
        val dateMap = startDate?.let { mapOf("date" to "ge$startDate") }
            ?: endDate?.let { mapOf("date" to "le$endDate") } ?: emptyMap()
        val parameters = mapOf("patient" to patientFHIRId) + dateMap
        return getResourceListFromSearchSTU3(tenant, parameters)
    }
}
