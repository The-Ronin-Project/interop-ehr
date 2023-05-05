package com.projectronin.interop.tenant.config

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.EhrDAO
import com.projectronin.interop.tenant.config.data.TenantCodesDAO
import com.projectronin.interop.tenant.config.data.TenantDAO
import com.projectronin.interop.tenant.config.data.model.TenantDO
import com.projectronin.interop.tenant.config.exception.NoEHRFoundException
import com.projectronin.interop.tenant.config.exception.NoTenantFoundException
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service responsible for [Tenant]s loaded from a database.
 */
@Service
class TenantService(
    private val tenantDAO: TenantDAO,
    private val ehrDAO: EhrDAO,
    private val ehrTenantDAOFactory: EHRTenantDAOFactory,
    private val tenantCodesDao: TenantCodesDAO
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Retrieves the [Tenant] for the supplied [mnemonic]. If none exists, null will be returned.
     */
    fun getTenantForMnemonic(mnemonic: String): Tenant? {
        logger.info { "Retrieving tenant for mnemonic : $mnemonic" }
        val tenantDO = tenantDAO.getTenantForMnemonic(mnemonic) ?: return null
        logger.debug { "Found tenant for mnemonic : $mnemonic" }
        val ehrDO = tenantDO.ehr
        val ehrDAO = ehrTenantDAOFactory.getEHRTenantDAO(tenantDO)

        val ehrTenantDO = ehrDAO.getByTenantMnemonic(tenantDO.mnemonic) ?: return null
        logger.debug { "Found EhrTenantDO for mnemonic : $mnemonic" }
        return tenantDO.toTenant(ehrTenantDO, ehrDO)
    }

    /**
     * Retrieves all [Tenant]s
     */
    fun getAllTenants(): List<Tenant> {
        logger.info { "Retrieving all tenants" }
        val tenantDOs = tenantDAO.getAllTenants()
        return getTenantsFromTenantDOs(tenantDOs)
    }

    /**
     * Retrieves all [Tenant]s flagged as monitored
     */
    fun getMonitoredTenants(): List<Tenant> {
        logger.info { "Retrieving all tenants" }
        val tenantDOs = tenantDAO.getMonitoredTenants()
        return getTenantsFromTenantDOs(tenantDOs)
    }

    /**
     * Retrieves configured codes for specified [mnemonic]
     */
    fun getCodesForTenantMnemonic(mnemonic: String): Map<String, String> {
        logger.info { "Retrieving codes for tenant with mnemonic: $mnemonic" }
        val tenantCodesDO = tenantCodesDao.getByTenantMnemonic(mnemonic) ?: return mapOf()
        return tenantCodesDO.properties.filterValues { it is String } as Map<String, String>
    }

    private fun getTenantsFromTenantDOs(tenantDOs: List<TenantDO>): List<Tenant> {
        if (tenantDOs.isEmpty()) return emptyList()
        logger.debug { "Found ${tenantDOs.size}" }

        val ehrDOByTenantId = tenantDOs.associate { it.id to it.ehr }

        val allEhrDAO = VendorType.values().map { ehrTenantDAOFactory.getEHRTenantDAOByVendorType(it) }

        val ehrTenantDOListMap = allEhrDAO.map {
            it.getAll().associateBy { it.tenantId }
        }
        val ehrTenantDOMap = ehrTenantDOListMap.flatMap { it.entries }
            .associate { it.key to it.value }

        logger.debug { "Found ${ehrTenantDOMap.size} EhrTenantDOs" }

        return tenantDOs.map {
            val ehrTenantDAO = ehrTenantDOMap[it.id]!!
            val ehrDO = ehrDOByTenantId[it.id]!!
            it.toTenant(ehrTenantDAO, ehrDO)
        }
    }

    /**
     * Inserts a new [Tenant] into the database
     */
    @Transactional
    fun insertTenant(tenant: Tenant): Tenant {
        logger.info { "Creating new tenant for mnemonic : ${tenant.mnemonic}" }

        val ehrDO = ehrDAO.getByInstance(tenant.vendor.instanceName)
            ?: throw NoEHRFoundException("No EHR found with instance: ${tenant.vendor.instanceName}")

        val newTenantDO = tenant.toTenantDO(ehrDO)
        logger.debug { "Inserting into tenantDAO" }
        val createdTenantDO = tenantDAO.insertTenant(newTenantDO)

        val ehrTenantDAO = ehrTenantDAOFactory.getEHRTenantDAO(createdTenantDO)
        val newEHRTenantDO = tenant.vendor.toEHRTenantDO(createdTenantDO.id)
        logger.debug { "Inserting into ehrTenantDAO" }
        val createdEHRTenantDO = ehrTenantDAO.insert(newEHRTenantDO)
        logger.info { "Successfully created new tenant for mnemonic : ${tenant.mnemonic}" }
        return createdTenantDO.toTenant(createdEHRTenantDO, ehrDO)
    }

    /**
     * Updates an existing [Tenant] into the database
     */
    @Transactional
    fun updateTenant(tenant: Tenant): Tenant {
        logger.info { "Updating tenant for mnemonic : ${tenant.mnemonic}" }

        val existingId = tenantDAO.getTenantForId(tenant.internalId)?.id
            ?: throw NoTenantFoundException("No tenant found with id: ${tenant.internalId}")
        val ehrDO = ehrDAO.getByInstance(tenant.vendor.instanceName)
            ?: throw NoEHRFoundException("No EHR found with instance: ${tenant.vendor.instanceName}")

        val updatedTenantDO = tenant.toTenantDO(ehrDO)
        updatedTenantDO.id = existingId
        logger.debug { "Updating tenantDAO" }
        tenantDAO.updateTenant(updatedTenantDO)

        val ehrTenantDAO = ehrTenantDAOFactory.getEHRTenantDAO(updatedTenantDO)
        val updatedEHRDO = tenant.vendor.toEHRTenantDO(existingId)
        logger.debug { "Updating ehrTenantDAO" }
        ehrTenantDAO.update(updatedEHRDO)
        logger.info { "Successfully updated tenant for mnemonic : ${tenant.mnemonic}" }
        return updatedTenantDO.toTenant(updatedEHRDO, ehrDO)
    }
}
