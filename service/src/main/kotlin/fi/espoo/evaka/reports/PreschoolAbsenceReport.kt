// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PreschoolAbsenceReport(
    private val accessControl: AccessControl
) {
    @GetMapping("/employee/reports/preschool-absence")
    fun getPreschoolAbsenceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam groupId: GroupId?,
        @RequestParam termStart: LocalDate,
        @RequestParam termEnd: LocalDate
    ): List<ChildPreschoolAbsenceRow> =
        db.connect { dbc ->
            dbc
                .read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_PRESCHOOL_ABSENCE_REPORT,
                        unitId
                    )
                    val daycare = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")
                    val termRange = FiniteDateRange(termStart, termEnd)
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val preschoolAbsenceTypes =
                        AbsenceType.entries.filter {
                            when (it) {
                                AbsenceType.OTHER_ABSENCE,
                                AbsenceType.SICKLEAVE,
                                AbsenceType.UNKNOWN_ABSENCE -> true
                                else -> false
                            }
                        }
                    val dailyPreschoolTime =
                        daycare.dailyPreschoolTime
                            ?: TimeRange(LocalTime.of(9, 0, 0, 0), LocalTime.of(13, 0, 0, 0))

                    if (groupId == null) {
                        calculateHourlyResults(
                            getPreschoolAbsenceReportRowsForUnit(
                                tx,
                                daycare.id,
                                termRange,
                                preschoolAbsenceTypes
                            ),
                            calculateDailyPreschoolAttendanceDeviationForUnit(
                                tx,
                                unitId,
                                termRange,
                                dailyPreschoolTime
                            ),
                            dailyPreschoolTime
                        )
                    } else {
                        calculateHourlyResults(
                            getPreschoolAbsenceReportRowsForGroup(
                                tx,
                                daycare.id,
                                groupId,
                                termRange,
                                preschoolAbsenceTypes
                            ),
                            calculateDailyPreschoolAttendanceDeviationForGroup(
                                tx,
                                unitId,
                                groupId,
                                termRange,
                                dailyPreschoolTime
                            ),
                            dailyPreschoolTime
                        )
                    }
                }.also {
                    Audit.PreschoolAbsenceReport.log(
                        meta =
                            mapOf(
                                "unitId" to unitId,
                                "termStart" to termStart,
                                "termEnd" to termEnd,
                                "groupId" to groupId
                            )
                    )
                }
        }

    private fun calculateHourlyResults(
        absenceRows: List<PreschoolAbsenceRow>,
        deviationsByChild: Map<ChildId, List<ChildDailyAttendanceDeviation>>,
        dailyPreschoolTime: TimeRange
    ): List<ChildPreschoolAbsenceRow> {
        val hourlyRows =
            absenceRows.map { r ->
                val childAttendanceDeviationMinutes =
                    if (r.absenceType == AbsenceType.OTHER_ABSENCE) {
                        val childDeviations = deviationsByChild[r.childId] ?: emptyList()
                        childDeviations.sumOf { it.missingMinutes }
                    } else {
                        0
                    }
                PreschoolAbsenceReportRow(
                    r.childId,
                    r.firstName,
                    r.lastName,
                    r.absenceType,
                    (
                        r.absenceCount * dailyPreschoolTime.duration.toMinutes() +
                            childAttendanceDeviationMinutes
                    ).floorDiv(60)
                        .toInt()
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
                    hourlyTypeResults = k.value.associateBy({ it.absenceType }, { it.absenceHours })
                )
            }
    }
}

data class PreschoolAbsenceRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val absenceType: AbsenceType,
    val absenceCount: Int
)

data class PreschoolAbsenceReportRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val absenceType: AbsenceType,
    val absenceHours: Int
)

data class ChildPreschoolAbsenceRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val hourlyTypeResults: Map<AbsenceType, Int>
)

data class ChildDailyAttendanceDeviation(
    val missingMinutes: Int,
    val childId: ChildId,
    val date: LocalDate
)

fun getPreschoolAbsenceReportRowsForUnit(
    tx: Database.Read,
    daycareId: DaycareId,
    preschoolTerm: FiniteDateRange,
    absenceTypes: List<AbsenceType>
): List<PreschoolAbsenceRow> =
    tx
        .createQuery {
            sql(
                """
                WITH preschool_placement_children as
                         (select child.id,
                                 child.first_name,
                                 child.last_name,
                                 daterange(p.start_date, p.end_date, '[]') * ${bind(preschoolTerm)} as examination_range
                          from placement p
                                   join person child on p.child_id = child.id
                                   join daycare d
                                        on p.unit_id = d.id
                                            AND 'PRESCHOOL'::care_types = ANY (d.type)
                          where daterange(p.start_date, p.end_date, '[]') &&
                                ${bind(preschoolTerm)}
                            and p.type = ANY
                                ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
                            and p.unit_id = ${bind(daycareId)})
                select ppc.id            as child_id,
                       ppc.first_name,
                       ppc.last_name,
                       types.absence_type as absence_type,
                       count(ab.id)       as absence_count
                from preschool_placement_children ppc
                         left join unnest(${bind(absenceTypes)}::absence_type[]) as types (absence_type) on true
                         left join absence ab on ppc.examination_range @> ab.date and
                                                 ppc.id = ab.child_id and
                                                 ab.absence_type = types.absence_type
                group by ppc.id, ppc.first_name, ppc.last_name, types.absence_type;
                """.trimIndent()
            )
        }.toList<PreschoolAbsenceRow>()

fun getPreschoolAbsenceReportRowsForGroup(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId,
    preschoolTerm: FiniteDateRange,
    absenceTypes: List<AbsenceType>
): List<PreschoolAbsenceRow> =
    tx
        .createQuery {
            sql(
                """
            WITH preschool_group_placement_children as
         (select child.id,
                 child.first_name,
                 child.last_name,
                 daterange(dgp.start_date, dgp.end_date, '[]') * ${bind(preschoolTerm)} as examination_range
          from placement p
                   join person child on p.child_id = child.id
                   join daycare d
                        on p.unit_id = d.id
                            AND 'PRESCHOOL'::care_types = ANY (d.type)
                   join daycare_group_placement dgp
                        on p.id = dgp.daycare_placement_id
                            and dgp.daycare_group_id = ${bind(groupId)}
                            and daterange(dgp.start_date, dgp.end_date, '[]') &&
                                ${bind(preschoolTerm)}
          where daterange(p.start_date, p.end_date, '[]') &&
                ${bind(preschoolTerm)}
            and p.type = ANY
                ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
            and p.unit_id = ${bind(daycareId)})
select pgpc.id            as child_id,
       pgpc.first_name,
       pgpc.last_name,
       types.absence_type as absence_type,
       count(ab.id)       as absence_count
from preschool_group_placement_children pgpc
         left join unnest(${bind(absenceTypes)}::absence_type[]) as types (absence_type) on true
         left join absence ab on pgpc.examination_range @> ab.date and
                                 pgpc.id = ab.child_id and
                                 ab.absence_type = types.absence_type
group by pgpc.id, pgpc.first_name, pgpc.last_name, types.absence_type;
                """.trimIndent()
            )
        }.toList<PreschoolAbsenceRow>()

fun calculateDailyPreschoolAttendanceDeviationForUnit(
    tx: Database.Read,
    daycareId: DaycareId,
    preschoolTerm: FiniteDateRange,
    preschoolDailyServiceTime: TimeRange
): Map<ChildId, List<ChildDailyAttendanceDeviation>> {
    val endTime = preschoolDailyServiceTime.end.inner
    val startTime = preschoolDailyServiceTime.start.inner
    return tx
        .createQuery {
            sql(
                """
-- calculate child attendance deviation from standard daily preschool service time in minutes
select floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) as missing_minutes,
       intersection.date,
       intersection.child_id
from (select (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
              tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})) as range,
             ca.date,
             ca.child_id
      from placement p
               join daycare d
                    on p.unit_id = d.id
                        and 'PRESCHOOL'::care_types = ANY (d.type)
               join child_attendance ca
                    on ca.child_id = p.child_id
                        and daterange(p.start_date, p.end_date, '[]') * ${bind(preschoolTerm)} @> ca.date
                        and ca.end_time IS NOT NULL
                        and ca.unit_id = d.id
      -- pick out attendance intersecting preschool service time
      where tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})
        and daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        and p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
        and p.unit_id = ${bind(daycareId)}) intersection
group by intersection.date, intersection.child_id
-- filter out daily deviations below 20 minutes
having floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) >= 20;
                """.trimIndent()
            )
        }.toList<ChildDailyAttendanceDeviation>()
        .groupBy { it.childId }
}

fun calculateDailyPreschoolAttendanceDeviationForGroup(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId,
    preschoolTerm: FiniteDateRange,
    preschoolDailyServiceTime: TimeRange
): Map<ChildId, List<ChildDailyAttendanceDeviation>> {
    val endTime = preschoolDailyServiceTime.end.inner
    val startTime = preschoolDailyServiceTime.start.inner
    return tx
        .createQuery {
            sql(
                """
-- calculate child attendance deviation from standard daily preschool service time in minutes
select floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) as missing_minutes,
       intersection.date,
       intersection.child_id
from (select (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
              tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})) as range,
             ca.date,
             ca.child_id
      from placement p
               join daycare d
                    on p.unit_id = d.id
                        and 'PRESCHOOL'::care_types = ANY (d.type)
               join daycare_group_placement dgp
                    on p.id = dgp.daycare_placement_id
                        and daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(preschoolTerm)}
                        and dgp.daycare_group_id = ${bind(groupId)}
               join child_attendance ca
                    on ca.child_id = p.child_id
                        and daterange(dgp.start_date, dgp.end_date, '[]') * ${bind(preschoolTerm)} @> ca.date
                        and ca.end_time IS NOT NULL
                        and ca.unit_id = d.id
      -- pick out attendance intersecting preschool service time
      where tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})
        and daterange(p.start_date, p.end_date, '[]') && ${bind(preschoolTerm)}
        and p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
        and p.unit_id = ${bind(daycareId)}) intersection
group by intersection.date, intersection.child_id
-- filter out daily deviations below 20 minutes
having floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) >= 20;
                """.trimIndent()
            )
        }.toList<ChildDailyAttendanceDeviation>()
        .groupBy { it.childId }
}
