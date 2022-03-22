package com.projectronin.interop.ehr.transform

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Defines a Transformer capable of converting EHR [Patient]s into [OncologyPatient]s.
 */
interface PatientTransformer {
    /**
     * Transforms the [Patient] into an [OncologyPatient] based on the [tenant]. If the transformation
     * can not be completed due to missing or incomplete information, null will be returned.
     */
    fun transformPatient(patient: Patient, tenant: Tenant): OncologyPatient?

    /**
     * Transforms the [bundle] into a List of [OncologyPatient]s based on the [tenant]. Only [Patient]s that
     * could be transformed successfully will be included in the response.
     */
    fun transformPatients(bundle: Bundle<Patient>, tenant: Tenant): List<OncologyPatient>
}
