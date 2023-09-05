package com.projectronin.interop.fhir.ronin.spring

import com.projectronin.interop.fhir.ronin.resource.RoninPatient
import com.projectronin.interop.fhir.ronin.transform.TransformManager
import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [RoninProfileConfig::class, TestConfig::class])
class RoninProfileConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads TransformManager`() {
        val service = applicationContext.getBean<TransformManager>()
        assertNotNull(service)
        assertInstanceOf(TransformManager::class.java, service)
    }

    @Test
    fun `loads RoninPatient`() {
        val patient = applicationContext.getBean<RoninPatient>()
        assertNotNull(patient)
        assertInstanceOf(RoninPatient::class.java, patient)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun httpClient() = mockk<HttpClient>(relaxed = true)

    @Bean
    fun threadPoolTaskExecutor() = mockk<ThreadPoolTaskExecutor>(relaxed = true)
}
