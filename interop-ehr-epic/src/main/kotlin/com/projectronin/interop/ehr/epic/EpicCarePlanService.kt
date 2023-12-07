package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.CarePlanService
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to CarePlans within Epic.
 */
@Component
class EpicCarePlanService(epicClient: EpicClient) : CarePlanService, EpicFHIRService<CarePlan>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/CarePlan"
    override val fhirResourceType = CarePlan::class.java

    @Trace
    override fun findPatientCarePlans(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CarePlan> {
        val parameters =
            mapOf(
                "patient" to patientFhirId,
                // hard-coded: https://vendorservices.epic.com/Sandbox/Index?api=10074
                "category" to "736378000",
                // Epic doesn't support date
            )
        return getResourceListFromSearch(tenant, parameters)
    }
}
