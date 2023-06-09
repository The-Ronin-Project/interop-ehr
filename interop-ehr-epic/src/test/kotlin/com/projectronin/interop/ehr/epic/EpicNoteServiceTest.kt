package com.projectronin.interop.ehr.epic

import ca.uhn.hl7v2.model.v251.message.MDM_T02
import com.projectronin.interop.ehr.hl7.MDMService
import com.projectronin.interop.ehr.inputs.NoteInput
import com.projectronin.interop.ehr.inputs.NoteSender
import com.projectronin.interop.fhir.generators.datatypes.name
import com.projectronin.interop.fhir.generators.resources.patient
import com.projectronin.interop.fhir.generators.resources.practitioner
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType
import com.projectronin.interop.queue.QueueService
import com.projectronin.interop.tenant.config.model.Tenant
import io.ktor.utils.io.core.toByteArray
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.Month
import java.util.Base64

class EpicNoteServiceTest {
    private val capturedDocumentReference = slot<DocumentReference>()
    private val queueService = mockk<QueueService> {
        every { enqueueMessages(any()) } just runs
    }
    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }
    private val mdmService = mockk<MDMService> {
    }
    private val epicNoteService =
        EpicNoteService(mdmService, queueService)

    @BeforeEach
    fun setup() {
        val testMDM = MDM_T02()
        testMDM.initQuickstart("MDM", "T02", "T")
        testMDM.txa.uniqueDocumentNumber.universalID.value = "123"
        every {
            mdmService.generateMDM(
                patient = any(),
                practitioner = any(),
                documentReference = capture(capturedDocumentReference),
                tenant = tenant
            )
        } returns testMDM
    }

    @Test
    fun `works for sendPatientNote`() {
        val createdTime =
            LocalDateTime.of(2023, Month.JUNE, 6, 9, 30, 22)
        val noteInput = NoteInput(
            dateTime = createdTime,
            isAlert = true,
            noteSender = NoteSender.PATIENT,
            noteText = "Note Text",
            patient = patient {
                identifier generates 2
                name of listOf(
                    name {
                        use of Code("Offical")
                    }
                )
            },
            practitioner = practitioner {
                identifier generates 1
            }
        )
        val result = epicNoteService.sendPatientNote(tenant, noteInput)
        assertEquals("123", result)
        val docRef = capturedDocumentReference.captured
        assertEquals(CompositionStatus.PRELIMINARY.code, docRef.docStatus?.value)
        assertEquals(DocumentReferenceStatus.CURRENT.code, docRef.status?.value)
        assertEquals("2023-06-06T09:30:22.000Z", docRef.date?.value)
        assertEquals(
            Base64.getEncoder().encodeToString("Note Text".toByteArray()),
            docRef.content.first().attachment?.data?.value
        )
        assertEquals(0, docRef.relatesTo.size)
    }

    @Test
    fun `works for sendPatientNoteAddendum`() {
        val createdTime = LocalDateTime.of(2023, Month.JUNE, 6, 9, 30, 22)
        val noteInput = NoteInput(
            dateTime = createdTime,
            isAlert = false,
            noteSender = NoteSender.PRACTITIONER,
            noteText = "Note Text",
            patient = patient {
                identifier generates 2
                name of listOf(
                    name {
                        use of Code("Offical")
                    }
                )
            },
            practitioner = practitioner {
                identifier generates 1
            }
        )
        val result = epicNoteService.sendPatientNoteAddendum(tenant, noteInput, "oldId")
        assertEquals("123", result)
        val docRef = capturedDocumentReference.captured
        assertEquals(CompositionStatus.FINAL.code, docRef.docStatus?.value)
        assertEquals(DocumentReferenceStatus.CURRENT.code, docRef.status?.value)
        assertEquals("2023-06-06T09:30:22.000Z", docRef.date?.value)
        assertEquals(
            Base64.getEncoder().encodeToString("Note Text".toByteArray()),
            docRef.content.first().attachment?.data?.value
        )
        assertEquals(1, docRef.relatesTo.size)
        assertEquals(DocumentRelationshipType.APPENDS.code, docRef.relatesTo.first().code?.value)
        assertEquals("DocumentReference/oldId", docRef.relatesTo.first().target?.reference?.value)
    }

    @Test
    fun `works for patients without alerts`() {
        val createdTime = LocalDateTime.of(2023, Month.JUNE, 6, 9, 30, 22)
        val noteInput = NoteInput(
            dateTime = createdTime,
            isAlert = false,
            noteSender = NoteSender.PATIENT,
            noteText = "Note Text",
            patient = patient {
                identifier generates 2
                name of listOf(
                    name {
                        use of Code("Offical")
                    }
                )
            },
            practitioner = practitioner {
                identifier generates 1
            }
        )
        val result = epicNoteService.sendPatientNoteAddendum(tenant, noteInput, "oldId")
        assertEquals("123", result)
        val docRef = capturedDocumentReference.captured
        assertEquals(CompositionStatus.FINAL.code, docRef.docStatus?.value)
        assertEquals(DocumentReferenceStatus.CURRENT.code, docRef.status?.value)
        assertEquals("2023-06-06T09:30:22.000Z", docRef.date?.value)
        assertEquals(
            Base64.getEncoder().encodeToString("Note Text".toByteArray()),
            docRef.content.first().attachment?.data?.value
        )
        assertEquals(1, docRef.relatesTo.size)
        assertEquals(DocumentRelationshipType.APPENDS.code, docRef.relatesTo.first().code?.value)
        assertEquals("DocumentReference/oldId", docRef.relatesTo.first().target?.reference?.value)
    }
}
