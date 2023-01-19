package com.projectronin.interop.ehr.epic.apporchard.exceptions

import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.ehr.epic.apporchard.model.exceptions.AppOrchardError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppOrchardErrorTest {
    @Test
    fun `default works`() {
        val exception = AppOrchardError(null)
        assertEquals("No Epic Error Provided", exception.message)
        assertEquals(LogMarkers.CLIENT_FAILURE, exception.logMarker)
    }

    @Test
    fun `passed message works`() {
        val exception = AppOrchardError("I forgor")
        assertEquals("I forgor", exception.message)
    }
}
