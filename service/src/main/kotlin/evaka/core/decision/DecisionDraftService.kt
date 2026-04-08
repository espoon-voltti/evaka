// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.application.ApplicationDetails
import evaka.core.placement.PlacementPlan
import evaka.core.placement.PlacementType
import evaka.core.placement.getPlacementPlan
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID

private data class ExistingActiveDecision(val unitId: DaycareId, val type: DecisionType)

private fun Database.Read.getLatestAcceptedPreschoolDecision(
    childId: ChildId,
    date: LocalDate,
): ExistingActiveDecision? =
    createQuery {
            sql(
                """
SELECT d.unit_id, d.type
FROM decision d
JOIN application a ON d.application_id = a.id
WHERE a.child_id = ${bind(childId)}
  AND d.type = ANY(${bind(listOf(DecisionType.PRESCHOOL, DecisionType.PRESCHOOL_DAYCARE, DecisionType.PRESCHOOL_CLUB, DecisionType.PREPARATORY_EDUCATION))})
  AND d.status = ${bind(DecisionStatus.ACCEPTED)}
  AND daterange(d.start_date, d.end_date, '[]') @> ${bind(date)}
ORDER BY d.resolved DESC
LIMIT 1
"""
            )
        }
        .exactlyOneOrNull()

fun createDecisionDrafts(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    application: ApplicationDetails,
) {
    val placementPlan =
        tx.getPlacementPlan(application.id)
            ?: throw NotFound("Application ${application.id} has no placement")

    val drafts: List<DecisionDraft> =
        when (placementPlan.type) {
            PlacementType.CLUB -> {
                planClubDecisionDrafts(placementPlan)
            }

            PlacementType.DAYCARE,
            PlacementType.DAYCARE_PART_TIME,
            PlacementType.PRESCHOOL_DAYCARE_ONLY,
            PlacementType.PREPARATORY_DAYCARE_ONLY -> {
                planDaycareDecisionDrafts(placementPlan)
            }

            PlacementType.PRESCHOOL,
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PRESCHOOL_CLUB,
            PlacementType.PREPARATORY,
            PlacementType.PREPARATORY_DAYCARE -> {
                val existingDecision =
                    tx.getLatestAcceptedPreschoolDecision(
                        application.childId,
                        placementPlan.period.start,
                    )
                planPreschoolDecisionDrafts(placementPlan, existingDecision)
            }

            PlacementType.SCHOOL_SHIFT_CARE -> {
                listOf()
            }

            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> {
                error("Cannot create decision draft from placement of type '${placementPlan.type}'")
            }
        }

    tx.executeBatch(drafts) {
        sql(
            """
INSERT INTO decision (created_by, unit_id, application_id, type, start_date, end_date, planned)
VALUES (${bind(user.evakaUserId)}, ${bind { it.unitId }}, ${bind(application.id)}, ${bind { it.type }}, ${bind { it.startDate }}, ${bind { it.endDate }}, ${bind { it.planned }});
"""
        )
    }
}

fun updateDecisionDrafts(
    tx: Database.Transaction,
    applicationId: ApplicationId,
    updates: List<DecisionDraftUpdate>,
) {
    val successfulUpdates =
        tx.executeBatch(updates) {
                sql(
                    """
            UPDATE decision
            SET unit_id = ${bind { it.unitId }}, start_date = ${bind { it.startDate }}, end_date = ${bind { it.endDate }}, planned = ${bind { it.planned }}
            WHERE id = ${bind { it.id }} AND application_id = ${bind(applicationId)}
"""
                )
            }
            .sum()

    if (successfulUpdates < updates.size) {
        throw NotFound("Some decision draft was not found")
    }
}

data class DecisionDraftUpdate(
    val id: DecisionId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val planned: Boolean,
)

fun getDecisionUnits(tx: Database.Read): List<DecisionUnit> =
    tx.createQuery {
            sql(
                """
SELECT 
    id,
    name,
    decision_daycare_name AS daycareDecisionName,
    decision_preschool_name AS preschoolDecisionName,
    decision_handler, 
    decision_handler_address,
    unit_manager_name AS manager,
    street_address, 
    postal_code, 
    post_office,
    phone,
    provider_type
FROM daycare u
ORDER BY name
"""
            )
        }
        .toList()

fun getDecisionUnit(tx: Database.Read, unitId: DaycareId): DecisionUnit =
    tx.createQuery {
            sql(
                """
SELECT 
    id,
    name,
    decision_daycare_name AS daycareDecisionName,
    decision_preschool_name AS preschoolDecisionName,
    decision_handler, 
    decision_handler_address,
    unit_manager_name AS manager,
    street_address, 
    postal_code, 
    post_office,
    phone,
    provider_type
FROM daycare u
WHERE u.id = ${bind(unitId)}
"""
            )
        }
        .exactlyOne()

private fun planClubDecisionDrafts(plan: PlacementPlan): List<DecisionDraft> {
    return listOf(
        DecisionDraft(
            id = DecisionId(UUID.randomUUID()), // placeholder
            unitId = plan.unitId,
            type = DecisionType.CLUB,
            startDate = plan.period.start,
            endDate = plan.period.end,
            planned = true,
        )
    )
}

private fun planDaycareDecisionDrafts(plan: PlacementPlan): List<DecisionDraft> {
    val type =
        if (plan.type == PlacementType.DAYCARE_PART_TIME) {
            DecisionType.DAYCARE_PART_TIME
        } else {
            DecisionType.DAYCARE
        }

    return listOf(
        DecisionDraft(
            id = DecisionId(UUID.randomUUID()), // placeholder
            unitId = plan.unitId,
            type = type,
            startDate = plan.period.start,
            endDate = plan.period.end,
            planned = true,
        )
    )
}

private fun planPreschoolDecisionDrafts(
    plan: PlacementPlan,
    existingDecision: ExistingActiveDecision?,
): List<DecisionDraft> {
    val primaryType =
        if (plan.type in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE))
            DecisionType.PREPARATORY_EDUCATION
        else DecisionType.PRESCHOOL

    val primaryPlanned =
        if (existingDecision == null) {
            true
        } else {
            val existingPrimaryType =
                if (existingDecision.type == DecisionType.PREPARATORY_EDUCATION)
                    DecisionType.PREPARATORY_EDUCATION
                else DecisionType.PRESCHOOL
            existingDecision.unitId != plan.unitId || existingPrimaryType != primaryType
        }

    val primary =
        DecisionDraft(
            id = DecisionId(UUID.randomUUID()), // placeholder
            unitId = plan.unitId,
            type = primaryType,
            startDate = plan.period.start,
            endDate = plan.period.end,
            planned = primaryPlanned,
        )

    val connected =
        DecisionDraft(
            id = DecisionId(UUID.randomUUID()), // placeholder
            unitId = plan.unitId,
            type =
                if (plan.type == PlacementType.PRESCHOOL_CLUB) DecisionType.PRESCHOOL_CLUB
                else DecisionType.PRESCHOOL_DAYCARE,
            startDate = plan.preschoolDaycarePeriod?.start ?: plan.period.start,
            endDate = plan.preschoolDaycarePeriod?.end ?: plan.period.end,
            planned =
                plan.type in
                    listOf(
                        PlacementType.PRESCHOOL_DAYCARE,
                        PlacementType.PRESCHOOL_CLUB,
                        PlacementType.PREPARATORY_DAYCARE,
                    ),
        )

    return listOf(primary, connected)
}

fun Database.Transaction.clearDecisionDrafts(applicationIds: List<ApplicationId>) {
    execute {
        sql(
            "DELETE FROM decision WHERE application_id = ANY(${bind(applicationIds)}) AND sent_date IS NULL"
        )
    }
}
