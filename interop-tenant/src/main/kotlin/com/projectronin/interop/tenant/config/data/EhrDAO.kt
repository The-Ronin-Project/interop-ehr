package com.projectronin.interop.tenant.config.data

import com.projectronin.interop.tenant.config.data.binding.EhrDOs
import com.projectronin.interop.tenant.config.data.model.EhrDO
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.dsl.map
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
    fun insert(ehrVendor: EhrDO): EhrDO? {
        return try {
            val ehrKey = database.insertAndGenerateKey(EhrDOs) {
                set(it.name, ehrVendor.vendorType)
                set(it.clientId, ehrVendor.clientId)
                set(it.publicKey, ehrVendor.publicKey)
                set(it.privateKey, ehrVendor.privateKey)
            }
            val result = database.from(EhrDOs).select().where(EhrDOs.id eq ehrKey.toString().toInt())
                .map { EhrDOs.createEntity(it) }
            result.getOrNull(0)
        } catch (e: Exception) {
            logger.error(e) { "insert failed: $e" }
            null
        }
    }
    /**
     * Updates row based on id, returns updated values
     */
    fun update(ehrVendor: EhrDO): EhrDO? {
        return try {
            val row = database.update(EhrDOs) {
                set(it.name, ehrVendor.vendorType)
                set(it.clientId, ehrVendor.clientId)
                set(it.privateKey, ehrVendor.privateKey)
                set(it.publicKey, ehrVendor.publicKey)
                where {
                    it.name eq ehrVendor.vendorType
                }
            }
            val result = database.from(EhrDOs).select().where(EhrDOs.name eq ehrVendor.vendorType)
                .map { EhrDOs.createEntity(it) }
            return result.getOrNull(0)
        } catch (e: Exception) {
            logger.error(e) { "update failed: $e" }
            null
        }
    }
    /**
     * Returns all values in table
     */
    fun read(): List<EhrDO> {
        return database.from(EhrDOs).select().mapNotNull { EhrDOs.createEntity(it) }
    }
}
