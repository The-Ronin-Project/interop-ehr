package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.CernerTenantDAO
import com.projectronin.interop.tenant.config.data.EHRTenantDAO
import com.projectronin.interop.tenant.config.data.EpicTenantDAO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import org.springframework.stereotype.Repository

/**
 * Factory responsible for returning the right DAO for manipulating vendor specific data
 */
@Repository
class EHRTenantDAOFactory(
    private val epicTenantDAO: EpicTenantDAO,
    private val cernerTenantDAO: CernerTenantDAO
) {
    fun getEHRTenantDAO(tenantDO: TenantDO): EHRTenantDAO {
        return getEHRTenantDAOByVendorType(tenantDO.ehr.vendorType)
    }

    fun getEHRTenantDAOByVendorType(vendorType: VendorType): EHRTenantDAO {
        return when (vendorType) {
            VendorType.EPIC -> epicTenantDAO
            VendorType.CERNER -> cernerTenantDAO
        }
    }
}
