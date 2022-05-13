package com.projectronin.interop.ehr.epic

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.epic.model.EpicIDType
import com.projectronin.interop.ehr.epic.model.inbound.StandardizedIdentifierDeserializer
import com.projectronin.interop.ehr.epic.model.outbound.StandardizedIdentifierSerializer
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

    override fun getMRNIdentifier(
        tenant: Tenant,
        identifiers: List<R4Identifier>
    ): R4Identifier {
        val system = Uri(tenant.vendorAs<Epic>().mrnSystem)
        return identifiers.firstOrNull { it.system == system } ?: throw VendorIdentifierNotFoundException("No MRN identifier found for Patient")
    }

    private fun getEpicIdentifier(
        identifiers: List<Identifier>,
        system: String?,
        exceptionMessage: () -> String
    ): Identifier {
        // currently, we're only ever using EpicIDTypes, and the logic below sort of assumes this
        // but this check ensures these objects are, so we can serialize / deserialize appropriately
        if (identifiers.any { it !is EpicIDType }) throw IllegalArgumentException("getEpicIdentifiers only works for EpicIDType")
        val identifier = identifiers.firstOrNull { it.type?.text.equals("external", true) }
            ?: identifiers.firstOrNull { it.type?.text.equals("internal", true) }
        return identifier?.let {
            StandardizedIdentifier(system ?: it.system, it)
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

    @JsonSerialize(using = StandardizedIdentifierSerializer::class)
    @JsonDeserialize(using = StandardizedIdentifierDeserializer::class)
    data class StandardizedIdentifier(
        override val system: String?,
        private val sourceIdentifier: Identifier
    ) : Identifier {
        override val value: String = sourceIdentifier.value
        override val type: CodeableConcept? = sourceIdentifier.type
        override val raw: String = sourceIdentifier.raw
        override val element: Any = sourceIdentifier.element
    }
}
