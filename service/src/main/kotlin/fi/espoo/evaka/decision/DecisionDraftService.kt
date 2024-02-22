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

    // language=sql
    val sql =
        """
            INSERT INTO decision (created_by, unit_id, application_id, type, start_date, end_date, planned)
            VALUES (:createdBy, :unitId, :applicationId, :type, :startDate, :endDate, :planned);
            """
            .trimIndent()
    val batch = tx.prepareBatch(sql)
    drafts.forEach { draft ->
        batch
            .bind("createdBy", user.evakaUserId)
            .bind("unitId", draft.unitId)
            .bind("applicationId", application.id)
            .bind("type", draft.type)
            .bind("startDate", draft.startDate)
            .bind("endDate", draft.endDate)
            .bind("planned", draft.planned)
            .add()
    }
    batch.execute()
}

fun updateDecisionDrafts(
    tx: Database.Transaction,
    applicationId: ApplicationId,
    updates: List<DecisionDraftUpdate>
) {
    // language=sql
    val sql =
        """
            UPDATE decision
            SET unit_id = :unitId, start_date = :startDate, end_date = :endDate, planned = :planned
            WHERE id = :decisionId AND application_id = :applicationId
            """
            .trimIndent()

    val batch = tx.prepareBatch(sql)
    updates.forEach {
        batch
            .bind("applicationId", applicationId)
            .bind("decisionId", it.id)
            .bind("unitId", it.unitId)
            .bind("startDate", it.startDate)
            .bind("endDate", it.endDate)
            .bind("planned", it.planned)
            .add()
    }
    val successfulUpdates = batch.execute().sum()

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

fun getDecisionUnits(tx: Database.Read): List<DecisionUnit> {
    val sql =
        """
            $decisionUnitQuery
            ORDER BY name
            """
            .trimIndent()
    @Suppress("DEPRECATION") return tx.createQuery(sql).toList<DecisionUnit>()
}

fun getDecisionUnit(tx: Database.Read, unitId: DaycareId): DecisionUnit {
    // language=SQL
    val sql =
        """
             $decisionUnitQuery
             WHERE u.id = :id
            """
            .trimIndent()

    @Suppress("DEPRECATION")
    return tx.createQuery(sql).bind("id", unitId).exactlyOne<DecisionUnit>()
}

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
    // language=sql
    val sql =
        """DELETE FROM decision WHERE application_id = ANY(:applicationIds) AND sent_date IS NULL"""
            .trimIndent()

    @Suppress("DEPRECATION") createUpdate(sql).bind("applicationIds", applicationIds).execute()
}

private val decisionUnitQuery =
    """
SELECT 
    u.id,
    u.name,
    decision_daycare_name AS daycareDecisionName,
    decision_preschool_name AS preschoolDecisionName,
    decision_handler, 
    decision_handler_address,
    unit_manager_name AS manager,
    street_address, 
    postal_code, 
    post_office,
    u.phone,
    provider_type
FROM daycare u
    """
        .trimIndent()
