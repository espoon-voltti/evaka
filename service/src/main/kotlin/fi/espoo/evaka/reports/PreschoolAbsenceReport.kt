// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PreschoolAbsenceReport(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/preschool-absence")
    fun getPreschoolAbsenceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam groupId: GroupId?,
        @RequestParam termStart: LocalDate,
        @RequestParam termEnd: LocalDate,
    ): List<ChildPreschoolAbsenceRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_PRESCHOOL_ABSENCE_REPORT,
                        unitId,
                    )
                    val daycare = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")
                    val termRange = FiniteDateRange(termStart, termEnd)
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val preschoolAbsenceTypes =
                        AbsenceType.entries.filter {
                            when (it) {
                                AbsenceType.OTHER_ABSENCE,
                                AbsenceType.SICKLEAVE,
                                AbsenceType.PLANNED_ABSENCE,
                                AbsenceType.UNKNOWN_ABSENCE -> true
                                else -> false
                            }
                        }
                    val dailyPreschoolTime =
                        daycare.dailyPreschoolTime
                            ?: TimeRange(LocalTime.of(9, 0, 0, 0), LocalTime.of(13, 0, 0, 0))

                    val dailyPreparatoryTime =
                        daycare.dailyPreparatoryTime
                            ?: TimeRange(LocalTime.of(9, 0, 0, 0), LocalTime.of(14, 0, 0, 0))

                    if (groupId == null)
                        calculateHourlyResults(
                            getPreschoolAbsenceReportRowsForUnit(
                                tx,
                                daycare.id,
                                termRange,
                                preschoolAbsenceTypes,
                            ),
                            calculateDailyPreschoolAttendanceDeviationForUnit(
                                tx,
                                unitId,
                                termRange,
                                dailyPreschoolTime,
                                dailyPreparatoryTime,
                            ),
                            dailyPreschoolTime,
                            dailyPreparatoryTime,
                        )
                    else
                        calculateHourlyResults(
                            getPreschoolAbsenceReportRowsForGroup(
                                tx,
                                daycare.id,
                                groupId,
                                termRange,
                                preschoolAbsenceTypes,
                            ),
                            calculateDailyPreschoolAttendanceDeviationForGroup(
                                tx,
                                unitId,
                                groupId,
                                termRange,
                                dailyPreschoolTime,
                                dailyPreparatoryTime,
                            ),
                            dailyPreschoolTime,
                            dailyPreparatoryTime,
                        )
                }
                .also {
                    Audit.PreschoolAbsenceReport.log(
                        meta =
                            mapOf(
                                "unitId" to unitId,
                                "termStart" to termStart,
                                "termEnd" to termEnd,
                                "groupId" to groupId,
                            )
                    )
                }
        }
    }

    private fun calculateHourlyResults(
        absenceRows: List<PreschoolAbsenceRow>,
        deviationsByChild: Map<ChildId, List<ChildDailyAttendanceDeviation>>,
        dailyPreschoolTime: TimeRange,
        dailyPreparatoryTime: TimeRange,
    ): List<ChildPreschoolAbsenceRow> {
        val hourlyRows =
            absenceRows.map { r ->
                val dailyTimeInMinutes =
                    when (r.placementType) {
                        PlacementType.PREPARATORY,
                        PlacementType.PREPARATORY_DAYCARE ->
                            dailyPreparatoryTime.duration.toMinutes()
                        else -> dailyPreschoolTime.duration.toMinutes()
                    }
                val childAttendanceDeviationMinutes =
                    if (r.absenceType == AbsenceType.OTHER_ABSENCE) {
                        val childDeviations = deviationsByChild[r.childId] ?: emptyList()
                        childDeviations.sumOf { it.missingMinutes }
                    } else 0
                PreschoolAbsenceReportRow(
                    r.childId,
                    r.firstName,
                    r.lastName,
                    r.absenceType,
                    (r.absenceCount * dailyTimeInMinutes + childAttendanceDeviationMinutes)
                        .floorDiv(60)
                        .toInt(),
                )
            }

        return hourlyRows
            .groupBy { row -> row.childId }
            .entries
            .map { k ->
                ChildPreschoolAbsenceRow(
                    childId = k.key,
                    firstName = k.value[0].firstName,
                    lastName = k.value[0].lastName,
                    hourlyTypeResults = k.value.associateBy({ it.absenceType }, { it.absenceHours }),
                )
            }
    }
}

data class PreschoolAbsenceRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val absenceType: AbsenceType,
    val absenceCount: Int,
    val placementType: PlacementType,
)

data class PreschoolAbsenceReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val absenceType: AbsenceType,
    val absenceHours: Int,
)

data class ChildPreschoolAbsenceRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val hourlyTypeResults: Map<AbsenceType, Int>,
)

data class ChildDailyAttendanceDeviation(
    val missingMinutes: Int,
    val childId: ChildId,
    val date: LocalDate,
)

fun getPreschoolAbsenceReportRowsForUnit(
    tx: Database.Read,
    daycareId: DaycareId,
    preschoolTerm: FiniteDateRange,
    absenceTypes: List<AbsenceType>,
): List<PreschoolAbsenceRow> {
    val holidays = getHolidays(preschoolTerm)
    return tx.createQuery {
            sql(
                """
WITH preschool_placement_children AS (
    SELECT child.id,
        child.first_name,
        child.last_name,
        daterange(p.start_date, p.end_date, '[]') * ${bind(preschoolTerm)} AS examination_range,
        p.type AS placement_type
    FROM placement p
    JOIN person child ON p.child_id = child.id
    JOIN daycare d ON p.unit_id = d.id
        AND 'PRESCHOOL'::care_types = ANY (d.type)
    WHERE daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        AND p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
        AND p.unit_id = ${bind(daycareId)}
)
SELECT ppc.id AS child_id,
    ppc.first_name,
    ppc.last_name,
    CASE types.absence_type
        WHEN 'PLANNED_ABSENCE' THEN 'OTHER_ABSENCE'
        ELSE types.absence_type
    END AS absence_type,
    count(ab.id) AS absence_count,
    ppc.placement_type
FROM preschool_placement_children ppc
    LEFT JOIN unnest(${bind(absenceTypes)}::absence_type[]) AS types (absence_type) ON TRUE
    LEFT JOIN absence ab ON ppc.examination_range @> ab.date
        AND ppc.id = ab.child_id
        AND ab.absence_type = types.absence_type
        AND ab.category = 'NONBILLABLE'
        AND extract(ISODOW FROM ab.date) BETWEEN 1 AND 5
        AND ab.date != ALL (${bind(holidays)})
        AND NOT exists (SELECT FROM preschool_term pt WHERE pt.term_breaks @> ab.date)
GROUP BY 1, 2, 3, 4, 6;
        """
                    .trimIndent()
            )
        }
        .toList<PreschoolAbsenceRow>()
}

fun getPreschoolAbsenceReportRowsForGroup(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId,
    preschoolTerm: FiniteDateRange,
    absenceTypes: List<AbsenceType>,
): List<PreschoolAbsenceRow> {
    val holidays = getHolidays(preschoolTerm)
    return tx.createQuery {
            sql(
                """
WITH preschool_group_placement_children AS (
    SELECT child.id,
        child.first_name,
        child.last_name,
        daterange(dgp.start_date, dgp.end_date, '[]') * ${bind(preschoolTerm)} AS examination_range,
        p.type AS placement_type
    FROM placement p
        JOIN person child ON p.child_id = child.id
        JOIN daycare d ON p.unit_id = d.id
            AND 'PRESCHOOL'::care_types = ANY (d.type)
        JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
            AND dgp.daycare_group_id = ${bind(groupId)}
            AND daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(preschoolTerm)}
    WHERE daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        AND p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
        AND p.unit_id = ${bind(daycareId)}
)
SELECT pgpc.id AS child_id,
    pgpc.first_name,
    pgpc.last_name,
    CASE types.absence_type WHEN 'PLANNED_ABSENCE' THEN 'OTHER_ABSENCE' ELSE types.absence_type END AS absence_type,
    count(ab.id) AS absence_count,
    pgpc.placement_type
FROM preschool_group_placement_children pgpc
    LEFT JOIN unnest(${bind(absenceTypes)}::absence_type[]) AS types (absence_type) ON TRUE
    LEFT JOIN absence ab ON pgpc.examination_range @> ab.date
        AND pgpc.id = ab.child_id
        AND ab.absence_type = types.absence_type
        AND ab.category = 'NONBILLABLE'
        AND extract(ISODOW FROM ab.date) BETWEEN 1 AND 5
        AND ab.date != ALL (${bind(holidays)})
        AND NOT exists (SELECT FROM preschool_term pt WHERE pt.term_breaks @> ab.date)
GROUP BY 1, 2, 3, 4, 6;
        """
                    .trimIndent()
            )
        }
        .toList<PreschoolAbsenceRow>()
}

fun calculateDailyPreschoolAttendanceDeviationForUnit(
    tx: Database.Read,
    daycareId: DaycareId,
    preschoolTerm: FiniteDateRange,
    preschoolDailyServiceTime: TimeRange,
    preparatoryDailyServiceTime: TimeRange,
): Map<ChildId, List<ChildDailyAttendanceDeviation>> {
    val preschoolEndTime = preschoolDailyServiceTime.end.inner
    val preschoolStartTime = preschoolDailyServiceTime.start.inner
    val preparatoryEndTime = preparatoryDailyServiceTime.end.inner
    val preparatoryStartTime = preparatoryDailyServiceTime.start.inner
    return tx.createQuery {
            sql(
                """
WITH intersection AS(
    SELECT
        (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
            tsrange(ca.date + ${bind(preschoolStartTime)}, ca.date + ${bind(preschoolEndTime)})
        ) AS preschool_range,
        (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
            tsrange(ca.date + ${bind(preparatoryStartTime)}, ca.date + ${bind(preparatoryEndTime)})
        ) AS preparatory_range,
        ca.date,
        ca.child_id,
        p.type
    FROM placement p
    JOIN daycare d ON p.unit_id = d.id
        AND 'PRESCHOOL'::care_types = ANY (d.type)
    JOIN child_attendance ca ON ca.child_id = p.child_id
        AND daterange(p.start_date, p.end_date, '[]') * ${bind(preschoolTerm)} @> ca.date
        AND ca.end_time IS NOT NULL
        AND ca.unit_id = d.id
    -- pick out attendance intersecting preschool service time
    WHERE (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(preschoolStartTime)}, ca.date + ${bind(preschoolEndTime)})
        OR tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(preparatoryStartTime)}, ca.date + ${bind(preparatoryEndTime)}))
        AND daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        AND p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
        AND p.unit_id = ${bind(daycareId)}
)
-- calculate child attendance deviation from standard daily preschool service time in minutes
SELECT floor(
    extract(
        EPOCH FROM (${bind(preschoolEndTime)} - ${bind(preschoolStartTime)}) 
        - sum((upper(preschool_range) - lower(preschool_range)))
    ) / 60) AS missing_minutes,
    date,
    child_id
FROM  intersection
WHERE type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
GROUP BY date, child_id
-- filter out daily deviations below 20 minutes
HAVING floor(
    extract(
        EPOCH FROM (${bind(preschoolEndTime)} - ${bind(preschoolStartTime)})
        - sum((upper(preschool_range) - lower(preschool_range)))
    ) / 60
) >= 20

UNION

-- calculate child attendance deviation from standard daily preparatory service time in minutes
SELECT floor(
    extract(
        EPOCH FROM (${bind(preparatoryEndTime)} - ${bind(preparatoryStartTime)}) 
        - sum((upper(preparatory_range) - lower(preparatory_range)))
    ) / 60) AS missing_minutes,
    date,
    child_id
FROM intersection
WHERE type = ANY ('{PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
GROUP BY date, child_id
-- filter out daily deviations below 20 minutes
HAVING floor(
    extract(
        EPOCH FROM (${bind(preparatoryEndTime)} - ${bind(preparatoryStartTime)})
        - sum((upper(preparatory_range) - lower(preparatory_range)))
    ) / 60
) >= 20;
    """
                    .trimIndent()
            )
        }
        .toList<ChildDailyAttendanceDeviation>()
        .groupBy { it.childId }
}

fun calculateDailyPreschoolAttendanceDeviationForGroup(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId,
    preschoolTerm: FiniteDateRange,
    preschoolDailyServiceTime: TimeRange,
    preparatoryDailyServiceTime: TimeRange,
): Map<ChildId, List<ChildDailyAttendanceDeviation>> {
    val preschoolEndTime = preschoolDailyServiceTime.end.inner
    val preschoolStartTime = preschoolDailyServiceTime.start.inner
    val preparatoryEndTime = preparatoryDailyServiceTime.end.inner
    val preparatoryStartTime = preparatoryDailyServiceTime.start.inner
    return tx.createQuery {
            sql(
                """
WITH intersection AS (
    SELECT
        (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
            tsrange(ca.date + ${bind(preschoolStartTime)}, ca.date + ${bind(preschoolEndTime)})
        ) AS preschool_range,
        (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
            tsrange(ca.date + ${bind(preparatoryStartTime)}, ca.date + ${bind(preparatoryEndTime)})
        ) AS preparatory_range,
        ca.date,
        ca.child_id,
        p.type
    FROM placement p
    JOIN daycare d
        ON p.unit_id = d.id
        AND 'PRESCHOOL'::care_types = ANY (d.type)
    JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
        AND daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(preschoolTerm)}
        AND dgp.daycare_group_id = ${bind(groupId)}
    JOIN child_attendance ca ON ca.child_id = p.child_id
        AND daterange(dgp.start_date, dgp.end_date, '[]') * ${bind(preschoolTerm)} @> ca.date
        AND ca.end_time IS NOT NULL
        AND ca.unit_id = d.id
    -- pick out attendance intersecting preschool service time
    WHERE tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(preschoolStartTime)}, ca.date + ${bind(preschoolEndTime)})
        OR tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(preparatoryStartTime)}, ca.date + ${bind(preparatoryEndTime)})
        AND daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        AND p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE, PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
        AND p.unit_id = ${bind(daycareId)}
)
-- calculate child attendance deviation from standard daily preschool service time in minutes
SELECT floor(
    extract(
        EPOCH FROM (${bind(preschoolEndTime)} - ${bind(preschoolStartTime)})
        - sum((upper(preschool_range) - lower(preschool_range)))
    ) / 60) AS missing_minutes,
    date,
    child_id
FROM intersection
WHERE type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
GROUP BY date, child_id
-- filter out daily deviations below 20 minutes
HAVING floor(
    extract(
        EPOCH FROM (${bind(preschoolEndTime)} - ${bind(preschoolStartTime)})
        - sum((upper(preschool_range) - lower(preschool_range)))
    ) / 60
) >= 20

UNION

-- calculate child attendance deviation from standard daily preparatory service time in minutes
SELECT floor(
    extract(
        EPOCH FROM (${bind(preparatoryEndTime)} - ${bind(preparatoryStartTime)})
        - sum((upper(preparatory_range) - lower(preparatory_range)))
    ) / 60) AS missing_minutes,
    date,
    child_id
FROM intersection
WHERE type = ANY ('{ PREPARATORY, PREPARATORY_DAYCARE}'::placement_type[])
GROUP BY date, child_id
-- filter out daily deviations below 20 minutes
HAVING floor(
    extract(
        EPOCH FROM (${bind(preparatoryEndTime)} - ${bind(preparatoryStartTime)})
        - sum((upper(preparatory_range) - lower(preparatory_range)))
    ) / 60
) >= 20;
    """
                    .trimIndent()
            )
        }
        .toList<ChildDailyAttendanceDeviation>()
        .groupBy { it.childId }
}
