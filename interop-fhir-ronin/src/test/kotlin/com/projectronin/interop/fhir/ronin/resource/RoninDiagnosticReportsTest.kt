package com.projectronin.interop.fhir.ronin.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.ronin.localization.Localizer
import com.projectronin.interop.fhir.ronin.localization.Normalizer
import com.projectronin.interop.fhir.ronin.resource.diagnosticReport.RoninDiagnosticReportLaboratory
import com.projectronin.interop.fhir.ronin.resource.diagnosticReport.RoninDiagnosticReportNoteExchange
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninDiagnosticReportsTest {
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val extension1 = Extension(
        url = Uri("http://example.com/extension"),
        value = DynamicValue(DynamicValueType.STRING, FHIRString("value"))
    )
    private val normalizer = mockk<Normalizer>()
    private val localizer = mockk<Localizer>()
    private val profile1 = mockk<RoninDiagnosticReportLaboratory>()
    private val profile2 = mockk<RoninDiagnosticReportNoteExchange>()
    private val roninDiagnosticReports = RoninDiagnosticReports(normalizer, localizer, profile1, profile2)

    @Test
    fun `always qualifies`() {
        assertTrue(
            roninDiagnosticReports.qualifies(
                DiagnosticReport(
                    code = CodeableConcept(text = "dx report".asFHIR()),
                    status = Code("registered")
                )
            )
        )
    }

    @Test
    fun `can validate against a profile`() {
        val dxReport = mockk<DiagnosticReport>()

        every { profile1.qualifies(dxReport) } returns true
        every { profile1.validate(dxReport, LocationContext(DiagnosticReport::class)) } returns Validation()
        every { profile2.qualifies(dxReport) } returns false

        roninDiagnosticReports.validate(dxReport).alertIfErrors()
    }

    @Test
    fun `can transform to profile`() {
        val original = mockk<DiagnosticReport> {
            every { id } returns Id("1234")
        }
        every { normalizer.normalize(original, tenant) } returns original

        val mappedDxReport = mockk<DiagnosticReport> {
            every { id } returns Id("1234")
            every { extension } returns listOf(extension1)
        }

        val roninDxReport = mockk<DiagnosticReport> {
            every { id } returns Id("test-1234")
            every { extension } returns listOf(extension1)
        }
        every { localizer.localize(roninDxReport, tenant) } returns roninDxReport

        every { profile1.qualifies(original) } returns false
        every { profile2.qualifies(original) } returns true
        every { profile2.conceptMap(original, LocationContext(DiagnosticReport::class), tenant) } returns Pair(
            mappedDxReport,
            Validation()
        )
        every { profile1.qualifies(mappedDxReport) } returns false
        every { profile2.qualifies(mappedDxReport) } returns true
        every {
            profile2.transformInternal(
                mappedDxReport,
                LocationContext(DiagnosticReport::class),
                tenant
            )
        } returns Pair(
            roninDxReport,
            Validation()
        )
        every { profile1.qualifies(roninDxReport) } returns false
        every { profile2.qualifies(roninDxReport) } returns true
        every { profile2.validate(roninDxReport, LocationContext(DiagnosticReport::class)) } returns Validation()

        val (transformed, validation) = roninDiagnosticReports.transform(original, tenant)
        validation.alertIfErrors()

        transformed!!
        assertEquals(roninDxReport, transformed)
    }
}
