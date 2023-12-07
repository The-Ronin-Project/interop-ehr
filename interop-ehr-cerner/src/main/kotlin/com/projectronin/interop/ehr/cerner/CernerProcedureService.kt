package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.ProcedureService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Procedures within Cerner.
 */
@Component
class CernerProcedureService(
    cernerClient: CernerClient,
) : ProcedureService, CernerFHIRService<Procedure>(cernerClient) {
    override val fhirURLSearchPart = "/Procedure"
    override val fhirResourceType = Procedure::class.java

    override fun getProcedureByPatient(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Procedure> {
        val parameters =
            mapOf(
                "patient" to patientFhirId,
                "date" to getAltDateParam(startDate, endDate, tenant),
            )
        return getResourceListFromSearch(tenant, parameters)
    }
}
