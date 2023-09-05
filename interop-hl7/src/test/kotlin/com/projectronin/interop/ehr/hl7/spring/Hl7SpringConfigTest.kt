package com.projectronin.interop.ehr.hl7.spring

import com.projectronin.interop.ehr.hl7.MDMService
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [HL7SpringConfig::class, TestConfig::class])
class Hl7SpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads MDMService`() {
        val service = applicationContext.getBean<MDMService>()
        assertNotNull(service)
        assertInstanceOf(MDMService::class.java, service)
    }
}

@Configuration
class TestConfig {

    @Bean
    fun threadPoolTaskExecutor() = mockk<ThreadPoolTaskExecutor>(relaxed = true)

    @Bean
    @Qualifier("ehr")
    fun ehrDB() = mockk<Database>(relaxed = true)
}
