package com.projectronin.interop.ehr.epic.apporchard.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class SendMessageRecipientTest {
    @Test
    fun `ensure json serialization conventions are correct`() {
        val sendMessageRecipientString =
            jacksonObjectMapper().writeValueAsString(SendMessageRecipient(iD = "ID", iDType = "Type", isPool = true))

        assert(sendMessageRecipientString.contains("\"IsPool\":true"))
        assert(sendMessageRecipientString.contains("\"Id\":\"ID\""))
        assert(sendMessageRecipientString.contains("\"Idtype\":\"Type\""))
    }
}
