package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.ehr.inputs.IdentifierVendorIdentifier
import com.projectronin.interop.ehr.inputs.VendorIdentifier
import com.projectronin.interop.ehr.model.CodeableConcept
import com.projectronin.interop.ehr.model.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.springframework.stereotype.Service
import com.projectronin.interop.fhir.r4.datatype.Identifier as R4Identifier

/**
 * Epic implementation of [IdentifierService]
 */
@Service
class EpicIdentifierService : IdentifierService {
    override fun getPractitionerIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier {
        val system = tenant.vendorAs<Epic>().practitionerProviderSystem
        return getEpicIdentifier(identifiers, system) { "No identifier found for practitioner" }
    }

    override fun getPatientIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier {
        // TODO: Is there a system that should be used here?
        return getEpicIdentifier(identifiers, null) { "No identifier found for patient" }
    }

    override fun getPractitionerProviderIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers
    ): VendorIdentifier<out Any> {
        return getSystemIdentifier(
            tenant.vendorAs<Epic>().practitionerProviderSystem,
            identifiers.identifiers
        ) { "No practitioner provider identifier found for resource with FHIR id ${identifiers.id.value}" }
    }

    override fun getPractitionerUserIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers
    ): VendorIdentifier<out Any> {
        return getSystemIdentifier(
            tenant.vendorAs<Epic>().practitionerUserSystem,
            identifiers.identifiers
        ) { "No practitioner user identifier found for resource with FHIR id ${identifiers.id.value}" }
    }

    private fun getEpicIdentifier(
        identifiers: List<Identifier>,
        system: String?,
        exceptionMessage: () -> String
    ): Identifier {
        val identifier = identifiers.firstOrNull { it.type?.text.equals("external", true) }
            ?: identifiers.firstOrNull { it.type?.text.equals("internal", true) }
        return identifier?.let {
            StandardizedIdentifier(it.value, system ?: it.system, it.type)
        } ?: throw VendorIdentifierNotFoundException(exceptionMessage.invoke())
    }

    private fun getSystemIdentifier(
        system: String,
        identifiers: List<R4Identifier>,
        exceptionMessage: () -> String
    ): IdentifierVendorIdentifier {
        val systemUri = Uri(system)
        val identifier = identifiers.firstOrNull { it.system == systemUri && it.type?.text.equals("external", true) }
            ?: throw VendorIdentifierNotFoundException(exceptionMessage.invoke())
        return IdentifierVendorIdentifier(identifier)
    }

    data class StandardizedIdentifier(
        override val value: String,
        override val system: String? = null,
        override val type: CodeableConcept? = null
    ) : Identifier {
        override val raw: String = this.toString()
        override val element: Any = this
    }
}
