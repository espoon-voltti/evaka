// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class DecisionsReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/decisions")
    fun getDecisionsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam columns: Set<DecisionReportColumnType>?,
    ): List<DecisionsReportRow> {
        if (to.isBefore(from)) throw BadRequest("Inverted time range")

        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_DECISIONS_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getDecisionsRows(FiniteDateRange(from, to), columns)
                }
            }
            .also {
                Audit.DecisionsReportRead.log(
                    meta = mapOf("from" to from, "to" to to, "count" to it.size)
                )
            }
    }
}

private fun Database.Read.getDecisionsRows(
    range: FiniteDateRange,
    columns: Set<DecisionReportColumnType>?,
): List<DecisionsReportRow> {
    val decisionTypes = columns?.flatMap { it.decisionTypes }?.toSet()
    val decisionPredicates =
        Predicate.allNotNull(
            Predicate { where("$it.sent_date IS NOT NULL") },
            Predicate {
                where("$it.sent_date BETWEEN ${bind(range.start)} AND ${bind(range.end)}")
            },
            decisionTypes?.let { Predicate { table -> where("$table.type = ANY (${bind(it)})") } },
        )
    val queryResult =
        createQuery {
                sql(
                    """
SELECT 
    ca.name AS care_area_name,
    u.id AS unit_id,
    u.name AS unit_name,
    u.provider_type,
    de.id AS decision_id,
    de.type AS decision_type,
    a.id AS application_id,
    (
        SELECT array_agg(e::UUID)
        FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') e
    ) AS preferred_units,
    date_part('year', age(de.sent_date, ch.date_of_birth)) AS age
FROM application a
JOIN decision de ON de.application_id = a.id
JOIN daycare u ON u.id = de.unit_id
JOIN care_area ca ON ca.id = u.care_area_id
JOIN person ch ON ch.id = a.child_id
WHERE ${predicate(decisionPredicates.forTable("de"))}
"""
                )
            }
            .toList<DecisionsReportQueryRow>()

    return queryResult
        .groupBy { it.unitId }
        .map { (unitId, rows) ->
            val applicationDecisionCount =
                rows.groupBy { it.applicationId }.mapValues { it.value.size }

            val daycareUnder3 =
                rows.filter {
                    it.age < 3 &&
                        it.decisionType in DecisionReportColumnType.daycareUnder3.decisionTypes
                }
            val daycareOver3 =
                rows.filter {
                    it.age >= 3 &&
                        it.decisionType in DecisionReportColumnType.daycareOver3.decisionTypes
                }
            val preschool =
                rows.filter {
                    it.decisionType in DecisionReportColumnType.preschool.decisionTypes &&
                        applicationDecisionCount.getValue(it.applicationId) == 1
                }
            val preschoolDaycare =
                rows.filter {
                    it.decisionType in DecisionReportColumnType.preschoolDaycare.decisionTypes &&
                        applicationDecisionCount.getValue(it.applicationId) == 2
                }
            val preparatory =
                rows.filter {
                    it.decisionType in DecisionReportColumnType.preparatory.decisionTypes &&
                        applicationDecisionCount.getValue(it.applicationId) == 1
                }
            val preparatoryDaycare =
                rows.filter {
                    it.decisionType in DecisionReportColumnType.preparatoryDaycare.decisionTypes &&
                        applicationDecisionCount.getValue(it.applicationId) == 2
                }
            val connectedDaycareOnly =
                rows.filter {
                    it.decisionType in
                        DecisionReportColumnType.connectedDaycareOnly.decisionTypes &&
                        applicationDecisionCount.getValue(it.applicationId) == 1
                }
            val club =
                rows.filter { it.decisionType in DecisionReportColumnType.club.decisionTypes }

            // Number of applications (not decisions) that have Nth preference for this unit
            val preference1 =
                rows
                    .filter { it.unitId == it.preferredUnits.getOrNull(0) }
                    .distinctBy { it.applicationId }
            val preference2 =
                rows
                    .filter { it.unitId == it.preferredUnits.getOrNull(1) }
                    .distinctBy { it.applicationId }
            val preference3 =
                rows
                    .filter { it.unitId == it.preferredUnits.getOrNull(2) }
                    .distinctBy { it.applicationId }
            val preferenceNone = rows.filter { !it.preferredUnits.contains(it.unitId) }

            DecisionsReportRow(
                careAreaName = rows.first().careAreaName,
                unitId = unitId,
                unitName = rows.first().unitName,
                providerType = rows.first().providerType,
                daycareUnder3 = daycareUnder3.count(),
                daycareOver3 = daycareOver3.count(),
                preschool = preschool.count(),
                preschoolDaycare = preschoolDaycare.count(),
                preparatory = preparatory.count(),
                preparatoryDaycare = preparatoryDaycare.count(),
                connectedDaycareOnly = connectedDaycareOnly.count(),
                club = club.count(),
                preference1 = preference1.count(),
                preference2 = preference2.count(),
                preference3 = preference3.count(),
                preferenceNone = preferenceNone.count(),
                total = applicationDecisionCount.size, // Number of applications
            )
        }
        .sortedWith(compareBy({ it.careAreaName }, { it.unitName }))
}

private data class DecisionsReportQueryRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val providerType: ProviderType,
    val decisionId: DecisionId,
    val decisionType: DecisionType,
    val applicationId: ApplicationId,
    val preferredUnits: List<DaycareId>,
    val age: Int,
)

data class DecisionsReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val providerType: ProviderType,
    val daycareUnder3: Int,
    val daycareOver3: Int,
    val preschool: Int,
    val preschoolDaycare: Int,
    val preparatory: Int,
    val preparatoryDaycare: Int,
    val connectedDaycareOnly: Int,
    val club: Int,
    val preference1: Int,
    val preference2: Int,
    val preference3: Int,
    val preferenceNone: Int,
    val total: Int,
)

@ConstList("decisionReportColumnTypes")
enum class DecisionReportColumnType(val decisionTypes: Set<DecisionType>) {
    daycareUnder3(setOf(DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME)),
    daycareOver3(setOf(DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME)),
    preschool(setOf(DecisionType.PRESCHOOL)),
    preschoolDaycare(setOf(DecisionType.PRESCHOOL)),
    preparatory(setOf(DecisionType.PREPARATORY_EDUCATION)),
    preparatoryDaycare(setOf(DecisionType.PREPARATORY_EDUCATION)),
    // PRESCHOOL_DAYCARE is used for connected daycare for both preschool and preparatory
    connectedDaycareOnly(setOf(DecisionType.PRESCHOOL_DAYCARE)),
    club(setOf(DecisionType.CLUB)),
}
