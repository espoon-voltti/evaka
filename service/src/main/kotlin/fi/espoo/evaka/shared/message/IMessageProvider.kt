// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.message

import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.shared.domain.OfficialLanguage

interface IMessageProvider {
    fun getDecisionHeader(lang: OfficialLanguage): String

    fun getDecisionContent(lang: OfficialLanguage): String

    fun getFeeDecisionHeader(lang: OfficialLanguage): String

    fun getFeeDecisionContent(lang: OfficialLanguage): String

    fun getVoucherValueDecisionHeader(lang: OfficialLanguage): String

    fun getVoucherValueDecisionContent(lang: OfficialLanguage): String

    fun getAssistanceNeedDecisionHeader(lang: OfficialLanguage): String

    fun getAssistanceNeedDecisionContent(lang: OfficialLanguage): String

    fun getAssistanceNeedPreschoolDecisionHeader(lang: OfficialLanguage): String

    fun getAssistanceNeedPreschoolDecisionContent(lang: OfficialLanguage): String

    /**
     * Returns address used for decisions when person has restricted details enabled or missing
     * address.
     */
    fun getDefaultDecisionAddress(lang: OfficialLanguage): DecisionSendAddress

    /** Returns address used for fee decisions when person is missing address. */
    fun getDefaultFinancialDecisionAddress(lang: OfficialLanguage): DecisionSendAddress

    fun getPlacementToolHeader(lang: OfficialLanguage): String

    fun getPlacementToolContent(lang: OfficialLanguage): String
}
