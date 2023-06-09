package com.projectronin.interop.ehr.epic

import ca.uhn.hl7v2.DefaultHapiContext
import com.projectronin.interop.common.hl7.EventType
import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.ehr.NoteService
import com.projectronin.interop.ehr.hl7.MDMService
import com.projectronin.interop.ehr.inputs.NoteInput
import com.projectronin.interop.ehr.inputs.NoteSender
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceRelatesTo
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import com.projectronin.interop.queue.QueueService
import com.projectronin.interop.queue.model.HL7Message
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.Base64

@Component
class EpicNoteService(
    private val mdmService: MDMService,
    private val queueService: QueueService
) : NoteService {
    companion object {
        private val instantFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss.SSS")
            .optionalStart()
            .appendLiteral("Z")
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HH:mm", "+00:00")
            .optionalEnd()
            .toFormatter()

        fun fromLocalDate(localDateTime: LocalDateTime): Instant {
            val formattedString = instantFormatter.format(localDateTime)
            return Instant(formattedString)
        }
    }

    override fun sendPatientNote(
        tenant: Tenant,
        noteInput: NoteInput
    ): String {
        return sendNoteInternal(tenant, noteInput, null)
    }

    override fun sendPatientNoteAddendum(
        tenant: Tenant,
        noteInput: NoteInput,
        parentDocumentId: String
    ): String {
        return sendNoteInternal(tenant, noteInput, parentDocumentId)
    }

    private fun sendNoteInternal(
        tenant: Tenant,
        noteInput: NoteInput,
        parentDocumentId: String?
    ): String {
        val documentStatus = if (noteInput.noteSender == NoteSender.PATIENT && noteInput.isAlert) {
            Code(CompositionStatus.PRELIMINARY.code)
        } else {
            Code(CompositionStatus.FINAL.code)
        }
        val parentDocumentReference = parentDocumentId?.let {
            listOf(
                DocumentReferenceRelatesTo(
                    code = Code(DocumentRelationshipType.APPENDS.code),
                    target = Reference(reference = "DocumentReference/$it".asFHIR())
                )
            )
        }
        val instant = fromLocalDate(noteInput.dateTime)
        val noteText = Base64.getEncoder().encodeToString(noteInput.noteText.toByteArray())
        val documentReference = DocumentReference(
            status = Code(DocumentReferenceStatus.CURRENT.code),
            docStatus = documentStatus,
            date = instant,
            content = listOf(
                DocumentReferenceContent(
                    attachment = Attachment(
                        data = Base64Binary(
                            value = noteText
                        )
                    )
                )
            ),
            relatesTo = parentDocumentReference ?: emptyList()
        )
        val mdm = mdmService.generateMDM(noteInput.patient, noteInput.practitioner, documentReference, tenant)
        val encodedMessage = DefaultHapiContext().pipeParser.encode(mdm)
        queueService.enqueueMessages(
            listOf(
                HL7Message(
                    id = null,
                    tenant = tenant.mnemonic,
                    text = encodedMessage,
                    hl7Type = MessageType.MDM,
                    hl7Event = parentDocumentId?.let { EventType.MDMT08 } ?: EventType.MDMT02
                )
            )
        )
        return mdm.txa.uniqueDocumentNumber.universalID.value
    }
}
