package com.projectronin.interop.ehr.hl7

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.tenant.config.data.TenantMDMConfigDAO
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import org.springframework.stereotype.Component

/***
 * This class handles decision points generating an MDM Message. Note that this only works for Epic tenants
 */
@Component
class MDMConfigService(
    private val tenantMdmConfigDAO: TenantMDMConfigDAO,
) {
    fun getIdentifiersToSend(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): List<Identifier> {
        val identifier =
            identifiers.firstOrNull { it.system?.value == CodeSystem.RONIN_MRN.uri.value }
                ?: identifiers.firstOrNull { it.system?.value == tenant.vendorAs<Epic>().patientMRNSystem }
                ?: throw VendorIdentifierNotFoundException("Failed to find either a Ronin or Patient MRN on Patient")
        return listOf(identifier)
    }

    fun getPractitionerIdentifierToSend(
        tenant: Tenant,
        identifiers: List<Identifier>,
    ): Identifier? {
        val system = tenantMdmConfigDAO.getByTenantMnemonic(tenant.mnemonic)?.providerIdentifierSystem
        return system?.let { identifiers.firstOrNull { it.system?.value == system } }
    }

    fun getDocumentTypeID(tenant: Tenant): String? =
        tenantMdmConfigDAO.getByTenantMnemonic(tenantMnemonic = tenant.mnemonic)?.mdmDocumentTypeID

    fun getReceivingApplication(tenant: Tenant): String? =
        tenantMdmConfigDAO.getByTenantMnemonic(tenantMnemonic = tenant.mnemonic)?.receivingSystem
}
