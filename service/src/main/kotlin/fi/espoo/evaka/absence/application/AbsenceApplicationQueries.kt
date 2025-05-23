// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence.application

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Transaction.insertAbsenceApplication(
    application: AbsenceApplicationCreateRequest,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
) =
    createUpdate {
            sql(
                """
INSERT INTO absence_application (created_by, modified_at, modified_by, child_id, start_date, end_date, description, status)
VALUES (
    ${bind(createdBy)},
    ${bind(createdAt)},
    ${bind(createdBy)},
    ${bind(application.childId)},
    ${bind(application.startDate)},
    ${bind(application.endDate)},
    ${bind(application.description)},
    'WAITING_DECISION'
)
RETURNING id,
    created_at, created_by,
    updated_at, modified_at, modified_by,
    child_id, start_date, end_date, description, status,
    decided_at, decided_by, rejected_reason
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<AbsenceApplication>()

fun Database.Read.selectAbsenceApplications(
    unitId: DaycareId? = null,
    childId: ChildId? = null,
    status: AbsenceApplicationStatus? = null,
): List<AbsenceApplicationSummary> {
    val predicates =
        Predicate.allNotNull(
            unitId?.let {
                Predicate { table ->
                    where(
                        """
EXISTS (SELECT FROM placement
WHERE placement.unit_id = ${bind(it)}
  AND placement.child_id = absence_application.child_id
  AND placement.start_date <= absence_application.end_date
  AND placement.end_date >= absence_application.start_date
)
        """
                    )
                }
            },
            childId?.let { Predicate { table -> where("$table.child_id = ${bind(it)}") } },
            status?.let { Predicate { table -> where("$table.status = ${bind(it)}") } },
        )
    return absenceApplicationSummaryQuery(predicates).toList<AbsenceApplicationSummary>()
}

fun Database.Read.selectAbsenceApplication(id: AbsenceApplicationId, forUpdate: Boolean = false) =
    createQuery {
            sql(
                """
SELECT id,
    created_at, created_by,
    updated_at, modified_at, modified_by,
    child_id, start_date, end_date, description, status,
    decided_at, decided_by, rejected_reason
FROM absence_application
WHERE id = ${bind(id)}
${if (forUpdate) "FOR UPDATE" else ""}
"""
            )
        }
        .exactlyOneOrNull<AbsenceApplication>()

fun Database.Transaction.decideAbsenceApplication(
    id: AbsenceApplicationId,
    status: AbsenceApplicationStatus,
    decidedAt: HelsinkiDateTime,
    decidedBy: EvakaUserId,
    rejectedReason: String?,
) =
    createUpdate {
            sql(
                """
UPDATE absence_application SET
    modified_at = ${bind(decidedAt)},
    modified_by = ${bind(decidedBy)},
    status = ${bind(status)},
    decided_at = ${bind(decidedAt)},
    decided_by = ${bind(decidedBy)},
    rejected_reason = ${bind(rejectedReason)}
WHERE id = ${bind(id)}
  AND status = 'WAITING_DECISION'
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteAbsenceApplication(id: AbsenceApplicationId) =
    createUpdate {
            sql(
                """
DELETE FROM absence_application
WHERE id = ${bind(id)}
RETURNING id,
    created_at, created_by,
    updated_at, modified_at, modified_by,
    child_id, start_date, end_date, description, status,
    decided_at, decided_by, rejected_reason
    """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<AbsenceApplication>()

fun Database.Read.getChildrenWithAbsenceApplicationPossibleOnSomeDate(
    childIds: Set<ChildId>
): Set<ChildId> =
    createQuery {
            sql(
                """
SELECT DISTINCT child_id
FROM placement
WHERE type = ANY (${bind(PlacementType.preschool)})
  AND child_id = ANY (${bind(childIds)})
        """
            )
        }
        .toSet()

private fun Database.Read.absenceApplicationSummaryQuery(predicate: Predicate) = createQuery {
    sql(
        """
SELECT
    absence_application.id AS id,
    absence_application.created_at,
    created_by.id AS created_by_id,
    created_by.name AS created_by_name,
    created_by.type AS created_by_type,
    child.id AS child_id,
    child.first_name AS child_first_name,
    child.last_name AS child_last_name,
    absence_application.start_date,
    absence_application.end_date,
    absence_application.description,
    absence_application.status,
    absence_application.rejected_reason,
    absence_application.decided_at,
    decided_by.id AS decided_by_id,
    decided_by.name AS decided_by_name,
    decided_by.type AS decided_by_type
FROM absence_application
JOIN evaka_user created_by ON absence_application.created_by = created_by.id
JOIN person child ON absence_application.child_id = child.id
LEFT JOIN evaka_user decided_by ON absence_application.decided_by = decided_by.id
WHERE ${predicate(predicate.forTable("absence_application"))}
"""
    )
}
