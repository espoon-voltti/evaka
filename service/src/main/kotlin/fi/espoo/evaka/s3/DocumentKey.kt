// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.s3

import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.domain.OfficialLanguage

sealed interface DocumentKey {
    val value: String

    data class AssistanceNeedDecision(override val value: String) : DocumentKey {
        constructor(
            id: AssistanceNeedDecisionId
        ) : this("assistance-need-decisions/assistance_need_decision_$id.pdf")
    }

    data class AssistanceNeedPreschoolDecision(override val value: String) : DocumentKey {
        constructor(
            id: AssistanceNeedPreschoolDecisionId
        ) : this("assistance-need-preschool-decisions/assistance_need_preschool_decision_$id.pdf")
    }

    data class Attachment(override val value: String) : DocumentKey {
        constructor(id: AttachmentId) : this(id.toString())
    }

    data class ChildDocument(override val value: String) : DocumentKey {
        constructor(id: ChildDocumentId) : this("child-documents/child_document_$id.pdf")
    }

    data class ChildImage(override val value: String) : DocumentKey {
        constructor(id: ChildImageId) : this("child-images/$id")
    }

    data class Decision(override val value: String) : DocumentKey {
        constructor(
            id: DecisionId,
            type: DecisionType,
            lang: OfficialLanguage,
        ) : this(
            when (type) {
                DecisionType.CLUB -> "clubdecision"
                DecisionType.DAYCARE,
                DecisionType.DAYCARE_PART_TIME -> "daycaredecision"
                DecisionType.PRESCHOOL -> "preschooldecision"
                DecisionType.PRESCHOOL_DAYCARE,
                DecisionType.PRESCHOOL_CLUB -> "connectingdaycaredecision"
                DecisionType.PREPARATORY_EDUCATION -> "preparatorydecision"
            }.let { "${it}_${id}_$lang" }
        )
    }

    data class FeeDecision(override val value: String) : DocumentKey {
        constructor(
            id: FeeDecisionId,
            lang: OfficialLanguage,
        ) : this("feedecision_${id}_${lang.isoLanguage.alpha2}.pdf")
    }

    data class VoucherValueDecision(override val value: String) : DocumentKey {
        constructor(id: VoucherValueDecisionId) : this("value_decision_$id.pdf")
    }
}
