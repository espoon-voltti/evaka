// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.template

import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.domain.OfficialLanguage

interface ITemplateProvider {
    fun getLocalizedFilename(
        type: DecisionType,
        lang: OfficialLanguage
    ): String

    fun getFeeDecisionPath(): String

    fun getVoucherValueDecisionPath(): String

    fun getClubDecisionPath(): String

    fun getDaycareVoucherDecisionPath(): String

    fun getDaycareTransferDecisionPath(): String

    fun getDaycareDecisionPath(): String

    fun getPreschoolDecisionPath(): String

    fun getPreparatoryDecisionPath(): String

    fun getAssistanceNeedDecisionPath(): String

    fun getAssistanceNeedPreschoolDecisionPath(): String
}
