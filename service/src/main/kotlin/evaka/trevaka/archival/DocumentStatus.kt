// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival

import evaka.core.decision.Decision
import evaka.core.decision.DecisionStatus
import evaka.core.document.childdocument.ChildDocumentDecisionStatus
import evaka.core.document.childdocument.ChildDocumentDetails
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus

fun status(decision: Decision): String =
    when (decision.status) {
        DecisionStatus.ACCEPTED -> "Hyväksytty"
        DecisionStatus.REJECTED -> "Hylätty"
        else -> error("Decision with status ${decision.status} cannot be archived")
    }

fun status(childDocumentDetails: ChildDocumentDetails): String {
    val decision = childDocumentDetails.decision
    return if (decision != null) {
        when (decision.status) {
            ChildDocumentDecisionStatus.ACCEPTED -> "Hyväksytty"
            ChildDocumentDecisionStatus.ANNULLED -> "Mitätöity"
            ChildDocumentDecisionStatus.REJECTED -> "Hylätty"
        }
    } else {
        when (childDocumentDetails.status) {
            DocumentStatus.COMPLETED -> "Valmis"
            else ->
                error(
                    "Child document with status ${childDocumentDetails.status} cannot be archived"
                )
        }
    }
}

fun status(feeDecision: FeeDecisionDetailed) =
    when (feeDecision.status) {
        FeeDecisionStatus.SENT -> "Lähetetty"
        FeeDecisionStatus.ANNULLED -> "Mitätöity"
        else -> error("Fee decision with status ${feeDecision.status} cannot be archived")
    }

fun status(voucherValueDecision: VoucherValueDecisionDetailed) =
    when (voucherValueDecision.status) {
        VoucherValueDecisionStatus.SENT -> "Lähetetty"
        VoucherValueDecisionStatus.ANNULLED -> "Mitätöity"
        else ->
            error(
                "Voucher value decision with status ${voucherValueDecision.status} cannot be archived"
            )
    }
