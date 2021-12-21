package com.projectronin.interop.transform.fhir.r4

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.fhir.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.PatientTransformer
import com.projectronin.interop.transform.PractitionerTransformer
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Patient as R4Patient

/**
 * Implementation of [PractitionerTransformer] suitable for all R4 FHIR Practitioners
 */
@Component
class R4PatientTransformer : PatientTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformPatients(
        bundle: Bundle<Patient>,
        tenant: Tenant
    ): List<OncologyPatient> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformPatient)
    }

    override fun transformPatient(patient: Patient, tenant: Tenant): OncologyPatient? {
        require(patient.dataSource == DataSource.FHIR_R4) { "Patient is not an R4 FHIR resource" }

        val r4Patient = try {
            JacksonManager.objectMapper.readValue<R4Patient>(patient.raw)
        } catch (e: Exception) {
            logger.warn { "Unable to read R4 Patient: ${e.message}" }
            return null
        }

        val id = r4Patient.id
        if (id == null) {
            logger.warn { "Unable to transform Patient due to missing ID" }
            return null
        }

        val gender = r4Patient.gender
        if (gender == null) {
            logger.warn { "Unable to transform Patient due to missing gender" }
            return null
        }

        val birthDate = r4Patient.birthDate
        if (birthDate == null) {
            logger.warn { "Unable to transform Patient due to missing birthDate" }
            return null
        }

        val maritalStatus = r4Patient.maritalStatus
        if (maritalStatus == null) {
            logger.warn { "Unable to transform Patient due to missing maritalStatus" }
            return null
        }

        try {
            return OncologyPatient(
                id = id.localize(tenant),
                meta = r4Patient.meta?.localize(tenant),
                implicitRules = r4Patient.implicitRules,
                language = r4Patient.language,
                text = r4Patient.text?.localize(tenant),
                contained = r4Patient.contained,
                extension = r4Patient.extension.map { it.localize(tenant) },
                modifierExtension = r4Patient.modifierExtension.map { it.localize(tenant) },
                identifier = r4Patient.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier(),
                active = r4Patient.active,
                name = r4Patient.name.map { it.localize(tenant) },
                telecom = r4Patient.telecom.map { it.localize(tenant) },
                gender = gender,
                birthDate = birthDate,
                deceased = r4Patient.deceased,
                address = r4Patient.address.map { it.localize(tenant) },
                maritalStatus = maritalStatus,
                multipleBirth = r4Patient.multipleBirth,
                photo = r4Patient.photo.map { it.localize(tenant) },
                contact = r4Patient.contact.map { it.localize(tenant) },
                communication = r4Patient.communication.map { it.localize(tenant) },
                generalPractitioner = r4Patient.generalPractitioner.map { it.localize(tenant) },
                managingOrganization = r4Patient.managingOrganization?.localize(tenant),
                link = r4Patient.link.map { it.localize(tenant) }
            )
        } catch (e: Exception) {
            logger.warn { "Unable to transform Patient: ${e.message}" }
            return null
        }
    }
}
