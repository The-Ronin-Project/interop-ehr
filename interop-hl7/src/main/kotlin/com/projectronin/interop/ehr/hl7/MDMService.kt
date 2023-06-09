package com.projectronin.interop.ehr.hl7

import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v251.datatype.TX
import ca.uhn.hl7v2.model.v251.message.MDM_T02
import ca.uhn.hl7v2.model.v251.segment.OBX
import ca.uhn.hl7v2.model.v251.segment.PID
import ca.uhn.hl7v2.model.v251.segment.PV1
import ca.uhn.hl7v2.model.v251.segment.TXA
import com.interop.ehr.hl7.fhir.converters.datatypes.toFormattedDate
import com.projectronin.interop.ehr.hl7.converters.datatypes.toFormattedDate
import com.projectronin.interop.ehr.hl7.converters.datatypes.toHL7Code
import com.projectronin.interop.ehr.hl7.converters.datatypes.toPID3
import com.projectronin.interop.ehr.hl7.converters.resources.getNote
import com.projectronin.interop.ehr.hl7.converters.resources.getParentNoteID
import com.projectronin.interop.ehr.hl7.converters.resources.toAvailableStatus
import com.projectronin.interop.ehr.hl7.converters.resources.toCompleteStatus
import com.projectronin.interop.ehr.hl7.converters.resources.toConfidentialityStatus
import com.projectronin.interop.fhir.r4.datatype.primitive.asEnum
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.util.UUID

/***
 * This service handles generating MDM Messages
 */
@Component
class MDMService(private val mdmConfigService: MDMConfigService) {
    /***
     * Given a fhir [patient], [practitioner] and [documentReference], using the [mdmConfigService] and [tenant]
     * to generate an MDM message
     */
    fun generateMDM(
        patient: Patient,
        practitioner: Practitioner,
        documentReference: DocumentReference,
        tenant: Tenant
    ): MDM_T02 {
        val mdm = MDM_T02()
        val parentNote = documentReference.getParentNoteID()
        val eventType = parentNote?.let { "T08" } ?: "T02"
        val formattedDateTime = documentReference.date?.toFormattedDate()!!
        mdm.initQuickstart("MDM", eventType, "T")
        mdm.msh.msh3_SendingApplication.namespaceID.value = "RONIN"
        mdm.msh.msh5_ReceivingApplication.namespaceID.value = mdmConfigService.getReceivingApplication(tenant)
        mdm.msh.msh10_MessageControlID.value = UUID.randomUUID().toString()
        // Populate the EVN Segment
        mdm.evn.eventTypeCode.value = eventType
        mdm.evn.recordedDateTime.time.value = formattedDateTime

        val noteID = if (parentNote != null) {
            parentNote
        } else {
            val re = Regex(pattern = "[^A-Za-z0-9 ]")
            "RoninNote" + re.replace(mdm.msh.dateTimeOfMessage.time.value, "") + "-" + mdm.msh.messageControlID.value
        }

        // Populate PID Segment
        mdm.pid.buildPID(patient, tenant)
        // Populate the PV1 Segment
        val pv1: PV1 = mdm.pV1
        pv1.patientClass.value = "U"

        // Populate the TXA Segment
        mdm.txa.buildTXA(practitioner, documentReference, formattedDateTime, noteID, tenant)

        // Populate the OBX Segment MDA: Replace all tabs with 4 spaces
        val note = documentReference.getNote()
        mdm.setOBX(note)

        return mdm
    }

    // In the event that we start generating other types of messages, this could easily be re-used
    private fun PID.buildPID(patient: Patient, tenant: Tenant) {
        // that determines which identifiers we should send back in PID3
        val identifiersToSend = mdmConfigService.getIdentifiersToSend(tenant, patient.identifier)
        identifiersToSend.forEachIndexed { index, it ->
            val cx = it.toPID3(this.message, "MRN")
            val existing = this.getPatientIdentifierList(index)
            existing.idNumber.value = cx.idNumber.value
            existing.assigningAuthority.namespaceID.value = cx.assigningAuthority.namespaceID.value
        }

        // per RCDM we are only guaranteed an official name
        val pid5 = patient.name.firstOrNull { it.use?.value == "official" }
        pid5?.let {
            this.getPatientName(0).familyName.surname.value = pid5.family?.value
            this.getPatientName(0).givenName.value = pid5.given.getOrNull(0)?.value
        }

        // PID-7 DOB
        this.dateTimeOfBirth.time.value = patient.birthDate?.toFormattedDate()

        // PID-8 Administrative Sex
        this.administrativeSex.value = patient.gender?.value.toString().take(1).uppercase()

        // PID-11 Address
        for (i in patient.address.indices) {
            this.getPatientAddress(i).streetAddress.streetOrMailingAddress.value =
                patient.address[i].line.mapNotNull { it.value }.joinToString(", ")
            this.getPatientAddress(i).city.value = patient.address[i].city?.value
            this.getPatientAddress(i).stateOrProvince.value = patient.address[i].state?.value
            this.getPatientAddress(i).zipOrPostalCode.value = patient.address[i].postalCode?.value
            this.getPatientAddress(i).country.value = patient.address[i].country?.value.orEmpty()
        }

        // PID-13 Phone
        val phonesToBeSend = patient.telecom.filter {
            it.use.asEnum<ContactPointUse>() in listOf(
                ContactPointUse.HOME,
                ContactPointUse.WORK,
                ContactPointUse.MOBILE
            )
        }
        phonesToBeSend.forEachIndexed { index, contactPoint ->
            this.getPhoneNumberHome(index).telephoneNumber.value = contactPoint.value?.value
            this.getPhoneNumberHome(index).telecommunicationUseCode.value =
                contactPoint.use.asEnum<ContactPointUse>()?.toHL7Code()
        }
    }

    private fun TXA.buildTXA(
        practitioner: Practitioner,
        documentReference: DocumentReference,
        formattedDate: String,
        noteId: String?,
        tenant: Tenant
    ) {
        this.setIDTXA.value = "1"

        // TXA-2 Document Type
        this.documentType.value = mdmConfigService.getDocumentTypeID(tenant)
        this.documentContentPresentation.value = "TX"

        // TXA-4 Activity Date/Time
        // we'd probably want to run this through the same formatter as above
        this.activityDateTime.time.value = formattedDate

        // TXA-5 Primary Activity Provider from Practitioner Info
        this.getPrimaryActivityProviderCodeName(0).familyName.surname.value =
            practitioner.name.getOrNull(0)?.family?.value
        this.getPrimaryActivityProviderCodeName(0).givenName.value =
            practitioner.name.getOrNull(0)?.given?.getOrNull(0)?.value

        val practitionerId = mdmConfigService.getPractitionerIdentifierToSend(tenant, practitioner.identifier)
        this.getPrimaryActivityProviderCodeName(0).idNumber.value = practitionerId?.value?.value

        // TXA-9 Originator Code/Name (need to provide information from Project Ronin as originators of the transcription)
        this.getOriginatorCodeName(0).familyName.surname.value = "Project"
        this.getOriginatorCodeName(0).givenName.value = "Ronin"

        // TXA-13 Parent Document Number, used for addendum (T06) messages
        this.uniqueDocumentNumber.universalID.value = noteId
        // TXA-17 Document Completion Status, default to documented.
        // This default comes from when a message is first requested at generation
        // MDA: IP "in progress" for incomplete record, AU "authenticated" for final record
        this.documentCompletionStatus.value = documentReference.toCompleteStatus()

        // TXA-18 Document Confidentiality Status, MDA: defaults to "U", unrestricted
        this.documentConfidentialityStatus.value = documentReference.toConfidentialityStatus()

        // TXA-19 Document Availability Status, MDA: defaults to "AV", available
        this.documentAvailabilityStatus.value = documentReference.toAvailableStatus()
    }

    private fun MDM_T02.setOBX(note: String) {
        var obxCount = 1
        for (line in note.lines()) {
            val lines = splitIntoChunks(string = line)
            for (i in lines.indices) {
                val obx: OBX = this.getOBSERVATION(obxCount - 1).obx
                obx.setIDOBX.value = (obxCount).toString()
                obx.valueType.value = "TX"
                val tx = TX(this)
                tx.value = lines[i]
                val value: Varies = obx.getObservationValue(0)
                value.data = tx
                obxCount += 1
            }
        }
    }

    private fun splitIntoChunks(max: Int = 65535, string: String): List<String> =
        ArrayList<String>(string.length / max + 1).also {
            var firstWord = true
            val builder = StringBuilder()

            // split string by whitespace
            for (word in string.split(Regex("( |\\n|\\r)+"))) {
                // if the current string exceeds the max size
                val newword = word.replace("\t", "    ")
                if (builder.length + newword.length > max) {
                    // then we add the string to the list and clear the builder
                    it.add(builder.toString())
                    builder.setLength(0)
                    firstWord = true
                }
                // append a space at the beginning of each word, except the first one
                if (firstWord) firstWord = false else builder.append(' ')
                builder.append(newword)
            }

            // add the last collected part if there was any
            if (builder.isNotEmpty()) {
                it.add(builder.toString())
            }
        }
}
