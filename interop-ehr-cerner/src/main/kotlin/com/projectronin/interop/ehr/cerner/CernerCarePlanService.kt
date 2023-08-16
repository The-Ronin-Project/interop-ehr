package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.CarePlanService
import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.tenant.config.model.Tenant
import datadog.trace.api.Trace
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Service providing access to CarePlans within Cerner.
 */
@Component
class CernerCarePlanService(cernerClient: CernerClient) : CarePlanService, CernerFHIRService<CarePlan>(cernerClient) {
    override val fhirURLSearchPart = "/CarePlan"
    override val fhirResourceType = CarePlan::class.java

    @Trace
    override fun findPatientCarePlans(
        tenant: Tenant,
        patientFhirId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ):
        List<CarePlan> {
        val parameters = mapOf(
            "patient" to patientFhirId,
            "category" to "assess-plan", // hard-coded: https://fhir.cerner.com/millennium/r4/clinical/care-provision/care-plan/#:~:text=The-,category,-parameter
            "date" to getAltDateParam(startDate, endDate, tenant)
        )
        return getResourceListFromSearch(tenant, parameters)
    }
}
