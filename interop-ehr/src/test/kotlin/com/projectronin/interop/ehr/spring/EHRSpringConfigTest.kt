package com.projectronin.interop.ehr.spring

import com.projectronin.interop.ehr.factory.EHRFactory
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
@ContextConfiguration(classes = [EHRSpringConfig::class])
class EHRSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads EHRFactory`() {
        val service = applicationContext.getBean<EHRFactory>()
        assertNotNull(service)
        assertInstanceOf(EHRFactory::class.java, service)
    }
}
