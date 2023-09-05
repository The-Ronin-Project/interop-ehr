package com.projectronin.interop.tenant.config.spring

import com.projectronin.interop.tenant.config.TenantService
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
@ContextConfiguration(classes = [TenantSpringConfig::class, TestConfig::class])
class TenantSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads TenantService`() {
        val service = applicationContext.getBean<TenantService>()
        assertNotNull(service)
        assertInstanceOf(TenantService::class.java, service)
    }
}

@Configuration
class TestConfig {
    @Bean
    @Qualifier("ehr")
    fun ehrDB() = mockk<Database>(relaxed = true)
}
