package com.projectronin.interop.ehr.cerner.spring

import com.projectronin.ehr.dataauthority.client.spring.EHRDataAuthorityClientSpringConfig
import com.projectronin.interop.datalake.spring.DatalakeSpringConfig
import com.projectronin.interop.ehr.spring.EHRSpringConfig
import com.projectronin.interop.tenant.config.spring.TenantSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan(
    *[
        "com.projectronin.interop.ehr.cerner",
    ],
)
@Import(
    EHRSpringConfig::class,
    DatalakeSpringConfig::class,
    TenantSpringConfig::class,
    EHRDataAuthorityClientSpringConfig::class,
)
class CernerSpringConfig
