package com.projectronin.interop.tenant.config.data.binding

import com.projectronin.interop.tenant.config.data.model.TenantCodesDO
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TenantCodesDOs : Table<TenantCodesDO>("io_tenant_codes") {
    var tenantId = int("io_tenant_id").bindTo { it.tenantId }
    val bsaCode = varchar("bsa_code").bindTo { it.bsaCode }
    val bmiCode = varchar("bmi_code").bindTo { it.bmiCode }
    val stageCodes = varchar("stage_codes").bindTo { it.stageCodes }
}
