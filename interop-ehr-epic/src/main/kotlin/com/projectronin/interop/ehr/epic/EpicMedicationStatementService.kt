package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MedicationStatementService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfSTU3Type
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.stu3.resource.STU3Bundle
import com.projectronin.interop.fhir.stu3.resource.STU3MedicationStatement
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * [Epic MedicationStatement.Search (STU3)](https://appmarket.epic.com/Sandbox?api=493)
 */
@Component
class EpicMedicationStatementService(private val epicClient: EpicClient) : MedicationStatementService {
    private val searchUrlPart = "/api/FHIR/STU3/MedicationStatement"

    /**
     * Requires a Patient FHIR ID as input. Returns a List of R4 [MedicationStatement]s.
     */
    override fun getMedicationStatementsByPatientFHIRId(
        tenant: Tenant,
        patientFHIRId: String
    ): List<MedicationStatement> {
        val parameters = mapOf("patient" to patientFHIRId)
        val medicationStatements: List<STU3MedicationStatement> = runBlocking {
            epicClient.get(tenant, searchUrlPart, parameters).body<STU3Bundle>()
        }.toListOfSTU3Type()
        return medicationStatements.map { stu3 ->
            stu3.transformToR4()
        }
    }
}
