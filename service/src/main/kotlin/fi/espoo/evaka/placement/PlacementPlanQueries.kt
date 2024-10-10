// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

fun Database.Transaction.deletePlacementPlans(applicationIds: List<ApplicationId>) {
    execute {
        sql("DELETE FROM placement_plan WHERE application_id = ANY(${bind(applicationIds)})")
    }
}

fun Database.Transaction.softDeletePlacementPlanIfUnused(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    applicationId: ApplicationId,
) {
    createUpdate {
            sql(
                """
UPDATE placement_plan
SET deleted = true, modified_at = ${bind(now)}, modified_by = ${bind(user.evakaUserId)}
WHERE application_id = ${bind(applicationId)}
AND NOT EXISTS (
  SELECT 1
  FROM decision
  WHERE application_id = ${bind(applicationId)}
  AND status = 'PENDING'
)
"""
            )
        }
        .execute()
}

fun Database.Transaction.createPlacementPlan(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    applicationId: ApplicationId,
    type: PlacementType,
    plan: DaycarePlacementPlan,
): PlacementPlanId =
    createUpdate {
            sql(
                """
INSERT INTO placement_plan (
    type,
    unit_id,
    application_id,
    start_date,
    end_date,
    preschool_daycare_start_date,
    preschool_daycare_end_date,
    modified_at,
    modified_by 
) VALUES (
    ${bind(type)},
    ${bind(plan.unitId)},
    ${bind(applicationId)},
    ${bind(plan.period.start)},
    ${bind(plan.period.end)},
    ${bind(plan.preschoolDaycarePeriod?.start)},
    ${bind(plan.preschoolDaycarePeriod?.end)},
    ${bind(now)},
    ${bind(user.evakaUserId)}
)
RETURNING id"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<PlacementPlanId>()

fun Database.Read.getPlacementPlan(applicationId: ApplicationId): PlacementPlan? {
    data class QueryResult(
        val id: PlacementPlanId,
        val unitId: DaycareId,
        val applicationId: ApplicationId,
        val type: PlacementType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val preschoolDaycareStartDate: LocalDate?,
        val preschoolDaycareEndDate: LocalDate?,
        val modifiedAt: HelsinkiDateTime,
        @Nested("modified_by") val modifiedBy: EvakaUser?,
    )
    return createQuery {
            sql(
                """
SELECT
    p.id,
    p.unit_id,
    p.application_id,
    p.type,
    p.start_date,
    p.end_date,
    p.preschool_daycare_start_date,
    p.preschool_daycare_end_date,
    p.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type
FROM placement_plan p LEFT JOIN evaka_user e ON p.modified_by = e.id
WHERE application_id = ${bind(applicationId)} AND deleted = false
    """
            )
        }
        .exactlyOneOrNull<QueryResult>()
        ?.let {
            PlacementPlan(
                id = it.id,
                unitId = it.unitId,
                applicationId = it.applicationId,
                type = it.type,
                period = FiniteDateRange(it.startDate, it.endDate),
                preschoolDaycarePeriod =
                    if (
                        it.preschoolDaycareStartDate != null && it.preschoolDaycareEndDate != null
                    ) {
                        FiniteDateRange(it.preschoolDaycareStartDate, it.preschoolDaycareEndDate)
                    } else {
                        null
                    },
                modifiedAt = it.modifiedAt,
                modifiedBy = it.modifiedBy,
            )
        }
}

fun Database.Read.getPlacementPlanUnitName(applicationId: ApplicationId): String {
    return createQuery {
            sql(
                """
SELECT d.name
FROM placement_plan
JOIN daycare d ON d.id = placement_plan.unit_id
WHERE application_id = ${bind(applicationId)} AND deleted = false
"""
            )
        }
        .exactlyOneOrNull<String>()
        ?: throw NotFound("Placement plan for application $applicationId not found")
}

fun Database.Read.getPlacementPlans(
    today: LocalDate,
    unitId: DaycareId,
    from: LocalDate?,
    to: LocalDate?,
    statuses: List<ApplicationStatus> = ApplicationStatus.values().asList(),
): List<PlacementPlanDetails> {
    data class QueryResult(
        val id: PlacementPlanId,
        val unitId: DaycareId,
        val applicationId: ApplicationId,
        val type: PlacementType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val preschoolDaycareStartDate: LocalDate?,
        val preschoolDaycareEndDate: LocalDate?,
        val childId: ChildId,
        val firstName: String,
        val lastName: String,
        val dateOfBirth: LocalDate,
        val unitConfirmationStatus: PlacementPlanConfirmationStatus,
        val unitRejectReason: PlacementPlanRejectReason?,
        val unitRejectOtherReason: String?,
        val rejectedByCitizen: Boolean,
        val modifiedAt: HelsinkiDateTime,
        @Nested("modified_by") val modifiedBy: EvakaUser?,
    )

    return createQuery {
            sql(
                """
SELECT
    pp.id, pp.unit_id, pp.application_id, pp.type, pp.start_date, pp.end_date, pp.preschool_daycare_start_date, pp.preschool_daycare_end_date,
    pp.unit_confirmation_status, pp.unit_reject_reason, pp.unit_reject_other_reason,
    p.id as child_id, p.first_name, p.last_name, p.date_of_birth, d.resolved IS NOT NULL AS rejected_by_citizen,
    pp.modified_at, e.id AS modified_by_id, e.name AS modified_by_name, e.type AS modified_by_type
FROM placement_plan pp
LEFT JOIN application a ON pp.application_id = a.id
LEFT JOIN person p ON a.child_id = p.id
LEFT JOIN evaka_user e ON pp.modified_by = e.id
LEFT JOIN LATERAL (
 SELECT min(d.resolved) AS resolved
 FROM decision d
 WHERE d.application_id = a.id
 AND a.status = 'REJECTED'::application_status_type
 AND d.status = 'REJECTED'::decision_status
) d ON TRUE
WHERE
    pp.unit_id = ${bind(unitId)} AND
    a.status = ANY(${bind(statuses)}::application_status_type[]) AND
    (
        (a.status != 'REJECTED'::application_status_type AND pp.deleted = false) OR
        d.resolved > ${bind(today)} - interval '2 week'
    ) AND
    pp.unit_confirmation_status != 'REJECTED'::confirmation_status
    ${if (to != null) " AND (start_date <= ${bind(to)} OR preschool_daycare_start_date <= ${bind(to)})" else ""}
    ${if (from != null) " AND (end_date >= ${bind(from)} OR preschool_daycare_end_date >= ${bind(from)})" else ""}
"""
            )
        }
        .toList<QueryResult>()
        .map {
            PlacementPlanDetails(
                id = it.id,
                unitId = it.unitId,
                applicationId = it.applicationId,
                type = it.type,
                period = FiniteDateRange(it.startDate, it.endDate),
                preschoolDaycarePeriod =
                    if (
                        it.preschoolDaycareStartDate != null && it.preschoolDaycareEndDate != null
                    ) {
                        FiniteDateRange(it.preschoolDaycareStartDate, it.preschoolDaycareEndDate)
                    } else {
                        null
                    },
                child =
                    PlacementPlanChild(
                        id = it.childId,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        dateOfBirth = it.dateOfBirth,
                    ),
                unitConfirmationStatus = it.unitConfirmationStatus,
                unitRejectReason = it.unitRejectReason,
                unitRejectOtherReason = it.unitRejectOtherReason,
                rejectedByCitizen = it.rejectedByCitizen,
                modifiedAt = it.modifiedAt,
                modifiedBy = it.modifiedBy,
            )
        }
}

fun Database.Transaction.updatePlacementPlanUnitConfirmation(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    applicationId: ApplicationId,
    status: PlacementPlanConfirmationStatus,
    rejectReason: PlacementPlanRejectReason?,
    rejectOtherReason: String?,
) {
    createUpdate {
            sql(
                """
UPDATE placement_plan
SET unit_confirmation_status = ${bind(status)},
    unit_reject_reason = ${bind(rejectReason)},
    unit_reject_other_reason = ${bind(rejectOtherReason)},
    modified_at = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)}
WHERE application_id = ${bind(applicationId)} AND deleted = false
"""
            )
        }
        .execute()
}

fun Database.Read.getPlacementDraftChild(childId: ChildId): PlacementDraftChild? {
    return createQuery {
            sql(
                """
SELECT id, first_name, last_name, date_of_birth AS dob
FROM person
WHERE id = ${bind(childId)}
"""
            )
        }
        .exactlyOneOrNull<PlacementDraftChild>()
}

fun Database.Read.getGuardiansRestrictedStatus(guardianId: PersonId): Boolean? {
    return createQuery {
            sql(
                """
SELECT restricted_details_enabled
FROM person
WHERE id = ${bind(guardianId)}
"""
            )
        }
        .exactlyOneOrNull<Boolean>()
}

fun Database.Read.getWaitingUnitConfirmationApplicationsCount(unitId: DaycareId): Int {
    return createQuery {
            sql(
                """
SELECT COUNT(*)
FROM placement_plan pp
JOIN application a ON pp.application_id = a.id
WHERE unit_id = ${bind(unitId)} AND a.status = ${bind(ApplicationStatus.WAITING_UNIT_CONFIRMATION)}
AND pp.unit_confirmation_status != ${bind(PlacementPlanConfirmationStatus.REJECTED)}
"""
            )
        }
        .exactlyOne<Int>()
}
