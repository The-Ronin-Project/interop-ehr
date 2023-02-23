package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.EpicTenantDO
import com.projectronin.interop.tenant.config.data.model.MirthTenantConfigDO
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Table binding definition for [EpicTenantDO] data objects.
 */
object MirthTenantConfigDOs : Table<MirthTenantConfigDO>("io_mirth_tenant_config") {
    var tenantId = int("io_tenant_id").references(TenantDOs) { it.tenant }
    val locationIds = varchar("location_ids").bindTo { it.locationIds }
    val loadTimestamp = utcDateTime("load_last_run").bindTo { it.lastUpdated }
}

/**
 * Reads a time into a UTC-based OffsetDateTime.
 */
fun BaseTable<*>.utcDateTime(name: String): Column<OffsetDateTime> = registerColumn(name, UTCDateTimeSqlType)

/**
 * SqlType supporting storing an OffsetDateTime relative to UTC.
 */
object UTCDateTimeSqlType : SqlType<OffsetDateTime>(Types.TIMESTAMP, "datetime") {
    override fun doGetResult(rs: ResultSet, index: Int): OffsetDateTime? {
        val timestamp = rs.getTimestamp(index)
        return timestamp?.let {
            OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC)
        }
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: OffsetDateTime) {
        ps.setTimestamp(index, Timestamp.from(parameter.toInstant()))
    }
}
