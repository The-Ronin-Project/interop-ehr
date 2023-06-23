package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.ParticipantGenerator
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.ronin.generators.resource.appointmentParticipant
import com.projectronin.interop.fhir.ronin.generators.resource.possibleParticipantStatus
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninParticipantUtilTest {
    var participant = ListDataGenerator(0, ParticipantGenerator())
    private val providedParticipant = listOf(
        Participant(
            status = possibleParticipantStatus.random(),
            type = listOf(
                CodeableConcept(
                    coding = listOf(Coding(system = Uri("some-system"), code = Code("some-code")))
                )
            ),
            actor = rcdmReference("Patient", "1234")
        )
    )

    @Test
    fun `generate rcdm participant when none is provided using possible participant`() {
        val roninParticipant = generateParticipant(participant.generate(), appointmentParticipant)
        assertEquals(roninParticipant, appointmentParticipant)
    }

    @Test
    fun `generate provided participant`() {
        val roninParticipant = generateParticipant(providedParticipant, appointmentParticipant)
        assertEquals(roninParticipant, providedParticipant)
    }
}
