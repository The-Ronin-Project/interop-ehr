package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's patient service.
 */
interface PatientService {
    /**
     * Finds the Bundle of patients associated to the requested
     * [tenant], [birthDate], [givenName], and [familyName] from an EHR tenant.
     */
    fun findPatient(tenant: Tenant, birthDate: LocalDate, givenName: String, familyName: String): Bundle<Patient>

    /**
     * Finds the patient associated to any of the requested vendor's [patientIdsByKey] for a particular EHR [tenant].
     * Will return a map of supplied key, of type [K], to [Patient]s when found, otherwise no entry for that key.
     */
    fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient>
}
