package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.EncounterService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.util.toListOfType
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to Encounters within Epic.
 */
@Component
class EpicEncounterService(epicClient: EpicClient) : EncounterService, EpicPagingService(epicClient) {
    private val encounterSearchUrlPart = "/api/FHIR/R4/Encounter"

    override fun findPatientEncounters(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ):
        List<Encounter> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "date" to listOf("ge$startDate", "le$endDate")
        )
        return getBundleWithPaging(tenant, encounterSearchUrlPart, parameters).toListOfType()
    }
}
