// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.absence.AbsenceType
import evaka.core.daycare.getDaycaresForArea
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.GroupId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildAbsenceReport(private val accessControl: AccessControl) {

    data class ChildAbsenceReportBody(
        val range: FiniteDateRange,
        val areaId: AreaId?,
        val unitId: DaycareId?,
        val groupId: GroupId?,
    )

    @PostMapping("/employee/reports/child-absence")
    fun getChildAbsenceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestBody body: ChildAbsenceReportBody,
    ): List<ChildAbsenceReportRow> {
        if (body.areaId == null && body.unitId == null)
            throw BadRequest("Must give either area ID or unit ID")
        if (body.areaId != null && body.unitId != null)
            throw BadRequest("Must give only one of area ID or unit ID")
        if (body.range.end > body.range.start.plusYears(2))
            throw BadRequest("Date range must not exceed two years")
        return db.connect { dbc ->
                dbc.read { tx ->
                    if (body.areaId != null)
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Global.READ_CHILD_ABSENCE_REPORT_FOR_AREA,
                        )
                    else if (body.unitId != null)
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Unit.READ_CHILD_ABSENCE_REPORT_FOR_UNIT,
                            body.unitId,
                        )

                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val today = clock.today()
                    if (body.range.start > today) return@read emptyList()
                    val range = FiniteDateRange(body.range.start, minOf(body.range.end, today))

                    val unitIds =
                        if (body.unitId != null) listOf(body.unitId)
                        else tx.getDaycaresForArea(body.areaId!!).map { it.id }
                    val groupId = if (body.unitId != null) body.groupId else null

                    val rows = getChildAbsenceReportRows(tx, unitIds, groupId, range)
                    val placementInfo =
                        getChildPlacementInfo(tx, rows.map { it.childId }.distinct(), range)

                    rows
                        .groupBy { it.childId }
                        .map { (childId, childRows) ->
                            val info = placementInfo.getValue(childId)
                            ChildAbsenceReportRow(
                                childId = childId,
                                firstName = childRows[0].firstName,
                                lastName = childRows[0].lastName,
                                placementType = info.placementType,
                                daycareName = info.daycareName,
                                groupName = info.groupName,
                                absenceCountsByType =
                                    childRows.associate { it.absenceType to it.absenceCount },
                            )
                        }
                }
            }
            .also {
                Audit.ChildAbsenceReport.log(
                    meta =
                        mapOf(
                            "unitId" to body.unitId,
                            "areaId" to body.areaId,
                            "groupId" to body.groupId,
                            "rangeStart" to body.range.start,
                            "rangeEnd" to body.range.end,
                        )
                )
            }
    }
}

data class ChildAbsenceReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val placementType: PlacementType,
    val daycareName: String,
    val groupName: String,
    val absenceCountsByType: Map<AbsenceType, Int>,
)

data class ChildAbsenceRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val absenceType: AbsenceType,
    val absenceCount: Int,
)

data class ChildPlacementInfo(
    val placementType: PlacementType,
    val daycareName: String,
    val groupName: String,
)

fun getChildAbsenceReportRows(
    tx: Database.Read,
    unitIds: List<DaycareId>,
    groupId: GroupId?,
    range: FiniteDateRange,
): List<ChildAbsenceRow> {
    val placementsQuery =
        if (groupId != null) {
            QuerySql {
                sql(
                    """
SELECT
    p.child_id,
    p.type AS placement_type,
    daterange(dgp.start_date, dgp.end_date, '[]') * ${bind(range)} AS examination_range
FROM daycare_group_placement dgp
JOIN placement p ON p.id = dgp.daycare_placement_id
WHERE
    p.unit_id = ANY (${bind(unitIds)}) AND
    dgp.daycare_group_id = ${bind(groupId)} AND
    daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(range)}
"""
                )
            }
        } else {
            QuerySql {
                sql(
                    """
SELECT
    p.child_id,
    p.type AS placement_type,
    daterange(p.start_date, p.end_date, '[]') * ${bind(range)} AS examination_range
FROM placement p
WHERE
    p.unit_id = ANY (${bind(unitIds)}) AND
    daterange(p.start_date, p.end_date, '[]') && ${bind(range)}
"""
                )
            }
        }

    return tx.createQuery {
            sql(
                """
WITH placements_in_scope AS (${subquery(placementsQuery)}),
full_day_absences AS (
    SELECT
        pis.child_id,
        ab.date,
        cardinality(absence_categories(pis.placement_type)) AS required_categories,
        count(DISTINCT ab.category) AS present_categories,
        coalesce(
            max(ab.absence_type) FILTER (WHERE ab.category = 'NONBILLABLE'),
            max(ab.absence_type) FILTER (WHERE ab.category = 'BILLABLE')
        ) AS day_type
    FROM placements_in_scope pis
    JOIN absence ab ON ab.child_id = pis.child_id
        AND ${bind(range)} @> ab.date
        AND pis.examination_range @> ab.date
        AND ab.category = ANY (absence_categories(pis.placement_type))
    GROUP BY pis.child_id, ab.date, pis.placement_type
)
SELECT
    child.id AS child_id,
    child.first_name,
    child.last_name,
    fda.day_type AS absence_type,
    count(*) AS absence_count
FROM full_day_absences fda
JOIN person child ON child.id = fda.child_id
WHERE fda.required_categories > 0
    AND fda.present_categories = fda.required_categories
    AND fda.day_type = ANY (${bind(listOf(
        AbsenceType.OTHER_ABSENCE,
        AbsenceType.SICKLEAVE,
        AbsenceType.PLANNED_ABSENCE,
        AbsenceType.UNKNOWN_ABSENCE,
    ))})
GROUP BY child.id, child.first_name, child.last_name, absence_type
                """
            )
        }
        .toList<ChildAbsenceRow>()
}

fun getChildPlacementInfo(
    tx: Database.Read,
    children: List<ChildId>,
    range: FiniteDateRange,
): Map<ChildId, ChildPlacementInfo> =
    tx.createQuery {
            sql(
                """
SELECT pi.child_id, pi.placement_type, d.name AS daycare_name, coalesce(dg.name, '') AS group_name
FROM (
    SELECT DISTINCT ON (p.child_id)
        p.child_id,
        p.type AS placement_type,
        p.unit_id,
        dgp.daycare_group_id
    FROM placement p
    LEFT JOIN daycare_group_placement dgp ON
        p.id = dgp.daycare_placement_id AND
        daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(range)}
    WHERE
        p.child_id = ANY (${bind(children)}) AND
        daterange(p.start_date, p.end_date, '[]') && ${bind(range)}
    ORDER BY p.child_id, p.start_date DESC, dgp.start_date DESC
) pi
JOIN daycare d ON pi.unit_id = d.id
LEFT JOIN daycare_group dg ON pi.daycare_group_id = dg.id
                """
            )
        }
        .toMap {
            column<ChildId>("child_id") to
                ChildPlacementInfo(
                    column<PlacementType>("placement_type"),
                    column<String>("daycare_name"),
                    column<String>("group_name"),
                )
        }
