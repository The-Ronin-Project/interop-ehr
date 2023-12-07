package com.projectronin.interop.ehr.epic.spring

import com.projectronin.interop.datalake.spring.DatalakeSpringConfig
import com.projectronin.interop.ehr.hl7.spring.HL7SpringConfig
import com.projectronin.interop.ehr.spring.EHRSpringConfig
import com.projectronin.interop.tenant.config.spring.TenantSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan(
    *[
        "com.projectronin.interop.ehr.epic",
        "com.projectronin.ehr.dataauthority.client", // INT-2129
    ],
)
@Import(
    EHRSpringConfig::class,
    DatalakeSpringConfig::class,
    TenantSpringConfig::class,
    HL7SpringConfig::class,
)
class EpicSpringConfig
