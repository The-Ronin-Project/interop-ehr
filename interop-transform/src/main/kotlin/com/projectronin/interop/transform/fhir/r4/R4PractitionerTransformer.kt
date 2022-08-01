package com.projectronin.interop.transform.fhir.r4

import com.projectronin.interop.ehr.model.Bundle
import com.projectronin.interop.ehr.model.Practitioner
import com.projectronin.interop.ehr.model.enums.DataSource
import com.projectronin.interop.ehr.transform.PractitionerTransformer
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.fhir.validate.validateAndAlert
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.transform.fhir.r4.util.localize
import com.projectronin.interop.transform.util.toFhirIdentifier
import mu.KotlinLogging
import org.springframework.stereotype.Component
import com.projectronin.interop.fhir.r4.resource.Practitioner as R4Practitioner

/**
 * Implementation of [PractitionerTransformer] suitable for all R4 FHIR Practitioners
 */
@Component
class R4PractitionerTransformer : PractitionerTransformer {
    private val logger = KotlinLogging.logger { }

    override fun transformPractitioners(
        bundle: Bundle<Practitioner>,
        tenant: Tenant
    ): List<OncologyPractitioner> {
        require(bundle.dataSource == DataSource.FHIR_R4) { "Bundle is not an R4 FHIR resource" }

        return bundle.transformResources(tenant, this::transformPractitioner)
    }

    override fun transformPractitioner(practitioner: Practitioner, tenant: Tenant): OncologyPractitioner? {
        require(practitioner.dataSource == DataSource.FHIR_R4) { "Practitioner is not an R4 FHIR resource" }

        val r4Practitioner = practitioner.resource as R4Practitioner

        val fhirStu3IdIdentifier = Identifier(
            value = r4Practitioner.id?.value,
            system = CodeSystem.FHIR_STU3_ID.uri,
            type = CodeableConcepts.FHIR_STU3_ID
        )

        val oncologyPractitioner = OncologyPractitioner(
            id = r4Practitioner.id?.localize(tenant),
            meta = r4Practitioner.meta?.localize(tenant),
            implicitRules = r4Practitioner.implicitRules,
            language = r4Practitioner.language,
            text = r4Practitioner.text?.localize(tenant),
            contained = r4Practitioner.contained,
            extension = r4Practitioner.extension.map { it.localize(tenant) },
            modifierExtension = r4Practitioner.modifierExtension.map { it.localize(tenant) },
            identifier = r4Practitioner.identifier.map { it.localize(tenant) } + tenant.toFhirIdentifier() + fhirStu3IdIdentifier,
            active = r4Practitioner.active,
            name = r4Practitioner.name.map { it.localize(tenant) },
            telecom = r4Practitioner.telecom.map { it.localize(tenant) },
            address = r4Practitioner.address.map { it.localize(tenant) },
            gender = r4Practitioner.gender,
            birthDate = r4Practitioner.birthDate,
            photo = r4Practitioner.photo.map { it.localize(tenant) },
            qualification = r4Practitioner.qualification.map { it.localize(tenant) },
            communication = r4Practitioner.communication.map { it.localize(tenant) }
        )

        return try {
            validateAndAlert {
                notNull(r4Practitioner.id) { "no FHIR id" }

                merge(oncologyPractitioner.validate())
            }

            oncologyPractitioner
        } catch (e: Exception) {
            logger.error(e) { "Unable to transform practitioner" }
            null
        }
    }
}
