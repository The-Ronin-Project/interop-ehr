package com.projectronin.interop.ehr.cerner

import com.projectronin.interop.ehr.cerner.client.CernerClient
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CernerMedicationStatementServiceTest {
    private val tenant = createTestTenant()
    private val cernerClient: CernerClient = mockk()
    private val medicationStatementService = CernerMedicationStatementService(cernerClient)

    @Test
    fun `empty list is returned`() {
        val response = medicationStatementService.getMedicationStatementsByPatientFHIRId(tenant, "123")
        assertEquals(emptyList<MedicationStatement>(), response)
    }

    @Test
    fun `empty list is returned event when you return a date`() {
        val response = medicationStatementService.getMedicationStatementsByPatientFHIRId(tenant, "123", LocalDate.now())
        assertEquals(emptyList<MedicationStatement>(), response)
    }

    @Test
    fun `getByID returns empty medication statement`() {
        val response = medicationStatementService.getByID(tenant, "8475")
        assertEquals(MedicationStatement(), response)
    }

    @Test
    fun `getByIDs returns empty map`() {
        val response = medicationStatementService.getByIDs(tenant, listOf("123", "abc", "45"))
        assertEquals(emptyMap<String, MedicationStatement>(), response)
    }
}
