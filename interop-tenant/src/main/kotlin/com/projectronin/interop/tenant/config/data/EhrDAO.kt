package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.binding.EhrDOs
import com.projectronin.interop.tenant.config.data.model.EhrDO
import com.projectronin.interop.tenant.config.exception.NoEHRFoundException
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.mapNotNull
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

/**
 * Provides data access operations for EHR data models.
 */
@Repository
class EhrDAO(@Qualifier("ehr") private val database: Database) {
    private val logger = KotlinLogging.logger { }

    /**
     * Inserts new row (if no conflicts) and returns values that were inserted
     */
    fun insert(ehrVendor: EhrDO): EhrDO {
        try {
            database.insertAndGenerateKey(EhrDOs) {
                set(it.name, ehrVendor.vendorType)
                set(it.clientId, ehrVendor.clientId)
                set(it.publicKey, ehrVendor.publicKey)
                set(it.privateKey, ehrVendor.privateKey)
            }
        } catch (e: Exception) {
            logger.error(e) { "insert failed: $e" }
            throw e
        }
        return getByType(ehrVendor.vendorType)
            // this is almost impossible to hit. The only way is if the insert is successful and either we can't find
            // the EHR DO or there are multiple (but DB constraints prevent that)
            ?: throw NoEHRFoundException("Failed to find EHR with type: ${ehrVendor.vendorType}")
    }

    /**
     * Updates row based on id, returns updated values
     */
    fun update(ehrVendor: EhrDO): EhrDO {
        try {
            database.update(EhrDOs) {
                set(it.name, ehrVendor.vendorType)
                set(it.clientId, ehrVendor.clientId)
                set(it.privateKey, ehrVendor.privateKey)
                set(it.publicKey, ehrVendor.publicKey)
                where {
                    it.name eq ehrVendor.vendorType
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "update failed: $e" }
            throw e
        }
        return getByType(ehrVendor.vendorType)
            ?: throw NoEHRFoundException("No Existing EHR found with type: ${ehrVendor.vendorType}")
    }

    /**
     * Returns all values in table
     */
    fun read(): List<EhrDO> {
        return database.from(EhrDOs).select().mapNotNull { EhrDOs.createEntity(it) }
    }

    /**
     * Returns single values in table with a matching vendorType
     */
    fun getByType(vendorType: VendorType): EhrDO? {
        return database.from(EhrDOs)
            .select()
            .where(EhrDOs.name eq vendorType)
            .mapNotNull { EhrDOs.createEntity(it) }
            .firstOrNull()
    }
}
