package com.projectronin.interop.ehr.epic.spring

import com.projectronin.interop.ehr.epic.EpicAppointmentService
import com.projectronin.interop.ehr.epic.EpicVendorFactory
import com.projectronin.interop.queue.QueueService
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [EpicSpringConfig::class])
class EpicSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads EHRFactory`() {
        val service = applicationContext.getBean<EpicVendorFactory>()
        assertNotNull(service)
        assertInstanceOf(EpicVendorFactory::class.java, service)
    }

    @Test
    fun `loads AppointmentService`() {
        val service = applicationContext.getBean<EpicAppointmentService>()
        assertNotNull(service)
        assertInstanceOf(EpicAppointmentService::class.java, service)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun interopQueue() = mockk<QueueService>(relaxed = true)

    @Bean
    fun httpClient() = mockk<HttpClient>(relaxed = true)

    @Bean
    fun threadPoolTaskExecutor() = mockk<ThreadPoolTaskExecutor>(relaxed = true)

    @Bean
    @Qualifier("ehr")
    fun ehrDB() = mockk<Database>(relaxed = true)
}
