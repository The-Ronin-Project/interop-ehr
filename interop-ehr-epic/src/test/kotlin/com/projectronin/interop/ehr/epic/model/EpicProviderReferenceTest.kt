package com.projectronin.interop.ehr.epic.model

import com.projectronin.interop.ehr.epic.apporchard.model.IDType
import com.projectronin.interop.ehr.epic.apporchard.model.ScheduleProviderReturnWithTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EpicProviderReferenceTest {
    private val okIDTypes = listOf(
        IDType(id = "123", type = "External"),
        IDType(id = "456", type = "External")
    )

    private val okIdentifiers = okIDTypes.map {
        it.toIdentifier()
    }

    private val okProviders = listOf(
        ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(okIDTypes[0]),
            duration = "30",
            providerName = "Davey",
            time = "900"
        ),
        ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Nope",
            providerIDs = listOf(okIDTypes[1]),
            duration = "30",
            providerName = "Davey2",
            time = "900"
        )
    )

    // cheating here by just assuming the first one is what Identifier service would return
    private val providerIdMap = okProviders.associateWith {
        it.providerIDs[0].toIdentifier()
    }

    @Test
    fun `can build object`() {
        val provider = okProviders[0]
        val epicReference = EpicProviderReference(provider, providerIdMap)
        assertEquals(provider.providerName, epicReference.display)
        assertTrue { epicReference.identifier != null }
        assertEquals(null, epicReference.reference)
        assertEquals(null, epicReference.id)
        assertEquals("Practitioner", epicReference.type)
        assertTrue { epicReference.identifier in okIdentifiers }
    }

    @Test
    fun `can build object with multiple Ids`() {
        val providerIdentifier1 = IDType(id = "123", type = "External")
        val providerIdentifier2 = IDType(id = "BAD", type = "BAD")

        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier1, providerIdentifier2),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        // none of the providers in our map had more than 1 identifier
        val localProviderIdMap = listOf(provider).associateWith {
            it.providerIDs[0].toIdentifier()
        }
        val epicReference = EpicProviderReference(provider, localProviderIdMap)
        assertEquals(provider.providerName, epicReference.display)
        assertTrue { epicReference.identifier != null }
        assertTrue { epicReference.identifier in okIdentifiers }
        assertEquals(providerIdentifier1.id, epicReference.identifier?.value)
    }

    @Test
    fun `can build object with bad ids`() {
        val providerIdentifier = IDType(id = "BAD", type = "BAD")

        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        val epicReference = EpicProviderReference(provider, providerIdMap)
        assertEquals(provider.providerName, epicReference.display)
        assertNull(epicReference.identifier)
    }

    @Test
    fun `can build object with good ids and bad type`() {
        val providerIdentifier = IDType(id = "123", type = "BAD")

        val provider = ScheduleProviderReturnWithTime(
            departmentIDs = listOf(),
            departmentName = "Blank",
            providerIDs = listOf(providerIdentifier),
            duration = "30",
            providerName = "Davey",
            time = "900"
        )
        val epicReference = EpicProviderReference(provider, providerIdMap)
        assertEquals(provider.providerName, epicReference.display)
        assertNull(epicReference.identifier)
    }
}
