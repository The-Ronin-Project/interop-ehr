package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.factory.EHRFactory
import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Patient
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.transform.PatientTransformer
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Patient as R4Patient

/**
 * Implementation of [PatientTransformer] suitable for all R4 FHIR Patients
 */
@Component
class R4PatientTransformer(private val ehrFactory: EHRFactory) : PatientTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformPatients(
        bundle: Bundle<Patient>,
        tenant: Tenant,
    ): List<OncologyPatient> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformPatient)
    }

    override fun transformPatient(
        patient: Patient,
        tenant: Tenant
    ): OncologyPatient? {
        require(patient.dataSource == DataSource.FHIR_R4) { "Patient is not an R4 FHIR resource" }

        val r4Patient = patient.resource as R4Patient

        val id = r4Patient.id
        if (id == null) {
            logger.warn { "Unable to transform patient due to missing ID" }
            return null
        }

        val gender = r4Patient.gender
        if (gender == null) {
            logger.warn { "Unable to transform patient due to missing gender" }
            return null
        }

        val birthDate = r4Patient.birthDate
        if (birthDate == null) {
            // warn, but don't error
            logger.warn { "Unable to transform patient due to missing birth date" }
        }

        val maritalStatus = r4Patient.maritalStatus ?: CodeableConcept(
            coding = listOf(
                Coding(
                    system = Uri("http://terminology.hl7.org/CodeSystem/v3-NullFlavor"),
                    code = Code("NI"),
                    display = "NoInformation"
                )
            )
        )
        val fhirStu3IdIdentifier = Identifier(
            value = id.value,
            system = CodeSystem.FHIR_STU3_ID.uri,
            type = CodeableConcepts.FHIR_STU3_ID
        )
        val identifierService = ehrFactory.getVendorFactory(tenant).identifierService
        val existingMRN = try {
            identifierService.getMRNIdentifier(tenant, r4Patient.identifier)
        } catch (e: Exception) {
            logger.warn(e) { "Unable to find MRN  for patient: ${e.message}" }
            return null
        }

        val mrnIdentifier = Identifier(
            value = existingMRN.value,
            system = CodeSystem.MRN.uri,
            type = CodeableConcepts.MRN
        )

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
                identifier = r4Patient.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier() +
                    fhirStu3IdIdentifier + mrnIdentifier,
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
            logger.warn(e) { "Unable to transform patient: ${e.message}" }
            return null
        }
    }
}
