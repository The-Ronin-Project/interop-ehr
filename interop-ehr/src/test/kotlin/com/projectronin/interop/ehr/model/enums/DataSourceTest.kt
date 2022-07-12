package com.projectronin.interop.ehr.model.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataSourceTest {
    @Test
    fun `all data source types are available`() {
        val allValues = DataSource.values()
        assertEquals(2, allValues.size)
    }
}
