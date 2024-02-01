package com.projectronin.interop.ehr.cerner.spring

import com.projectronin.interop.ehr.cerner.CernerAppointmentService
import com.projectronin.interop.ehr.cerner.CernerVendorFactory
import com.projectronin.interop.ehr.spring.EHRSpringConfig
import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.ktorm.database.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [CernerSpringConfig::class, EHRSpringConfig::class])
class CernerSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads EHRFactory`() {
        val service = applicationContext.getBean<CernerVendorFactory>()
        assertNotNull(service)
        assertInstanceOf(CernerVendorFactory::class.java, service)
    }

    @Test
    fun `loads AppointmentService`() {
        val service = applicationContext.getBean<CernerAppointmentService>()
        assertNotNull(service)
        assertInstanceOf(CernerAppointmentService::class.java, service)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun httpClient() = mockk<HttpClient>(relaxed = true)

    @Bean
    @Qualifier("ehr")
    fun ehrDB() = mockk<Database>(relaxed = true)
}
