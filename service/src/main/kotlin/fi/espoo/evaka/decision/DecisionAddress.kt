// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.MessageLanguage

private fun addressIsUnusable(streetAddress: String?, postalCode: String?, postOffice: String?): Boolean {
    return streetAddress.isNullOrBlank() || postalCode.isNullOrBlank() || postOffice.isNullOrBlank()
}

fun getSendAddress(messageProvider: IMessageProvider, guardian: PersonDTO, lang: String): DecisionSendAddress {
    val logMissingAddress = {
        logger.warn("Cannot deliver daycare decision to guardian ${guardian.id} with incomplete address. Using default decision address.")
    }
    return when {
        guardian.restrictedDetailsEnabled -> when (lang) {
            "sv" -> messageProvider.getDefaultDecisionAddress(MessageLanguage.SV)
            else -> messageProvider.getDefaultDecisionAddress(MessageLanguage.FI)
        }
        addressIsUnusable(guardian.streetAddress, guardian.postalCode, guardian.postOffice) -> {
            logMissingAddress()
            when (lang) {
                "sv" -> messageProvider.getDefaultDecisionAddress(MessageLanguage.SV)
                else -> messageProvider.getDefaultDecisionAddress(MessageLanguage.FI)
            }
        }
        else -> DecisionSendAddress(
            street = guardian.streetAddress!!,
            postalCode = guardian.postalCode!!,
            postOffice = guardian.postOffice!!,
            row1 = guardian.streetAddress,
            row2 = "${guardian.postalCode} ${guardian.postOffice}",
            row3 = ""
        )
    }
}

data class DecisionSendAddress(
    val street: String,
    val postalCode: String,
    val postOffice: String,
    val row1: String,
    val row2: String,
    val row3: String
)
