package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.tenant.config.data.model.EhrDO
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Table binding definition for [EhrDO] data objects.
 */
object EhrDOs : Table<EhrDO>("io_ehr") {
    val id = int("io_ehr_id").primaryKey().bindTo { it.id }
    val instanceName = varchar("instance_name").bindTo { it.instanceName }
    val name = enum<VendorType>("name").bindTo { it.vendorType }
    val clientId = varchar("client_id").bindTo { it.clientId }
    val publicKey = varchar("public_key").bindTo { it.publicKey }
    val privateKey = varchar("private_key").bindTo { it.privateKey }
}
