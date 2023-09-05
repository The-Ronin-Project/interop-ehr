package com.projectronin.interop.ehr.spring

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(
    *[
        "com.projectronin.interop.ehr.factory",
        "com.projectronin.interop.ehr.auth"
    ]
)
class EHRSpringConfig
