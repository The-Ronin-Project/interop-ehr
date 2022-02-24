package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpicProviderPractitionerTest {
    @Test
    fun `can build object`() {
        val providerIdentifier = IDType(id = "123", type = "External")
        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        val epicPractitioner = EpicProviderParticipant(provider, emptyMap())
        assertEquals(1, epicPractitioner.actor.size)
    }
}
