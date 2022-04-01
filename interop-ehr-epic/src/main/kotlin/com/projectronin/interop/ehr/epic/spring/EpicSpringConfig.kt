package com.projectronin.interop.ehr.epic.spring

import com.projectronin.interop.ehr.epic.EpicIdentifierService
import com.projectronin.interop.transform.fhir.r4.R4PatientTransformer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EpicSpringConfig {
    @Qualifier("epic")
    @Bean
    fun epicPatientTransformer(identifierService: EpicIdentifierService): R4PatientTransformer =
        R4PatientTransformer(identifierService)
}
