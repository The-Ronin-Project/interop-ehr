package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.r4.resource.Participant

fun generateParticipant(
    participant: List<Participant>,
    possibleParticipant: List<Participant>
): List<Participant> {
    return participant.ifEmpty {
        possibleParticipant
    }
}
