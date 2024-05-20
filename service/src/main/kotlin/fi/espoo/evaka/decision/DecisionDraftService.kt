// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.placement.PlacementPlan
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID

fun createDecisionDrafts(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    application: ApplicationDetails
) {
    val placementPlan =
        tx.getPlacementPlan(application.id)
            ?: throw NotFound("Application ${application.id} has no placement")

    val drafts: List<DecisionDraft> =
        when (placementPlan.type) {
            PlacementType.CLUB -> planClubDecisionDrafts(placementPlan)
            PlacementType.DAYCARE,
            PlacementType.DAYCARE_PART_TIME,
            PlacementType.PRESCHOOL_DAYCARE_ONLY,
            PlacementType.PREPARATORY_DAYCARE_ONLY -> planDaycareDecisionDrafts(placementPlan)
            PlacementType.PRESCHOOL,
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PRESCHOOL_CLUB,
            PlacementType.PREPARATORY,
            PlacementType.PREPARATORY_DAYCARE ->
                planPreschoolDecisionDrafts(placementPlan, application)
            PlacementType.SCHOOL_SHIFT_CARE -> listOf()
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
                error("Cannot create decision draft from placement of type '${placementPlan.type}'")
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
    updates: List<DecisionDraftUpdate>
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
    val planned: Boolean
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
            planned = true
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
            planned = true
        )
    )
}

private fun planPreschoolDecisionDrafts(
    plan: PlacementPlan,
    application: ApplicationDetails
): List<DecisionDraft> {
    val primaryType =
        if (plan.type in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE))
            DecisionType.PREPARATORY_EDUCATION
        else DecisionType.PRESCHOOL

    val primary =
        DecisionDraft(
            id = DecisionId(UUID.randomUUID()), // placeholder
            unitId = plan.unitId,
            type = primaryType,
            startDate = plan.period.start,
            endDate = plan.period.end,
            planned = !application.additionalDaycareApplication
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
                        PlacementType.PREPARATORY_DAYCARE
                    )
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
