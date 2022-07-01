package com.projectronin.interop.ehr.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Observation
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyObservation
import com.projectronin.interop.tenant.config.model.Tenant

interface ObservationTransformer {
    /**
     * Transforms the EHR [Observation] into a Ronin [OncologyObservation] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformObservation(observation: Observation, tenant: Tenant): OncologyObservation?

    /**
     * Transforms the [bundle] into a List of Ronin [OncologyObservation]s based on the [tenant]. Only [Observation]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformObservations(bundle: Bundle<Observation>, tenant: Tenant): List<OncologyObservation>
}
