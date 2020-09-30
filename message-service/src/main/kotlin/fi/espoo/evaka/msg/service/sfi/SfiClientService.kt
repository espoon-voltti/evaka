// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.mapper.ISfiMapper
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageDetails
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.service.sfi.SfiClientService.SfiRequestStatus.CREATING_REQUEST
import fi.espoo.evaka.msg.service.sfi.SfiClientService.SfiRequestStatus.ERROR_DURING_REQUEST
import fi.espoo.evaka.msg.service.sfi.SfiClientService.SfiRequestStatus.RECEIVED_ERROR_RESPONSE
import fi.espoo.evaka.msg.service.sfi.SfiClientService.SfiRequestStatus.RECEIVED_OK_RESPONSE
import fi.espoo.evaka.msg.sficlient.soap.LahetaViestiResponse
import fi.espoo.evaka.msg.sficlient.soap.ObjectFactory
import fi.espoo.voltti.logging.loggers.audit
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.ws.client.core.WebServiceTemplate
import java.util.UUID.randomUUID

@Service
@Profile("production", "sfi-dev")
class SfiClientService(
    private val sfiObjectFactory: ObjectFactory,
    private val wsTemplate: WebServiceTemplate,
    private val sfiAccountDetailsService: SfiAccountDetailsService,
    private val sfiMapper: ISfiMapper,
    private val sfiErrorResponseHandler: SfiErrorResponseHandler
) : ISfiClientService {

    private val logger = KotlinLogging.logger {}

    override fun sendMessage(metadata: MessageMetadata): SfiResponse {
        val uniqueSfiMessageId = metadata.message.messageId ?: randomUUID().toString()
        val caseId = metadata.message.uniqueCaseIdentifier

        logger.info { "Sending SFI message about $caseId with messageId: $uniqueSfiMessageId" }
        logger.audit(
            toLogParamsMap(
                messageDetails = metadata.message,
                requestId = uniqueSfiMessageId.toString(),
                status = CREATING_REQUEST
            )
        ) { "Sending SFI message about $caseId with messageId: $uniqueSfiMessageId" }

        val request = sfiObjectFactory.createLahetaViesti()
            .apply {

                viranomainen = sfiAccountDetailsService.getAuthorityDetails(uniqueSfiMessageId)
                kysely = sfiAccountDetailsService.createQueryWithPrintingDetails()
                kysely.kohteet = sfiMapper.mapToSoapTargets(metadata)
            }

        val soapResponse: LahetaViestiResponse = try {
            wsTemplate.marshalSendAndReceiveAsType(request)
        } catch (e: Exception) {
            logger.error("Error while sending SFI request $uniqueSfiMessageId", e)
            logger.audit(
                toLogParamsMap(
                    messageDetails = metadata.message,
                    requestId = uniqueSfiMessageId.toString(),
                    status = ERROR_DURING_REQUEST
                )
            ) {
                "Error while sending SFI request about $caseId with messageId: " +
                    "$uniqueSfiMessageId"
            }
            throw e
        }

        return soapResponse.let(SfiResponse.Mapper::from)
            .let {
                if (it.isOkResponse()) {
                    logger.info {
                        "Successfully sent SFI message about $caseId with messageId: $uniqueSfiMessageId " +
                            "response: ${it.text}"
                    }
                    logger.audit(
                        toLogParamsMap(
                            messageDetails = metadata.message,
                            requestId = uniqueSfiMessageId.toString(),
                            status = RECEIVED_OK_RESPONSE
                        )
                    ) {
                        "Successfully sent SFI message about $caseId " +
                            "with messageId: $uniqueSfiMessageId"
                    }
                } else {
                    logger.error {
                        "SFI responded with error to message about $caseId with messageId: " +
                            "$uniqueSfiMessageId. Response ${it.code} : ${it.text}"
                    }
                    logger.audit(
                        toLogParamsMap(
                            messageDetails = metadata.message,
                            requestId = uniqueSfiMessageId.toString(),
                            status = RECEIVED_ERROR_RESPONSE
                        )
                    ) {
                        "SFI responded with error to message about $caseId with " +
                            "messageId: $uniqueSfiMessageId. Response: ${it.code}"
                    }
                    sfiErrorResponseHandler.handleError(it)
                }
                it
            }
    }

    private fun toLogParamsMap(messageDetails: MessageDetails, requestId: String, status: SfiRequestStatus) = mapOf(
        "meta" to mapOf(
            "status" to status.value
        ),
        "targetId" to requestId,
        "objectId" to messageDetails.uniqueCaseIdentifier
    )

    enum class SfiRequestStatus(val value: String) {
        CREATING_REQUEST("creating request"),
        RECEIVED_OK_RESPONSE("ok response received"),
        RECEIVED_ERROR_RESPONSE("error response received"),
        ERROR_DURING_REQUEST("error during request")
    }

    private inline fun <reified T> WebServiceTemplate.marshalSendAndReceiveAsType(request: Any): T =
        marshalSendAndReceive(request)
            .let {
                it as? T ?: throw IllegalStateException("Unexpected SFI response type : ${it.javaClass}")
            }
}
