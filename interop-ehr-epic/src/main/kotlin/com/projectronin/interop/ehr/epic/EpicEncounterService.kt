package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.client.RepeatingParameter
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Encounters within Epic.
 */
@Component
class EpicEncounterService(epicClient: EpicClient) : EncounterService, EpicFHIRService<Encounter>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/Encounter"
    override val fhirResourceType = Encounter::class.java

    @Trace
    override fun findPatientEncounters(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ):
        List<Encounter> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "date" to RepeatingParameter(listOf("ge$startDate", "le$endDate"))
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
