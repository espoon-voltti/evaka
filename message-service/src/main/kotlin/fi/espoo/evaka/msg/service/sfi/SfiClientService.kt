// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.mapper.ISfiMapper
import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.sficlient.soap.LahetaViestiResponse
import fi.espoo.evaka.msg.sficlient.soap.ObjectFactory
import fi.espoo.voltti.logging.loggers.info
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

        logger.info(
            mapOf(
                "meta" to mapOf(
                    "caseId" to caseId,
                    "messageId" to uniqueSfiMessageId
                )
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
            throw Exception("Error while sending SFI request about $caseId with messageId: $uniqueSfiMessageId", e)
        }

        return soapResponse.let(SfiResponse.Mapper::from)
            .let {
                if (it.isOkResponse()) {
                    logger.info(
                        mapOf(
                            "meta" to mapOf(
                                "caseId" to caseId,
                                "messageId" to uniqueSfiMessageId,
                                "response" to it.text
                            )
                        )
                    ) { "Successfully sent SFI message about $caseId with messageId: $uniqueSfiMessageId response: ${it.text}" }
                } else {
                    sfiErrorResponseHandler.handleError(it)
                }
                it
            }
    }

    private inline fun <reified T> WebServiceTemplate.marshalSendAndReceiveAsType(request: Any): T =
        marshalSendAndReceive(request)
            .let {
                it as? T ?: throw IllegalStateException("Unexpected SFI response type : ${it.javaClass}")
            }
}
