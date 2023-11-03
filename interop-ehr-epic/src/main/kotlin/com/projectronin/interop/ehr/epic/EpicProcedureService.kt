package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.ProcedureService
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Procedures within Epic.
 */
@Component
class EpicProcedureService(
    epicClient: EpicClient
) : ProcedureService, EpicFHIRService<Procedure>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Procedure"
    override val fhirResourceType = Procedure::class.java

    override fun getProcedureByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Procedure> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "date" to RepeatingParameter(listOf("ge$startDate", "le$endDate"))
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
