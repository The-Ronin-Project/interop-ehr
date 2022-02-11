package com.projectronin.interop.tenant.config.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

@LiquibaseTest(changeLog = "ehr/db/changelog/ehr.db.changelog-master.yaml")
class TenantDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `no tenants found`() {
        val dao = TenantDAO(KtormHelper.database())
        val tenant = dao.getTenantForMnemonic("fake")
        assertNull(tenant)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/Tenants.yaml"], cleanAfter = true)
    fun `tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val tenant = dao.getTenantForMnemonic("mdaoc")
        assertNotNull(tenant)
        tenant?.let {
            assertEquals(1001, tenant.id)
            assertEquals("mdaoc", tenant.mnemonic)
            assertEquals(LocalTime.of(22, 0, 0), tenant.availableBatchStart)
            assertEquals(LocalTime.of(6, 0, 0), tenant.availableBatchEnd)

            val ehr = tenant.ehr
            assertEquals(101, ehr.id)
            assertEquals(VendorType.EPIC, ehr.vendorType)
            assertEquals("12345", ehr.clientId)
            assertEquals("public", ehr.publicKey)
            assertEquals("private", ehr.privateKey)
        }
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/EHRTenants.yaml"], cleanAfter = true)
    fun `no ehr tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val ehrTenant = dao.getEHRTenant<EpicTenantDO>(1002, VendorType.EPIC)
        assertNull(ehrTenant)
    }

    @Test
    @DataSet(value = ["/dbunit/tenant/EHRTenants.yaml"], cleanAfter = true)
    fun `epic tenant found`() {
        val dao = TenantDAO(KtormHelper.database())
        val ehrTenant = dao.getEHRTenant<EpicTenantDO>(1001, VendorType.EPIC)
        assertNotNull(ehrTenant)
        ehrTenant?.let {
            assertEquals(1001, ehrTenant.tenantId)
            assertEquals("2021.10", ehrTenant.release)
            assertEquals("https://localhost:8080/", ehrTenant.serviceEndpoint)
            assertEquals("RONIN", ehrTenant.ehrUserId)
            assertEquals("Ronin Alerts", ehrTenant.messageType)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", ehrTenant.practitionerProviderSystem)
            assertEquals("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", ehrTenant.practitionerUserSystem)
        }
    }
}
