package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.IdentifierService
import com.projectronin.interop.ehr.inputs.FHIRIdentifiers
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.ronin.toFhirIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Cerner
import datadog.trace.api.Trace
import org.springframework.stereotype.Service

/**
 * Cerner implementation of [IdentifierService]
 */
@Service
class CernerIdentifierService : IdentifierService {
    @Trace
    override fun getPractitionerIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        throw NotImplementedError("getPractitionerIdentifier not implemented for Cerner, as they will always use the FHIR ID")
    }

    @Trace
    override fun getPatientIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        throw NotImplementedError("getPatientIdentifier not implemented for Cerner, as they will always use the FHIR ID")
    }

    @Trace
    override fun getPractitionerProviderIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers,
    ): Identifier {
        return getValidFHIRId(identifiers)
    }

    @Trace
    override fun getPractitionerUserIdentifier(
        tenant: Tenant,
        identifiers: FHIRIdentifiers,
    ): Identifier {
        return getValidFHIRId(identifiers)
    }

    @Trace
    override fun getMRNIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        val system = Uri(tenant.vendorAs<Cerner>().patientMRNSystem)
        return identifiers.firstOrNull { it.system == system }
            ?: throw VendorIdentifierNotFoundException(
                "No MRN identifier with system '${tenant.vendorAs<Cerner>().patientMRNSystem}' found for Patient",
            )
    }

    @Trace
    override fun getLocationIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        throw NotImplementedError("getLocationIdentifier not implemented for Cerner, as they will always use the FHIR ID")
    }

    override fun getOrderIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        throw NotImplementedError("getOrderIdentifier not implemented for Cerner")
    }

    override fun getEncounterIdentifier(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier {
        throw NotImplementedError("getEncounterIdentifier not implemented for Cerner")
    }

    /**
     * Returns a valid FHIR Id as an [Identifier], or throws an exception.  An [Id] is required in [FHIRIdentifiers],
     * so it will never be null, but the value in the [Id] is nullable and might not be there.
     */
    private fun getValidFHIRId(identifiers: FHIRIdentifiers): Identifier {
        return identifiers.id.toFhirIdentifier().apply {
            this?.value ?: throw VendorIdentifierNotFoundException("No value on FHIR identifier")
        }
            // Shouldn't be possible to hit unless identifiers.id is null, which isn't allowed, or someone rewrites
            // toFhirIdentifier() to make it possible.
            ?: throw VendorIdentifierNotFoundException("No FHIR identifier found")
    }
}
