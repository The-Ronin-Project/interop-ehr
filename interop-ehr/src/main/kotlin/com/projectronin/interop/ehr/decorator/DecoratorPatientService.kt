package com.projectronin.interop.ehr.decorator

import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.PatientService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.util.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDate

/**
 * This class decorates the [PatientService] and should be used by all implementations. Any methods that return [Patient]s
 * are updated to ensure they contain the Ronin FHIR identifier and Ronin MRN identifier. While both of the identifiers
 * are important to the rest of Ronin's systems, the MRN identifier is particularly tricky for us to discover outside
 * the EHR boundaries. When this logic exists almost anywhere else, it begins to create dependency graph issues. Since
 * interop-ehr is the entry point for all access to Patients, placing this logic at this level standardizes all values
 * no matter where in the graph we are.
 */
class DecoratorPatientService(
    private val patientService: PatientService,
    private val identifierService: IdentifierService
) : PatientService {
    override val fhirResourceType: Class<Patient> by lazy { patientService.fhirResourceType }

    override fun getPatientFHIRId(tenant: Tenant, patientIDValue: String): String =
        patientService.getPatientFHIRId(tenant, patientIDValue)

    override fun findPatient(
        tenant: Tenant,
        birthDate: LocalDate,
        givenName: String,
        familyName: String
    ): List<Patient> {
        return patientService.findPatient(tenant, birthDate, givenName, familyName)
            .map { it.decorateWithCommonIds(tenant) }
    }

    override fun <K> findPatientsById(tenant: Tenant, patientIdsByKey: Map<K, Identifier>): Map<K, Patient> {
        return patientService.findPatientsById(tenant, patientIdsByKey)
            .mapValues { it.value.decorateWithCommonIds(tenant) }
    }

    override fun getPatient(tenant: Tenant, patientFHIRID: String): Patient {
        return patientService.getPatient(tenant, patientFHIRID).decorateWithCommonIds(tenant)
    }

    override fun getByID(tenant: Tenant, resourceFHIRId: String): Patient {
        return patientService.getByID(tenant, resourceFHIRId).decorateWithCommonIds(tenant)
    }

    override fun getByIDs(tenant: Tenant, resourceFHIRIds: List<String>): Map<String, Patient> {
        return patientService.getByIDs(tenant, resourceFHIRIds).mapValues { it.value.decorateWithCommonIds(tenant) }
    }

    /**
     * Decorates the provided Patient with the MRN found.
     */
    private fun Patient.decorateWithCommonIds(tenant: Tenant): Patient {
        val mrnIdentifier =
            runCatching { identifierService.getMRNIdentifier(tenant, identifier) }.getOrNull()?.let {
                listOf(
                    Identifier(
                        value = it.value,
                        system = CodeSystem.RONIN_MRN.uri,
                        type = CodeableConcepts.RONIN_MRN
                    )
                )
            } ?: emptyList()

        val fhirIdIdentifier = id.toFhirIdentifier()?.let { listOf(it) } ?: emptyList()
        return copy(identifier = identifier + fhirIdIdentifier + mrnIdentifier)
    }
}
