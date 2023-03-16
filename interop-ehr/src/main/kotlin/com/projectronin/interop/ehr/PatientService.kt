package com.projectronin.interop.ehr

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * Defines the functionality for an EHR's patient service.
 */
interface PatientService : FHIRService<Patient> {
    /**
     * Finds the Bundle of patients associated to the requested
     * [tenant], [birthDate], [givenName], and [familyName] from an EHR tenant.
     */
    fun findPatient(tenant: Tenant, birthDate: LocalDate, givenName: String, familyName: String): List<Patient>

    /**
     * Finds the patient associated to any of the requested vendor's [patientIdsByKey] for a particular EHR [tenant].
     * Will return a map of supplied key, of type [K], to [Patient]s when found, otherwise no entry for that key.
     */
    fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient>

    /**
     * Returns a single [Patient] based on their FHIR ID.
     */
    fun getPatient(tenant: Tenant, patientFHIRID: String): Patient

    /**
     * Finds FHIR ID (non-localized) for a patient, based on an Identifier value. Searches Aidbox first before querying
     * the EHR. Implementations of this interface must 'know' the correct identifier system to apply to this value.
     */
    fun getPatientFHIRId(
        tenant: Tenant,
        patientIDValue: String
    ): String
}
