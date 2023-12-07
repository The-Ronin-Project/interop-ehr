package com.projectronin.interop.tenant.config.data.binding

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.ZoneId

/**
 * Reads a String into a ZoneId.
 */
fun BaseTable<*>.timezone(name: String): Column<ZoneId> = registerColumn(name, ZoneIdSqlType)

/**
 * SqlType supporting storing a ZoneId.
 */
object ZoneIdSqlType : SqlType<ZoneId>(Types.VARCHAR, "varchar") {
    override fun doGetResult(
        rs: ResultSet,
        index: Int,
    ): ZoneId? {
        val timezone = rs.getString(index)
        return timezone?.let { ZoneId.of(timezone) }
    }

    override fun doSetParameter(
        ps: PreparedStatement,
        index: Int,
        parameter: ZoneId,
    ) {
        ps.setString(index, parameter.id)
    }
}
