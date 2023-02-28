package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Encounters within Cerner.
 */
@Component
class CernerEncounterService(cernerClient: CernerClient) : EncounterService, CernerFHIRService<Encounter>(cernerClient) {
    override val fhirURLSearchPart = "/Encounter"
    override val fhirResourceType = Encounter::class.java

    @Trace
    override fun findPatientEncounters(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ):
        List<Encounter> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "date" to getDateParam(startDate, endDate, tenant)
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
