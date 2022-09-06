package com.projectronin.interop.ehr

import com.projectronin.interop.ehr.outputs.GetFHIRIDResponse
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
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
    fun findPatient(tenant: Tenant, birthDate: LocalDate, givenName: String, familyName: String): List<Patient>

    /**
     * Finds the patient associated to any of the requested vendor's [patientIdsByKey] for a particular EHR [tenant].
     * Will return a map of supplied key, of type [K], to [Patient]s when found, otherwise no entry for that key.
     */
    fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient>

    /**
     * Finds FHIR IDs (non-localized) for a list of patients, based on the Epic Identifer (MRN or Internal). Searches
     * Aidbox first before querying the EHR. If a patient is found in the EHR, this will return the [Patient] object
     * that was found to save on future queries. Returns a map of the patient's searched ID to its [GetFHIRIDResponse].
     * If the patient's FHIR ID can't be found, the patient won't be included in the map.
     */
    fun getPatientsFHIRIds(
        tenant: Tenant,
        patientIDSystem: String,
        patientIDValues: List<String>
    ): Map<String, GetFHIRIDResponse>
}
