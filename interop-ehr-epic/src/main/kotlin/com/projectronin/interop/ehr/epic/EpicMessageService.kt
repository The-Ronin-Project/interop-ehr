package com.projectronin.interop.ehr.epic

import com.projectronin.interop.common.exceptions.VendorIdentifierNotFoundException
import com.projectronin.interop.ehr.MessageService
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRecipient
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageRequest
import com.projectronin.interop.ehr.epic.apporchard.model.SendMessageResponse
import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.ehr.inputs.EHRMessageInput
import com.projectronin.interop.ehr.inputs.EHRRecipient
import com.projectronin.interop.ehr.inputs.IdentifierVendorIdentifier
import com.projectronin.interop.tenant.config.ProviderPoolService
import com.projectronin.interop.tenant.config.model.Tenant
import com.projectronin.interop.tenant.config.model.vendor.Epic
import datadog.trace.api.Trace
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Service for facilitating sending messages to Epic.
 *
 * See: [SendMessage Documentation](https://apporchard.epic.com/Sandbox?api=384)
 */
@Component
class EpicMessageService(private val epicClient: EpicClient, private val providerPoolService: ProviderPoolService) :
    MessageService {
    private val logger = KotlinLogging.logger { }
    private val sendMessageUrlPart = "/api/epic/2014/Common/Utility/SENDMESSAGE/Message"

    @Trace
    override fun sendMessage(tenant: Tenant, messageInput: EHRMessageInput): String {
        val vendor = tenant.vendor
        if (vendor !is Epic) throw IllegalStateException("Tenant is not Epic vendor: ${tenant.mnemonic}")
        logger.info { "SendMessage started for ${tenant.mnemonic}" }

        val sendMessageRequest =
            translateMessageInput(messageInput, vendor.ehrUserId, vendor.messageType, tenant)

        val response = try {
            runBlocking {
                val httpResponse = epicClient.post(tenant, sendMessageUrlPart, sendMessageRequest)
                httpResponse.body<SendMessageResponse>()
            }
        } catch (e: Exception) { // further investigation required to see if this is a sustainable solution
            logger.error { "SendMessage failed for ${tenant.mnemonic}, with exception ${e.message}" }
            throw e
        }
        logger.info { "SendMessage completed for ${tenant.mnemonic}" }

        // Return the id of the message
        return response.idTypes[0].id
    }

    private fun translateMessageInput(
        messageInput: EHRMessageInput,
        userId: String,
        messageType: String,
        tenant: Tenant,
    ): SendMessageRequest {
        return SendMessageRequest(
            patientID = messageInput.patientMRN,
            patientIDType = (tenant.vendor as Epic).patientMRNTypeText,
            recipients = translateRecipients(messageInput.recipients, tenant),
            messageText = messageInput.text.split("\r\n", "\n").map { if (it.isEmpty()) " " else it },
            senderID = userId,
            messageType = messageType
        )
    }

    private fun translateRecipients(
        recipients: List<EHRRecipient>,
        tenant: Tenant,
    ): List<SendMessageRecipient> {

        // The Epic implementation of sendMessage expects the VendorIdentifier to be an IdentifierVendorIdentifier.
        // with a value. If it isn't, this will rightfully throw an exception.
        val idList =
            recipients.map {
                val identifierVendorIdentifier = it.identifier as IdentifierVendorIdentifier
                identifierVendorIdentifier.identifier.value?.value ?: throw VendorIdentifierNotFoundException()
            }
        val poolList = providerPoolService.getPoolsForProviders(tenant, idList)

        return idList.map { userID ->
            val poolID = poolList[userID]
            SendMessageRecipient(
                iD = poolID ?: userID,
                isPool = (poolID != null)
            )
        }.toList().distinct() // deduplicate
    }
}
