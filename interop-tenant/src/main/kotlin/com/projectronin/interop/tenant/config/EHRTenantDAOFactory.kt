package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.EHRTenantDAO
import com.projectronin.interop.tenant.config.data.EpicTenantDAO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import org.springframework.stereotype.Repository

/**
 * Factory responsible for returning the right DAO for manipulating vendor specific data
 */
@Repository
class EHRTenantDAOFactory(private val epicTenantDAO: EpicTenantDAO) {
    fun getEHRTenantDAO(tenantDO: TenantDO): EHRTenantDAO {
        return when (tenantDO.ehr.vendorType) {
            VendorType.EPIC -> epicTenantDAO
        }
    }
}
