// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.message

import fi.espoo.evaka.decision.DecisionSendAddress

interface IMessageProvider {
    fun getDecisionHeader(lang: MessageLanguage): String
    fun getDecisionContent(lang: MessageLanguage): String
    fun getFeeDecisionHeader(lang: MessageLanguage): String
    fun getFeeDecisionContent(lang: MessageLanguage): String
    fun getVoucherValueDecisionHeader(lang: MessageLanguage): String
    fun getVoucherValueDecisionContent(lang: MessageLanguage): String

    /**
     * Returns address used for decisions when person has restricted details enabled or missing address.
     */
    fun getDefaultDecisionAddress(lang: MessageLanguage): DecisionSendAddress
}

enum class MessageLanguage {
    FI,
    SV,
}

fun langWithDefault(lang: String): MessageLanguage =
    if (lang.toLowerCase() == "sv") MessageLanguage.SV else MessageLanguage.FI
