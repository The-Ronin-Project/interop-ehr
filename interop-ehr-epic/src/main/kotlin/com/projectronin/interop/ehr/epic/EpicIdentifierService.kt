package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.springframework.stereotype.Service

/**
 * Epic implementation of [IdentifierService]
 */
@Service
class EpicIdentifierService : IdentifierService {
    override fun getPractitionerIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier {
        val system = tenant.vendorAs<Epic>().practitionerProviderSystem
        return getIdentifierByType(identifiers, listOf("internal", "external"), system) { "No identifier found for practitioner" }
    }

    override fun getPatientIdentifier(tenant: Tenant, identifiers: List<Identifier>): Identifier {
        return getIdentifierByType(
            identifiers,
            listOf("internal"),
            tenant.vendorAs<Epic>().patientInternalSystem
        ) { "No matching identifier for the patient with system ${tenant.vendorAs<Epic>().patientInternalSystem}" }
    }

    override fun getPractitionerProviderIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers
    ): Identifier {
        return getSystemIdentifier(
            tenant.vendorAs<Epic>().practitionerProviderSystem,
            identifiers.identifiers
        ) { "No practitioner provider identifier with system '${tenant.vendorAs<Epic>().practitionerProviderSystem}' found for resource with FHIR id '${identifiers.id.value}'" }
    }

    override fun getPractitionerUserIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers
    ): Identifier {
        return getSystemIdentifier(
            tenant.vendorAs<Epic>().practitionerUserSystem,
            identifiers.identifiers
        ) { "No practitioner user identifier with system '${tenant.vendorAs<Epic>().practitionerUserSystem}' found for resource with FHIR id '${identifiers.id.value}'" }
    }

    override fun getMRNIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>
    ): Identifier {
        val system = Uri(tenant.vendorAs<Epic>().patientMRNSystem)
        return identifiers.firstOrNull { it.system == system }
            ?: throw VendorIdentifierNotFoundException("No MRN identifier with system '${tenant.vendorAs<Epic>().patientMRNSystem}' found for Patient")
    }

    private fun getIdentifierByType(
        identifiers: List<Identifier>,
        typeString: List<String>,
        newSystem: String,
        exceptionMessage: () -> String
    ): Identifier {
        val identifier = typeString.firstNotNullOfOrNull { type ->
            identifiers.firstOrNull {
                it.type?.text.equals(
                    type,
                    true
                )
            }
        } ?: throw VendorIdentifierNotFoundException(exceptionMessage.invoke())
        if (identifier.system?.value != null) return identifier // preservers system if it's present
        return Identifier(
            system = Uri(value = newSystem),
            value = identifier.value,
            type = identifier.type
        )
    }

    private fun getSystemIdentifier(
        system: String,
        identifiers: List<Identifier>,
        exceptionMessage: () -> String
    ): Identifier {
        val systemUri = Uri(system)
        return identifiers.firstOrNull { it.system == systemUri && it.type?.text.equals("external", true) }
            ?: throw VendorIdentifierNotFoundException(exceptionMessage.invoke())
    }
}
