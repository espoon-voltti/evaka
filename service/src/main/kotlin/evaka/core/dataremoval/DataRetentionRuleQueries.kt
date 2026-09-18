// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.dataremoval.DateSource.Custom
import evaka.core.dataremoval.ExpirationRule.ArchivedIfRequired
import evaka.core.shared.db.Database
import java.time.LocalDate
import java.util.UUID

/**
 * The date the document's template defines: a number of days after the child's last placement
 * ended, or after the document's last status change
 */
val childDocumentRetentionEnd =
    Custom(mayHaveNoDate = true) { tx, ids ->
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
val childDocumentArchivedIfRequired = ArchivedIfRequired { tx, ids ->
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
 * A draft was never sent, so it expires a year after it was created. A sent statement expires ten
 * years after its period, whose end the citizen form caps at a year after the start, so a missing
 * end date counts as a year.
 */
val incomeStatementRetentionEnd =
    Custom(mayHaveNoDate = false) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT
  id,
  CASE WHEN status = 'DRAFT'
    THEN ((created_at AT TIME ZONE 'Europe/Helsinki')::date + interval '1 year')::date
    ELSE (coalesce(end_date, start_date + interval '1 year') + interval '10 years')::date
  END AS date
FROM income_statement
WHERE id = ANY(${bind(ids)})
"""
                )
            }
            .toDatesById()
    }

/**
 * The end of the last placement among the children whose fee decisions the parentship can affect:
 * the children of the same head of child during the parentship, and the children of the head's
 * partners on the days when the partnership, this parentship and the partner's parentship overlap
 */
val fridgeChildRelevantPlacementEnd =
    Custom(mayHaveNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT fc.id, max(p.end_date) AS date
FROM fridge_child fc
JOIN LATERAL (
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
) relevant ON true
JOIN placement p ON p.child_id = relevant.child_id
WHERE fc.id = ANY(${bind(ids)})
GROUP BY fc.id
"""
                )
            }
            .toDatesById()
    }

/** The end of the last placement among the children of either partner during the partnership */
val fridgePartnerRelevantPlacementEnd =
    Custom(mayHaveNoDate = true) { tx, ids ->
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

/** The end of the last placement among the children on the fee decision */
val feeDecisionChildrenPlacementEnd =
    Custom(mayHaveNoDate = true) { tx, ids ->
        tx.createQuery {
                sql(
                    """
SELECT fdc.fee_decision_id AS id, max(p.end_date) AS date
FROM fee_decision_child fdc
JOIN placement p ON p.child_id = fdc.child_id
WHERE fdc.fee_decision_id = ANY(${bind(ids)})
GROUP BY fdc.fee_decision_id
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
