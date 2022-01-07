// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Transaction.deletePlacementPlans(applicationIds: List<ApplicationId>) {
    execute(
        "DELETE FROM placement_plan WHERE application_id = ANY(?)",
        applicationIds.toTypedArray()
    )
}

fun Database.Transaction.softDeletePlacementPlanIfUnused(applicationId: ApplicationId) {
    createUpdate(
        // language=SQL
        """
UPDATE placement_plan
SET deleted = true
WHERE application_id = :applicationId
AND NOT EXISTS (
  SELECT 1
  FROM decision
  WHERE application_id = :applicationId
  AND status = 'PENDING'
)"""
    ).bind("applicationId", applicationId).execute()
}

fun Database.Transaction.createPlacementPlan(
    applicationId: ApplicationId,
    type: PlacementType,
    plan: DaycarePlacementPlan
) {
    createUpdate(
        // language=SQL
        """
INSERT INTO placement_plan (type, unit_id, application_id, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date)
VALUES (
    :type,
    :unitId,
    :applicationId,
    :startDate,
    :endDate,
    :preschoolDaycareStartDate,
    :preschoolDaycareEndDate
)"""
    )
        .bind("type", type)
        .bind("unitId", plan.unitId)
        .bind("applicationId", applicationId)
        .bind("startDate", plan.period.start)
        .bind("endDate", plan.period.end)
        .bind("preschoolDaycareStartDate", plan.preschoolDaycarePeriod?.start)
        .bind("preschoolDaycareEndDate", plan.preschoolDaycarePeriod?.end)
        .execute()
}

fun Database.Read.getPlacementPlan(applicationId: ApplicationId): PlacementPlan? {
    data class QueryResult(
        val id: PlacementPlanId,
        val unitId: DaycareId,
        val applicationId: ApplicationId,
        val type: PlacementType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val preschoolDaycareStartDate: LocalDate?,
        val preschoolDaycareEndDate: LocalDate?
    )
    return createQuery(
        // language=SQL
        """
SELECT id, unit_id, application_id, type, start_date, end_date, preschool_daycare_start_date, preschool_daycare_end_date
FROM placement_plan
WHERE application_id = :applicationId AND deleted = false
    """
    ).bind("applicationId", applicationId)
        .mapTo<QueryResult>()
        .findOne()
        .orElse(null)
        ?.let {
            PlacementPlan(
                id = it.id,
                unitId = it.unitId,
                applicationId = it.applicationId,
                type = it.type,
                period = FiniteDateRange(it.startDate, it.endDate),
                preschoolDaycarePeriod = if (it.preschoolDaycareStartDate != null && it.preschoolDaycareEndDate != null) {
                    FiniteDateRange(it.preschoolDaycareStartDate, it.preschoolDaycareEndDate)
                } else null
            )
        }
}

fun Database.Read.getPlacementPlanUnitName(applicationId: ApplicationId): String {
    return createQuery(
        // language=SQL
        """
SELECT d.name
FROM placement_plan
JOIN daycare d ON d.id = placement_plan.unit_id
WHERE application_id = :applicationId AND deleted = false
    """
    ).bind("applicationId", applicationId)
        .mapTo<String>()
        .findOne()
        .orElseThrow { NotFound("Placement plan for application $applicationId not found") }
}

fun Database.Read.getPlacementPlans(
    today: LocalDate,
    unitId: DaycareId,
    from: LocalDate?,
    to: LocalDate?,
    statuses: List<ApplicationStatus> = ApplicationStatus.values().asList()
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
        val rejectedByCitizen: Boolean
    )

    return createQuery(
        // language=SQL
        """
SELECT 
    pp.id, pp.unit_id, pp.application_id, pp.type, pp.start_date, pp.end_date, pp.preschool_daycare_start_date, pp.preschool_daycare_end_date,
    pp.unit_confirmation_status, pp.unit_reject_reason, pp.unit_reject_other_reason,
    p.id as child_id, p.first_name, p.last_name, p.date_of_birth, (d.status = 'REJECTED'::decision_status AND d.updated > :today - interval '2 week') AS rejected_by_citizen
FROM placement_plan pp
LEFT JOIN application a ON pp.application_id = a.id
LEFT JOIN person p ON a.child_id = p.id
LEFT JOIN decision d ON d.application_id = a.id
WHERE
    pp.unit_id = :unitId AND
    a.status = ANY(:statuses::application_status_type[]) AND
    (
        (a.status != 'REJECTED'::application_status_type AND pp.deleted = false) OR
        (
            d.status = 'REJECTED'::decision_status AND 
            a.status = 'REJECTED'::application_status_type AND
            d.updated > :today - interval '2 week'
        )
    ) AND
    pp.unit_confirmation_status != 'REJECTED'::confirmation_status
    ${if (to != null) " AND (start_date <= :to OR preschool_daycare_start_date <= :to)" else ""}
    ${if (from != null) " AND (end_date >= :from OR preschool_daycare_end_date >= :from)" else ""} 
    """
    ).bind("unitId", unitId)
        .bind("today", today)
        .bind("from", from)
        .bind("to", to)
        .bind("statuses", statuses.toTypedArray())
        .mapTo<QueryResult>()
        .list()
        .map {
            PlacementPlanDetails(
                id = it.id,
                unitId = it.unitId,
                applicationId = it.applicationId,
                type = it.type,
                period = FiniteDateRange(it.startDate, it.endDate),
                preschoolDaycarePeriod = if (it.preschoolDaycareStartDate != null && it.preschoolDaycareEndDate != null)
                    FiniteDateRange(it.preschoolDaycareStartDate, it.preschoolDaycareEndDate)
                else null,
                child = PlacementPlanChild(
                    id = it.childId,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    dateOfBirth = it.dateOfBirth
                ),
                unitConfirmationStatus = it.unitConfirmationStatus,
                unitRejectReason = it.unitRejectReason,
                unitRejectOtherReason = it.unitRejectOtherReason,
                rejectedByCitizen = it.rejectedByCitizen
            )
        }
}

fun Database.Transaction.updatePlacementPlanUnitConfirmation(
    applicationId: ApplicationId,
    status: PlacementPlanConfirmationStatus,
    rejectReason: PlacementPlanRejectReason?,
    rejectOtherReason: String?
) {
    // language=SQL
    val sql =
        """
        UPDATE placement_plan
        SET unit_confirmation_status = :status, unit_reject_reason = :rejectReason, unit_reject_other_reason = :rejectOtherReason
        WHERE application_id = :applicationId AND deleted = false
    """
    createUpdate(sql)
        .bind("applicationId", applicationId)
        .bind("status", status)
        .bind("rejectReason", rejectReason)
        .bind("rejectOtherReason", rejectOtherReason)
        .execute()
}

fun Database.Read.getPlacementDraftChild(childId: ChildId): PlacementDraftChild? {
    return createQuery(
        // language=SQL
        """
            SELECT id, first_name, last_name, date_of_birth AS dob
            FROM person
            WHERE id = :id
        """
    )
        .bind("id", childId)
        .mapTo<PlacementDraftChild>()
        .list()
        .singleOrNull()
}

fun Database.Read.getGuardiansRestrictedStatus(guardianId: PersonId): Boolean? {
    return createQuery(
        // language=SQL
        """
            SELECT restricted_details_enabled
            FROM person
            WHERE id = :id
        """
    )
        .bind("id", guardianId)
        .mapTo<Boolean>()
        .toList()
        .singleOrNull()
}

fun Database.Transaction.terminatePlacementFrom(terminationRequestedDate: LocalDate, placementId: PlacementId, placementTerminationDate: LocalDate, terminatedBy: EvakaUserId?) {
    createUpdate(
        //language=SQL
        """
UPDATE daycare_group_placement 
SET end_date = :placementTerminationDate 
WHERE daycare_placement_id = :placementId
        """.trimIndent()
    )
        .bind("placementId", placementId)
        .bind("placementTerminationDate", placementTerminationDate)
        .execute()

    createUpdate(
        //language=SQL
        """
DELETE FROM service_need 
WHERE placement_id = :placementId
    AND start_date > :placementTerminationDate
        """.trimIndent()
    )
        .bind("placementId", placementId)
        .bind("placementTerminationDate", placementTerminationDate)
        .execute()

    createUpdate(
        //language=SQL
        """
UPDATE service_need 
SET end_date = :placementTerminationDate 
WHERE placement_id = :placementId
    AND daterange(start_date, end_date, '[]') @> :placementTerminationDate::date
        """.trimIndent()
    )
        .bind("placementId", placementId)
        .bind("placementTerminationDate", placementTerminationDate)
        .execute()

    createUpdate(
        """
UPDATE placement
SET termination_requested_date = :terminationRequestedDate,
    terminated_by = :terminationRequestedBy,
    end_date = :placementTerminationDate
WHERE id = :placementId
        """.trimIndent()
    )
        .bindNullable("terminationRequestedDate", if (terminatedBy == null) null else terminationRequestedDate)
        .bindNullable("terminationRequestedBy", terminatedBy)
        .bind("placementTerminationDate", placementTerminationDate)
        .bind("placementId", placementId)
        .execute()
}
