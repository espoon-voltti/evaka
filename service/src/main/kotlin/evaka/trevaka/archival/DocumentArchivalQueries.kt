// SPDX-FileCopyrightText: 2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.archival

import evaka.core.shared.ChildDocumentId
import evaka.core.shared.DecisionId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate

fun Database.Read.getEligibleChildDocumentCount(
    documentPredicate: Predicate,
    templatePredicate: Predicate,
): Int =
    createQuery {
            sql(
                """
                SELECT count(cd.id)
                FROM child_document cd
                JOIN document_template dt ON cd.template_id = dt.id
                WHERE ${predicate(templatePredicate.forTable("dt").and(documentPredicate.forTable("cd")))}
                """
            )
        }
        .exactlyOne<Int>()

fun Database.Read.getChildDocumentsEligibleForArchival(
    documentPredicate: Predicate,
    templatePredicate: Predicate,
    limit: Int,
): List<ChildDocumentId> =
    createQuery {
            sql(
                """
                SELECT cd.id
                FROM child_document cd
                JOIN document_template dt ON cd.template_id = dt.id
                WHERE ${predicate(templatePredicate.forTable("dt").and(documentPredicate.forTable("cd")))}
                ORDER BY cd.created
                LIMIT $limit
                """
            )
        }
        .toList<ChildDocumentId>()

fun Database.Read.getEligibleDecisionCount(decision: Predicate): Int =
    createQuery {
            sql(
                """
                SELECT count(d.id)
                FROM decision d
                WHERE ${predicate(decision.forTable("d"))}
                """
            )
        }
        .exactlyOne<Int>()

fun Database.Read.getDecisionsEligibleForArchival(
    decision: Predicate,
    limit: Int,
): List<DecisionId> =
    createQuery {
            sql(
                """
                SELECT d.id
                FROM decision d
                WHERE ${predicate(decision.forTable("d"))}
                ORDER BY d.resolved
                LIMIT $limit
                """
            )
        }
        .toList<DecisionId>()

fun Database.Read.getEligibleFeeDecisionCount(feeDecision: Predicate): Int =
    createQuery {
            sql(
                """
                SELECT count(fd.id)
                FROM fee_decision fd
                WHERE ${predicate(feeDecision.forTable("fd"))}
                """
            )
        }
        .exactlyOne<Int>()

fun Database.Read.getFeeDecisionsEligibleForArchival(
    feeDecision: Predicate,
    limit: Int,
): List<FeeDecisionId> =
    createQuery {
            sql(
                """
                SELECT fd.id
                FROM fee_decision fd
                WHERE ${predicate(feeDecision.forTable("fd"))}
                ORDER BY fd.approved_at
                LIMIT $limit
                """
            )
        }
        .toList<FeeDecisionId>()

fun Database.Read.getEligibleVoucherDecisionCount(voucherDecision: Predicate): Int =
    createQuery {
            sql(
                """
                SELECT count(vvd.id)
                FROM voucher_value_decision vvd
                WHERE ${predicate(voucherDecision.forTable("vvd"))}
                """
            )
        }
        .exactlyOne<Int>()

fun Database.Read.getVoucherValueDecisionsEligibleForArchival(
    voucherDecision: Predicate,
    limit: Int,
): List<VoucherValueDecisionId> =
    createQuery {
            sql(
                """
                SELECT vvd.id
                FROM voucher_value_decision vvd                
                WHERE ${predicate(voucherDecision.forTable("vvd"))}
                ORDER BY vvd.approved_at
                LIMIT $limit
                """
            )
        }
        .toList<VoucherValueDecisionId>()
