package com.projectronin.interop.ehr.hl7.spring

import com.projectronin.interop.tenant.config.spring.TenantSpringConfig
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ComponentScan("com.projectronin.interop.ehr.hl7")
@Import(TenantSpringConfig::class)
class HL7SpringConfig
