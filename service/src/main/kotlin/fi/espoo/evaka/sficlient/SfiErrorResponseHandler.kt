// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.sficlient.soap.LahetaViestiResponse

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
