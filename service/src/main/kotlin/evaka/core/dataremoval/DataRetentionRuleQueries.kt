// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.application.ApplicationStatus
import evaka.core.dataremoval.ExpirationRule.NotWhile
import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import java.time.LocalDate
import java.util.UUID

/**
 * The date the document's template defines: a number of days after the child's last placement
 * ended, or after the document's last status change
 */
val childDocumentRetentionEnd =
    DateSource.Custom(mayReturnNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT
  cd.id,
  CASE dt.deletion_retention_basis
    WHEN 'STATUS_TRANSITION' THEN
      (cd.status_modified_at AT TIME ZONE 'Europe/Helsinki')::date + dt.deletion_retention_days
    WHEN 'PLACEMENT_END' THEN
      (SELECT max(p.end_date) FROM placement p WHERE p.child_id = cd.child_id) + dt.deletion_retention_days
  END AS date
FROM child_document cd
JOIN document_template dt ON dt.id = cd.template_id
WHERE cd.id = ANY(${bind(ids)})
"""
                )
            }
            .toDatesById()
    }

/** A document whose template is archived externally waits until it has been archived */
val notWhileChildDocumentAwaitingArchival =
    NotWhile("the document awaits archival") { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT cd.id
FROM child_document cd
JOIN document_template dt ON dt.id = cd.template_id
WHERE cd.id = ANY(${bind(ids)}) AND cd.status <> 'DRAFT' AND dt.archive_externally AND cd.archived_at IS NULL
"""
                )
            }
            .toSet<UUID>()
    }

/**
 * A draft expires a year after it was created. A sent statement that a fee decision of its person
 * overlaps, other than an ignored one, expires ten years after its period, and a missing end date
 * counts as a year after the start. Any other sent statement expires a year after it was sent.
 */
val incomeStatementRetentionEnd =
    DateSource.Custom(mayReturnNoDate = false) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT
  s.id,
  CASE
    WHEN s.status = 'DRAFT' THEN
      ((s.created_at AT TIME ZONE 'Europe/Helsinki')::date + interval '1 year')::date
    WHEN EXISTS (
      SELECT FROM fee_decision fd
      WHERE (fd.head_of_family_id = s.person_id OR fd.partner_id = s.person_id)
        AND fd.status <> 'IGNORED'
        AND fd.valid_during && daterange(s.start_date, s.end_date, '[]')
    ) OR EXISTS (
      SELECT FROM fee_decision_child fdc
      JOIN fee_decision fd ON fd.id = fdc.fee_decision_id
      WHERE fdc.child_id = s.person_id
        AND fd.status <> 'IGNORED'
        AND fd.valid_during && daterange(s.start_date, s.end_date, '[]')
    ) THEN
      (coalesce(s.end_date, s.start_date + interval '1 year') + interval '10 years')::date
    ELSE ((s.sent_at AT TIME ZONE 'Europe/Helsinki')::date + interval '1 year')::date
  END AS date
FROM income_statement s
WHERE s.id = ANY(${bind(ids)})
"""
                )
            }
            .toDatesById()
    }

val notWhileIncomeStatementAwaitingHandling =
    NotWhile("the statement awaits handling") { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT id
FROM income_statement
WHERE id = ANY(${bind(ids)}) AND status IN ('SENT', 'HANDLING')
"""
                )
            }
            .toSet<UUID>()
    }

/** Expects the surrounding query to name the parentship `fc` */
private val fridgeChildRelevantChildren = QuerySql {
    sql(
        """
SELECT own.child_id
FROM fridge_child own
WHERE own.head_of_child = fc.head_of_child
  AND daterange(own.start_date, own.end_date, '[]') && daterange(fc.start_date, fc.end_date, '[]')

UNION

SELECT theirs.child_id
FROM fridge_partner fp
JOIN fridge_partner partner ON partner.partnership_id = fp.partnership_id AND partner.indx <> fp.indx
JOIN fridge_child theirs ON theirs.head_of_child = partner.person_id
WHERE fp.person_id = fc.head_of_child
  AND NOT isempty(
    daterange(fc.start_date, fc.end_date, '[]')
    * daterange(fp.start_date, fp.end_date, '[]')
    * daterange(theirs.start_date, theirs.end_date, '[]')
  )
"""
    )
}

/**
 * The end of the last placement among the children whose fee decisions the parentship can affect:
 * the children of the same head of child during the parentship, and the children of the head's
 * partners on the days when the partnership, this parentship and the partner's parentship overlap
 */
val fridgeChildRelevantPlacementEnd =
    DateSource.Custom(mayReturnNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT fc.id, max(p.end_date) AS date
FROM fridge_child fc
JOIN LATERAL (${subquery(fridgeChildRelevantChildren)}) relevant ON true
JOIN placement p ON p.child_id = relevant.child_id
WHERE fc.id = ANY(${bind(ids)})
GROUP BY fc.id
"""
                )
            }
            .toDatesById()
    }

val notWhileFridgeChildRelevantApplicationPending =
    NotWhile("a child the parentship affects has a pending application") { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT DISTINCT fc.id
FROM fridge_child fc
JOIN LATERAL (${subquery(fridgeChildRelevantChildren)}) relevant ON true
JOIN application a ON a.child_id = relevant.child_id AND a.status = ANY(${bind(ApplicationStatus.pending)})
WHERE fc.id = ANY(${bind(ids)})
"""
                )
            }
            .toSet<UUID>()
    }

/** The end of the last placement among the children of either partner during the partnership */
val fridgePartnerRelevantPlacementEnd =
    DateSource.Custom(mayReturnNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT fp.partnership_id AS id, max(p.end_date) AS date
FROM fridge_partner fp
JOIN fridge_child fc
  ON fc.head_of_child = fp.person_id
  AND daterange(fc.start_date, fc.end_date, '[]') && daterange(fp.start_date, fp.end_date, '[]')
JOIN placement p ON p.child_id = fc.child_id
WHERE fp.partnership_id = ANY(${bind(ids)})
GROUP BY fp.partnership_id
"""
                )
            }
            .toDatesById()
    }

val notWhileFridgePartnerRelevantApplicationPending =
    NotWhile("a child the partnership affects has a pending application") { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT DISTINCT fp.partnership_id
FROM fridge_partner fp
JOIN fridge_child fc
  ON fc.head_of_child = fp.person_id
  AND daterange(fc.start_date, fc.end_date, '[]') && daterange(fp.start_date, fp.end_date, '[]')
JOIN application a ON a.child_id = fc.child_id AND a.status = ANY(${bind(ApplicationStatus.pending)})
WHERE fp.partnership_id = ANY(${bind(ids)})
"""
                )
            }
            .toSet<UUID>()
    }

val notWhileApplicationPending =
    NotWhile("the application is pending") { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT id
FROM application
WHERE id = ANY(${bind(ids)}) AND status = ANY(${bind(ApplicationStatus.pending)})
"""
                )
            }
            .toSet<UUID>()
    }

/**
 * The end of the last placement among the children on the fee decision, or the end of the decision
 * when none of them has placements
 */
val feeDecisionChildrenPlacementOrValidityEnd =
    DateSource.Custom(mayReturnNoDate = false) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT fd.id, coalesce(max(p.end_date), upper(fd.valid_during) - 1) AS date
FROM fee_decision fd
LEFT JOIN fee_decision_child fdc ON fdc.fee_decision_id = fd.id
LEFT JOIN placement p ON p.child_id = fdc.child_id
WHERE fd.id = ANY(${bind(ids)})
GROUP BY fd.id
"""
                )
            }
            .toDatesById()
    }

/**
 * The end of the last placement among the children on the invoice rows, or the end of the invoice
 * period when none of them has placements
 */
val invoiceChildrenPlacementOrPeriodEnd =
    DateSource.Custom(mayReturnNoDate = false) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT i.id, coalesce(max(p.end_date), i.period_end) AS date
FROM invoice i
LEFT JOIN invoice_row ir ON ir.invoice_id = i.id
LEFT JOIN placement p ON p.child_id = ir.child
WHERE i.id = ANY(${bind(ids)})
GROUP BY i.id
"""
                )
            }
            .toDatesById()
    }

/**
 * The end of the last placement of the child the correction is for, or the end of the correction
 * period when the child has no placements
 */
val invoiceCorrectionChildPlacementOrPeriodEnd =
    DateSource.Custom(mayReturnNoDate = false) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT ic.id, coalesce(max(p.end_date), upper(ic.period) - 1) AS date
FROM invoice_correction ic
LEFT JOIN placement p ON p.child_id = ic.child_id
WHERE ic.id = ANY(${bind(ids)})
GROUP BY ic.id
"""
                )
            }
            .toDatesById()
    }

/**
 * The end of the latest fee or voucher value decision naming the income's person as the head of
 * family, the partner or a child
 */
val incomePersonLastFinanceDecisionEnd =
    DateSource.Custom(mayReturnNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT i.id, max(decision.end_date) AS date
FROM income i
JOIN LATERAL (
  SELECT upper(fd.valid_during) - 1 AS end_date
  FROM fee_decision fd
  WHERE fd.head_of_family_id = i.person_id OR fd.partner_id = i.person_id

  UNION ALL

  SELECT upper(fd.valid_during) - 1
  FROM fee_decision_child fdc
  JOIN fee_decision fd ON fd.id = fdc.fee_decision_id
  WHERE fdc.child_id = i.person_id

  UNION ALL

  SELECT vvd.valid_to
  FROM voucher_value_decision vvd
  WHERE vvd.head_of_family_id = i.person_id OR vvd.partner_id = i.person_id OR vvd.child_id = i.person_id
) decision ON true
WHERE i.id = ANY(${bind(ids)})
GROUP BY i.id
"""
                )
            }
            .toDatesById()
    }

/** The rows without a date are left out, so that the fallback of the rule decides for them */
private fun Database.Query.toDatesById(): Map<UUID, LocalDate> = toList {
    column<UUID>("id") to column<LocalDate?>("date")
}
    .mapNotNull { (id, date) -> date?.let { id to it } }
    .toMap()
