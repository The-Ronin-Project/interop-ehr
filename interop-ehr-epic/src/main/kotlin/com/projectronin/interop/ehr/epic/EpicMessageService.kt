package com.projectronin.interop.ehr.epic

import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import io.ktor.client.call.receive
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Service for facilitating sending messages to Epic.
 *
 * See: [SendMessage Documentation](https://apporchard.epic.com/Sandbox?api=384)
 */
@Component
class EpicMessageService(private val epicClient: EpicClient) :
    MessageService {
    private val logger = KotlinLogging.logger { }
    private val sendMessageUrlPart = "/api/epic/2014/Common/Utility/SENDMESSAGE/Message"

    override fun sendMessage(tenant: Tenant, messageInput: EHRMessageInput): String {
        val vendor = tenant.vendor
        if (vendor !is Epic) throw IllegalStateException("Tenant is not Epic vendor: ${tenant.mnemonic}")

        val sendMessageRequest = translateMessageInput(messageInput, vendor.ehrUserId, vendor.messageType)

        val response = runBlocking {
            val httpResponse = epicClient.post(tenant, sendMessageUrlPart, sendMessageRequest)
            if (httpResponse.status != HttpStatusCode.OK) {
                logger.error { "SendMessage failed for ${tenant.mnemonic}, with a ${httpResponse.status}" }
                throw IOException("Call to tenant ${tenant.mnemonic} failed with a ${httpResponse.status}")
            }
            httpResponse.receive<SendMessageResponse>()
        }

        // Return the id of the message
        return response.idTypes[0].id
    }

    private fun translateMessageInput(
        messageInput: EHRMessageInput,
        userId: String,
        messageType: String
    ): SendMessageRequest {
        return SendMessageRequest(
            patientID = messageInput.patientMRN,
            recipients = translateRecipients(messageInput.recipients),
            messageText = messageInput.text,
            senderID = userId,
            messageType = messageType
        )
    }

    private fun translateRecipients(recipients: List<EHRRecipient>): List<SendMessageRecipient> {
        return recipients.map { SendMessageRecipient(iD = it.id, isPool = it.isPool) }.toList()
    }
}
