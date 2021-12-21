package com.projectronin.interop.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines a Transformer capable of converting EHR [Practitioner]s into [OncologyPractitioner]s.
 */
interface PractitionerTransformer {
    /**
     * Transforms the [practitioner] into an [OncologyPractitioner] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformPractitioner(practitioner: Practitioner, tenant: Tenant): OncologyPractitioner?

    /**
     * Transforms the [bundle] into a List of [OncologyPractitioner]s based on the [tenant]. Only [Practitioner]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformPractitioners(bundle: Bundle<Practitioner>, tenant: Tenant): List<OncologyPractitioner>
}
