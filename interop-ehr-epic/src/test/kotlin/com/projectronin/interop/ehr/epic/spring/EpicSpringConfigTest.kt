package com.projectronin.interop.ehr.epic.spring

import com.projectronin.interop.ehr.epic.EpicIdentifierService
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EpicSpringConfigTest {
    @Test
    fun `gets patient transformer`() {
        val identifierService = mockk<EpicIdentifierService>()
        val patientTransformer = EpicSpringConfig().epicPatientTransformer(identifierService)
        assertNotNull(patientTransformer)
    }
}
