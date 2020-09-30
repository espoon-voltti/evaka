// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.service.sfi.SfiErrorResponseHandler.SFiMessageDeliveryException
import fi.espoo.evaka.msg.sficlient.soap.LahetaViestiResponse
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/5c69b9e445a7231c486dbfe6
// Taulukko 31. LahetaViestiResponse-elementti.
const val MSG_SENDING_SUCCESSFUL = 202
const val ERROR_SFI_ACCOUNT_NOT_FOUND = 461
const val ERROR_VALIDATION = 525

interface SfiErrorResponseHandler {
    class SFiMessageDeliveryException(message: String) : Exception(message)

    @Throws(SFiMessageDeliveryException::class)
    fun handleError(response: SfiResponse)
}

class DefaultSfiErrorResponseHandler : SfiErrorResponseHandler {
    override fun handleError(response: SfiResponse) = throwWithCodeAndText(response)
}

data class SfiResponse(val code: Int, val text: String) {
    fun isOkResponse(): Boolean = code == MSG_SENDING_SUCCESSFUL

    object Mapper {
        fun from(soapResponse: LahetaViestiResponse) = soapResponse.lahetaViestiResult.tilaKoodi
            .let { SfiResponse(code = it.tilaKoodi, text = it.tilaKoodiKuvaus) }

        fun createOkResponse() = SfiResponse(
            code = MSG_SENDING_SUCCESSFUL,
            text = "Asia tallennettuna asiointitilipalvelun käsittelyjonoon," +
                " mutta se ei vielä näy asiakkaan asiointitilillä."
        )
    }
}

class IgnoreSpecificErrorsHandler : SfiErrorResponseHandler {
    override fun handleError(response: SfiResponse) = response.let {
        if (it.code == ERROR_VALIDATION && it.text == "Asian tietosisällössä virheitä. Viranomaistunnisteella löytyy jo asia, joka on tallennettu asiakkaan tilille Viestit-palveluun") {
            logger.info {
                "SFI message delivery failed with ${it.code}: ${it.text}. Skipping duplicate message"
            }
        } else if (it.code == ERROR_SFI_ACCOUNT_NOT_FOUND) {
            logger.info {
                "SFI message delivery failed with ${it.code}: ${it.text}. " +
                    "This is to be expected when the recipient does not have an SFI account."
            }
        } else {
            throwWithCodeAndText(response)
        }
    }
}

private fun throwWithCodeAndText(response: SfiResponse): Nothing =
    throw SFiMessageDeliveryException("SFI message delivery failed with code ${response.code}: ${response.text}")
