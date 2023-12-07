package com.projectronin.interop.ehr.inputs

import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NoteInputTest {
    @Test
    fun `can create object`() {
        val dt = LocalDateTime.now()
        val test =
            NoteInput(
                noteText = "123",
                dateTime = dt,
                noteSender = NoteSender.PRACTITIONER,
                isAlert = false,
                patient = mockk<Patient>(),
                practitioner = mockk<Practitioner>(),
            )
        assertEquals("123", test.noteText)
        assertEquals(dt, test.dateTime)
        assertEquals(NoteSender.PRACTITIONER, test.noteSender)
        assertFalse(test.isAlert)
        assertNotNull(test.patient)
        assertNotNull(test.practitioner)
    }

    @Test
    fun `enum works`() {
        assertEquals(2, NoteSender.values().size)
    }
}
