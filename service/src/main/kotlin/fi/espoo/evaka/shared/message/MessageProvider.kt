// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.message

interface IMessageProvider {
    fun get(type: MessageType, lang: MessageLanguage): String
}

enum class MessageType {
    DECISION_HEADER,
    DECISION_CONTENT,
    FEE_DECISION_HEADER,
    FEE_DECISION_CONTENT,
    VOUCHER_VALUE_DECISION_HEADER,
    VOUCHER_VALUE_DECISION_CONTENT,
}

enum class MessageLanguage {
    FI,
    SV,
}

fun langWithDefault(lang: String): MessageLanguage =
    if (lang.toLowerCase() == "sv") MessageLanguage.SV else MessageLanguage.FI
