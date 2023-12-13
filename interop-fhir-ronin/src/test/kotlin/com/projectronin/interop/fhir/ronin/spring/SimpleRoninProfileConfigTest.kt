package com.projectronin.interop.fhir.ronin.spring

import com.projectronin.interop.fhir.ronin.resource.RoninPatient
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SimpleRoninProfileConfig::class, TestConfig::class])
class SimpleRoninProfileConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads RoninPatient`() {
        val patient = applicationContext.getBean<RoninPatient>()
        assertNotNull(patient)
        assertInstanceOf(RoninPatient::class.java, patient)
    }
}
