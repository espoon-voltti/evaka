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
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
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
    private val accessControl: AccessControl,
) {

    @GetMapping("/employee/reports/preschool-absence")
    fun getPreschoolAbsenceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam groupId: GroupId?,
        @RequestParam termId: PreschoolTermId
    ): List<ChildPreschoolAbsenceRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_PRESCHOOL_ABSENCE_REPORT,
                        unitId
                    )
                    val daycare = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")

                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val dailyPreschoolTime =
                        daycare.dailyPreschoolTime
                            ?: TimeRange(LocalTime.of(9, 0, 0, 0), LocalTime.of(13, 0, 0, 0))
                    val absenceRows = getPreschoolAbsenceReportRows(tx, daycare.id, groupId, termId)
                    val deviationsByChild =
                        calculateDailyPreschoolAttendanceDeviation(
                                tx,
                                unitId,
                                groupId,
                                termId,
                                dailyPreschoolTime
                            )
                            .groupBy { it.childId }

                    val hourlyRows =
                        absenceRows.map { r ->
                            val childAttendanceDeviationMinutes =
                                if (r.absenceType == AbsenceType.OTHER_ABSENCE) {
                                    val childDeviations =
                                        deviationsByChild[r.childId] ?: emptyList()
                                    childDeviations.sumOf { it.missingMinutes }
                                } else 0
                            PreschoolAbsenceReportRow(
                                r.childId,
                                r.firstName,
                                r.lastName,
                                r.absenceType,
                                (r.absenceCount * dailyPreschoolTime.duration.toMinutes() +
                                        childAttendanceDeviationMinutes)
                                    .floorDiv(60)
                                    .toInt()
                            )
                        }

                    hourlyRows
                        .groupBy { row -> row.childId }
                        .entries
                        .map { k ->
                            ChildPreschoolAbsenceRow(
                                childId = k.key,
                                firstName = k.value[0].firstName,
                                lastName = k.value[0].lastName,
                                hourlyTypeResults =
                                    k.value.associateBy({ it.absenceType }, { it.absenceHours })
                            )
                        }
                }
                .also {
                    Audit.PreschoolAbsenceReport.log(
                        meta = mapOf("unitId" to unitId, "termId" to termId, "groupId" to groupId)
                    )
                }
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

fun getPreschoolAbsenceReportRows(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId?,
    termId: PreschoolTermId
): List<PreschoolAbsenceRow> {
    val preschoolAbsenceTypes =
        AbsenceType.entries.filter {
            when (it) {
                AbsenceType.OTHER_ABSENCE,
                AbsenceType.SICKLEAVE,
                AbsenceType.UNKNOWN_ABSENCE -> true
                else -> false
            }
        }
    return tx.createQuery {
            sql(
                """
WITH preschool_students as (select child.id, child.first_name, child.last_name, dgp.daycare_group_id
                            from preschool_term pt
                                     join placement p
                                          on daterange(p.start_date, p.end_date, '[]') && pt.finnish_preschool and
                                             p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[]) and
                                             p.unit_id = ${bind(daycareId)}
                                     join person child on p.child_id = child.id
                                     join daycare d on p.unit_id = d.id AND 'PRESCHOOL'::care_types = ANY (d.type)
                                     left join daycare_group_placement dgp on p.id = dgp.daycare_placement_id and
                                                                              daterange(dgp.start_date, dgp.end_date, '[]') &&
                                                                              pt.finnish_preschool
                                     where (${bind(groupId)} IS NULL OR ${bind(groupId)} = dgp.daycare_group_id)
                                     and pt.id = ${bind(termId)})
select student.id as child_id, student.first_name, student.last_name, types.absence_type as absence_type, count(ab.id) as absence_count
from preschool_students student
         join preschool_term pt on pt.id = ${bind(termId)}
         left join unnest(${bind(preschoolAbsenceTypes)}::absence_type[]) as types(absence_type) on true
         left join absence ab on pt.finnish_preschool @> ab.date and
                                 student.id = ab.child_id and
                                 ab.absence_type = types.absence_type
group by student.id, student.first_name, student.last_name, types.absence_type;
    """
                    .trimIndent()
            )
        }
        .toList<PreschoolAbsenceRow>()
}

fun calculateDailyPreschoolAttendanceDeviation(
    tx: Database.Read,
    daycareId: DaycareId,
    groupId: GroupId?,
    termId: PreschoolTermId,
    preschoolDailyServiceTime: TimeRange,
): List<ChildDailyAttendanceDeviation> {
    val endTime = preschoolDailyServiceTime.end.inner
    val startTime = preschoolDailyServiceTime.start.inner
    return tx.createQuery {
            sql(
                """
-- calculate child attendance deviation from standard daily preschool service time in minutes
select floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) as missing_minutes,
       intersection.date,
       intersection.child_id
-- target attendances in
-- - given preschool unit 
-- - during given preschool term 
-- - for children in preschool placements
-- - and optionally in given group placement
from (select (tsrange(ca.date + ca.start_time, ca.date + ca.end_time) *
              tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})) as range,
             ca.date,
             ca.child_id
      from preschool_term pt
               join placement p
                    on daterange(p.start_date, p.end_date, '[]') && pt.finnish_preschool
                        and p.type = ANY ('{PRESCHOOL, PRESCHOOL_DAYCARE}'::placement_type[])
                        and p.unit_id = ${bind(daycareId)}
               join daycare d
                    on p.unit_id = d.id
                        and 'PRESCHOOL'::care_types = ANY (d.type)
               left join daycare_group_placement dgp
                         on p.id = dgp.daycare_placement_id
                             and daterange(dgp.start_date, dgp.end_date, '[]') && pt.finnish_preschool
               join child_attendance ca
                    on ca.child_id = p.child_id
                        and pt.finnish_preschool @> ca.date
                        and ca.end_time IS NOT NULL
      -- pick out attendance intersecting preschool service time
      where tsrange(ca.date + ca.start_time, ca.date + ca.end_time) &&
            tsrange(ca.date + ${bind(startTime)}, ca.date + ${bind(endTime)})
        and (${bind(groupId)} IS NULL OR ${bind(groupId)} = dgp.daycare_group_id)
        and pt.id = ${bind(termId)}) intersection
group by intersection.date, intersection.child_id
-- filter out daily deviations below 20 minutes
having floor(extract(EPOCH FROM (${bind(endTime)} - ${bind(startTime)}) -
                                sum((upper(intersection.range) - lower(intersection.range)))) /
             60) > 19;
    """
                    .trimIndent()
            )
        }
        .toList<ChildDailyAttendanceDeviation>()
}
