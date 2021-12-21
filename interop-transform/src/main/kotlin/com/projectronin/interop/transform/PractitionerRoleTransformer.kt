package com.projectronin.interop.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.PractitionerRole
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitionerRole
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines a Transformer capable of converting EHR [PractitionerRole]s into [OncologyPractitionerRole]s.
 */
interface PractitionerRoleTransformer {
    /**
     * Transforms the [practitionerRole] into an [OncologyPractitionerRole] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformPractitionerRole(practitionerRole: PractitionerRole, tenant: Tenant): OncologyPractitionerRole?

    /**
     * Transforms the [bundle] into a List of [OncologyPractitionerRole]s based on the [tenant]. Only [PractitionerRole]s
     * that could be transformed successfully will be included in the response.
     */
    fun transformPractitionerRoles(bundle: Bundle<PractitionerRole>, tenant: Tenant): List<OncologyPractitionerRole>
}
