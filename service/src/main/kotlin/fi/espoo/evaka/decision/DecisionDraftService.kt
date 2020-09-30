// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.placement.PlacementPlan
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Service
class DecisionDraftService(
    private val placementPlanService: PlacementPlanService,
    private val jdbc: NamedParameterJdbcTemplate,
    private val dataSource: DataSource
) {
    fun getDecisionDrafts(applicationId: UUID): List<DecisionDraft> {
        return withSpringHandle(dataSource) { h -> fetchDecisionDrafts(h, applicationId) }
    }

    @Transactional
    fun clearDecisionDrafts(applicationId: UUID) {
        // language=sql
        val sql =
            """DELETE FROM decision WHERE application_id = :applicationId AND sent_date IS NULL""".trimIndent()

        jdbc.update(sql, mapOf("applicationId" to applicationId))
    }

    @Transactional
    fun createDecisionDrafts(user: AuthenticatedUser, application: ApplicationDetails) {
        val placementPlan = placementPlanService.getPlacementPlanByApplication((application.id))
            ?: throw NotFound("Application ${application.id} has no placement")

        val drafts: List<DecisionDraft> = when (placementPlan.type) {
            PlacementType.CLUB -> planClubDecisionDrafts(placementPlan)
            PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME -> planDaycareDecisionDrafts(placementPlan)
            PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE -> planPreschoolDecisionDrafts(placementPlan, application)
        }

        // language=sql
        val sql =
            """
            INSERT INTO decision (created_by, unit_id, application_id, type, start_date, end_date, planned)
            VALUES (:createdBy, :unitId, :applicationId, :type::decision_type, :startDate, :endDate, :planned);
            """.trimIndent()
        drafts.forEach { draft ->
            jdbc.update(
                sql,
                mapOf(
                    "createdBy" to user.id,
                    "unitId" to draft.unitId,
                    "applicationId" to application.id,
                    "type" to draft.type.toString(),
                    "startDate" to draft.startDate,
                    "endDate" to draft.endDate,
                    "planned" to draft.planned
                )
            )
        }
    }

    @Transactional
    fun updateDecisionDrafts(applicationId: UUID, updates: List<DecisionDraftUpdate>) {
        // language=sql
        val sql =
            """
            UPDATE decision
            SET unit_id = :unitId, start_date = :startDate, end_date = :endDate, planned = :planned
            WHERE id = :decisionId AND application_id = :applicationId
            """.trimIndent()

        val successfulUpdates = jdbc.batchUpdate(
            sql,
            updates.map {
                mutableMapOf(
                    "applicationId" to applicationId,
                    "decisionId" to it.id,
                    "unitId" to it.unitId,
                    "startDate" to it.startDate,
                    "endDate" to it.endDate,
                    "planned" to it.planned
                )
            }.toTypedArray()
        ).sum()

        if (successfulUpdates < updates.size) {
            throw NotFound("Some decision draft was not found")
        }
    }

    data class DecisionDraftUpdate(
        val id: UUID,
        val unitId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val planned: Boolean
    )

    fun getDecisionUnits(): List<DecisionUnit> {
        val sql =
            """
            $decisionUnitQuery
            ORDER BY name
            """.trimIndent()
        return jdbc.query(sql, mapOf<String, String>()) { rs, _ -> toDecisionUnit(rs) }
    }

    fun getDecisionUnit(unitId: UUID): DecisionUnit {
        // language=SQL
        val sql =
            """
             $decisionUnitQuery
             WHERE u.id = :id
            """.trimIndent()

        val params = mapOf("id" to unitId)

        return jdbc.query(sql, params) { rs, _ -> toDecisionUnit(rs) }.first()
    }

    private fun planClubDecisionDrafts(plan: PlacementPlan): List<DecisionDraft> {
        return listOf(
            DecisionDraft(
                id = UUID.randomUUID(), // placeholder
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
            if (plan.type == PlacementType.DAYCARE_PART_TIME) DecisionType.DAYCARE_PART_TIME
            else DecisionType.DAYCARE

        return listOf(
            DecisionDraft(
                id = UUID.randomUUID(), // placeholder
                unitId = plan.unitId,
                type = type,
                startDate = plan.period.start,
                endDate = plan.period.end,
                planned = true
            )
        )
    }

    private fun planPreschoolDecisionDrafts(plan: PlacementPlan, application: ApplicationDetails): List<DecisionDraft> {
        val primaryType = if (plan.type in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE)) DecisionType.PREPARATORY_EDUCATION else DecisionType.PRESCHOOL

        val primary = DecisionDraft(
            id = UUID.randomUUID(), // placeholder
            unitId = plan.unitId,
            type = primaryType,
            startDate = plan.period.start,
            endDate = plan.period.end,
            planned = !application.additionalDaycareApplication
        )

        val connected = DecisionDraft(
            id = UUID.randomUUID(), // placeholder
            unitId = plan.unitId,
            type = DecisionType.PRESCHOOL_DAYCARE,
            startDate = plan.preschoolDaycarePeriod?.start ?: plan.period.start,
            endDate = plan.preschoolDaycarePeriod?.end ?: plan.period.end,
            planned = plan.type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)
        )

        return listOf(primary, connected)
    }
}

private val decisionUnitQuery =
    """
SELECT 
    u.id,
    u.name,
    decision_daycare_name, 
    decision_preschool_name, 
    decision_handler, 
    decision_handler_address,
    m.name AS manager,
    street_address, 
    postal_code, 
    post_office,
    provider_type
FROM daycare u
LEFT JOIN unit_manager m ON u.unit_manager_id = m.id
    """.trimIndent()

private val toDecisionUnit = { rs: ResultSet ->
    DecisionUnit(
        id = UUID.fromString(rs.getString("id")),
        name = rs.getString("name"),
        manager = rs.getString("manager"),
        streetAddress = rs.getString("street_address"),
        postalCode = rs.getString("postal_code"),
        postOffice = rs.getString("post_office"),
        daycareDecisionName = rs.getString("decision_daycare_name"),
        preschoolDecisionName = rs.getString("decision_preschool_name"),
        decisionHandler = rs.getString("decision_handler"),
        decisionHandlerAddress = rs.getString("decision_handler_address"),
        providerType = rs.getEnum("provider_type")
    )
}
