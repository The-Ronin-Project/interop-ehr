package com.projectronin.interop.ehr.hl7.tenant

import ca.uhn.hl7v2.DefaultHapiContext
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.common.vendor.VendorType
import com.projectronin.interop.ehr.hl7.MDMConfigService
import com.projectronin.interop.ehr.hl7.MDMService
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.tenant.config.data.TenantMDMConfigDAO
import com.projectronin.interop.tenant.config.data.model.TenantMDMConfigDO
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MDAStageHL7Test {
    val mdaTenantDO =
        TenantMDMConfigDO {
            mdmDocumentTypeID = "3000326"
            providerIdentifierSystem = "urn:oid:1.2.840.114350.1.13.412.3.7.5.737384.6"
            receivingSystem = "TenantApplication"
        }
    val mdmTenant =
        mockk<Tenant> {
            every { mnemonic } returns "mdaoc"
            every { name } returns "mda"
            every { vendor } returns
                mockk<Epic> {
                    every { type } returns VendorType.EPIC
                    every { patientMRNSystem } returns "urn:oid:1.2.840.114350.1.13.412.3.7.5.737384.14"
                }
        }
    private val mockMDMConfigDAO =
        mockk<TenantMDMConfigDAO> {
            every { getByTenantMnemonic(any()) } returns mdaTenantDO
        }
    private val mdmConfigService = MDMConfigService(mockMDMConfigDAO)
    val service = MDMService(mdmConfigService)

    // This is a real example from Stage and a real message from stage
    // DocumentReference is constructed from guessed inputs as to what was given to proxy, but patient / practitioner
    // are the real responses from datadog
    // You can't break this unless you get on the phone with MDA
    @Test
    fun test() {
        val patientString = this.javaClass.getResource("/mda-stage/patient1.json")!!.readText()
        val patient = JacksonUtil.readJsonObject(patientString, Patient::class)
        val practitionerString = this.javaClass.getResource("/mda-stage/practitioner1.json")!!.readText()
        val practitioner = JacksonUtil.readJsonObject(practitionerString, Practitioner::class)
        val documentReference =
            DocumentReference(
                status = Code("notUsed"),
                docStatus = Code(CompositionStatus.PRELIMINARY.code),
                date = Instant("2023-05-26T05:43:52.239+02:00"),
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    data =
                                        @Suppress("ktlint:standard:max-line-length")
                                        Base64Binary(
                                            value = "TU9OIDA1LzE1LzIzIDk6MzUgQU0gQ0RUCkxhdXJlbiBEVU1NWQogMzcgeS5vIE1STjogMjgyMjczCi1CbG9hdGluZwogCiBCbG9hdGluZwogCiBGcmVxdWVuY3k6IE9mdGVuCiAKIFNldmVyaXR5OiBNb2RlcmF0ZQogCiBOZXcgcHJvYmxlbTogWWVzCiAKIEludGVyZmVyZXMgd2l0aCBlYXRpbmcgb3IgZHJpbmtpbmc6IFllcwogClBhdGllbnQgYWxzbyB3cm90ZSBpbjoKSSdtIHRha2luZyBtZWRpY2luZSBidXQgSSBuZWVkIG1vcmUgcHJpbG9zZWM=",
                                        ),
                                ),
                        ),
                    ),
            )
        val expectedMDM =
            this.javaClass.getResource("/mda-stage/MDM1.txt")!!
                .readText()
                .replace("\n", "\r") // hapi hl7 garbage

        val mdm = service.generateMDM(patient, practitioner, documentReference, mdmTenant)
        // these are generated during construction, so we have to inject them back
        mdm.msh.dateTimeOfMessage.time.value = "20230526054354.202+0000"
        mdm.msh.messageControlID.value = "101"
        mdm.txa.uniqueDocumentNumber.universalID.value = "RoninNote202305260543542020000-101"
        val resultingMDM = DefaultHapiContext().pipeParser.encode(mdm)
        assertEquals(expectedMDM, resultingMDM)
    }
}
