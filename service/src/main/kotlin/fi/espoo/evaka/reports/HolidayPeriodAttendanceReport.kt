// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.holidayperiod.getHolidayPeriod
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class HolidayPeriodAttendanceReport(private val accessControl: AccessControl) {

    @GetMapping("/employee/reports/holiday-period-attendance")
    fun getHolidayPeriodAttendanceReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam unitId: DaycareId,
        @RequestParam periodId: HolidayPeriodId,
    ): List<HolidayPeriodAttendanceReportRow> {
        return db.connect { dbc ->
            dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val unit = tx.getDaycare(unitId) ?: throw BadRequest("No such unit")
                    val holidayPeriod =
                        tx.getHolidayPeriod(periodId) ?: throw BadRequest("No such holiday period")

                    val periodDays =
                        holidayPeriod.period
                            .dates()
                            .filter { unit.operationTimes[it.dayOfWeek.value - 1] != null }
                            .toList()
                    val noResponseChildrenByDate =
                        tx.getNoResponseChildren(periodDays, unitId).groupBy { it.date }

                    val holidayPeriodReservationData =
                        tx.getHolidayPeriodReservationData(unitId, periodDays)

                    val holidayPeriodAbsenceData =
                        tx.getHolidayPeriodAbsenceData(unitId, periodDays).associate {
                            it.date to it.absenceCount
                        }

                    val dataByDate = holidayPeriodReservationData.groupBy { r -> r.date }
                    periodDays.map { date ->
                        val rows = dataByDate[date]
                        val noResponses = noResponseChildrenByDate[date] ?: emptyList()
                        if (rows == null) {
                            HolidayPeriodAttendanceReportRow(
                                presentChildren = emptyList(),
                                assistanceChildren = emptyList(),
                                absentCount = holidayPeriodAbsenceData[date] ?: 0,
                                requiredStaff = 0,
                                date = date,
                                noResponseChildren = noResponses.map { it.child },
                                presentOccupancyCoefficient = BigDecimal.ZERO,
                            )
                        } else {
                            val dailyOccupancyCoefficient =
                                rows.sumOf {
                                    (it.assistanceCoefficient ?: BigDecimal.ONE) *
                                        it.serviceNeedCoefficient
                                }
                            HolidayPeriodAttendanceReportRow(
                                date = date,
                                presentChildren =
                                    rows.map { r ->
                                        ChildWithName(
                                            id = r.childId,
                                            firstName = r.firstName,
                                            lastName = r.lastName,
                                        )
                                    },
                                assistanceChildren =
                                    rows
                                        .filter { it.assistanceCoefficient != null }
                                        .map { r ->
                                            ChildWithName(
                                                id = r.childId,
                                                firstName = r.firstName,
                                                lastName = r.lastName,
                                            )
                                        },
                                presentOccupancyCoefficient =
                                    dailyOccupancyCoefficient.setScale(3, RoundingMode.HALF_UP),
                                absentCount = holidayPeriodAbsenceData[date] ?: 0,
                                requiredStaff =
                                    dailyOccupancyCoefficient
                                        .divide(
                                            occupancyCoefficientSeven,
                                            MathContext(1, RoundingMode.UP),
                                        )
                                        .setScale(0, RoundingMode.UP)
                                        .toInt(),
                                noResponseChildren = noResponses.map { it.child },
                            )
                        }
                    }
                }
                .also {
                    Audit.HolidayPeriodAttendanceReport.log(
                        meta = mapOf("unitId" to unitId, "periodId" to periodId)
                    )
                }
        }
    }
}

data class ChildWithName(val id: PersonId, val firstName: String, val lastName: String)

data class DailyNoResponseRow(@Nested("child") val child: ChildWithName, val date: LocalDate)

data class HolidayPeriodAttendanceReportRow(
    val date: LocalDate,
    val presentChildren: List<ChildWithName>,
    val assistanceChildren: List<ChildWithName>,
    val presentOccupancyCoefficient: BigDecimal,
    val requiredStaff: Int,
    val absentCount: Int,
    val noResponseChildren: List<ChildWithName>,
)

data class HolidayPeriodReservationDataRow(
    val date: LocalDate,
    val childId: PersonId,
    val firstName: String,
    val lastName: String,
    val assistanceCoefficient: BigDecimal?,
    val serviceNeedCoefficient: BigDecimal,
)

data class DailyAbsenceCountRow(val date: LocalDate, val absenceCount: Int)

fun Database.Read.getNoResponseChildren(
    periodDays: List<LocalDate>,
    daycareId: DaycareId,
): List<DailyNoResponseRow> =
    createQuery {
            sql(
                """
SELECT p.id         AS child_id,
       p.first_name AS child_first_name, 
       p.last_name  AS child_last_name, 
       pd.day       AS date
FROM (SELECT unnest(${bind(periodDays)}) AS day) pd
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> pd.day
JOIN person p ON pl.child_id = p.id
WHERE pl.unit_id = ${bind(daycareId)} 
    AND NOT EXISTS (SELECT FROM attendance_reservation ar
                        WHERE pd.day = ar.date 
                            AND ar.child_id = pl.child_id)
    AND NOT EXISTS (SELECT FROM absence ab
                        WHERE pd.day = ab.date 
                            AND ab.child_id = pl.child_id)
        """
            )
        }
        .toList<DailyNoResponseRow>()

fun Database.Read.getHolidayPeriodReservationData(
    daycareId: DaycareId,
    periodDays: List<LocalDate>,
): List<HolidayPeriodReservationDataRow> =
    createQuery {
            sql(
                """
SELECT pd.day                             AS date,
       pl.child_id,
       p.first_name,
       p.last_name,
       CASE
           WHEN date_part('year', age(pd.day, p.date_of_birth)) < 3
               THEN coalesce(sno.realized_occupancy_coefficient_under_3y,
                             default_sno.realized_occupancy_coefficient_under_3y)
           ELSE coalesce(sno.realized_occupancy_coefficient, default_sno.realized_occupancy_coefficient)
           END                            AS service_need_coefficient,
       af.capacity_factor AS assistance_coefficient
FROM (SELECT unnest(${bind(periodDays)}) AS day) pd
         JOIN placement pl
              ON pl.unit_id = ${bind(daycareId)}
                  AND daterange(pl.start_date, pl.end_date, '[]') @> pd.day
         JOIN attendance_reservation ar ON date = pd.day AND ar.child_id = pl.child_id
         JOIN person p ON pl.child_id = p.id
         LEFT JOIN service_need sn ON pl.id = sn.placement_id
         LEFT JOIN service_need_option sno ON sn.option_id = sno.id
         LEFT JOIN service_need_option default_sno
              ON pl.type = default_sno.valid_placement_type 
                  AND default_sno.default_option
         LEFT JOIN assistance_factor af
                         ON pl.child_id = af.child_id
                             AND af.valid_during @> pd.day
        """
            )
        }
        .toList<HolidayPeriodReservationDataRow>()

fun Database.Read.getHolidayPeriodAbsenceData(
    daycareId: DaycareId,
    periodDays: List<LocalDate>,
): List<DailyAbsenceCountRow> =
    createQuery {
            sql(
                """
SELECT pd.day             AS date,
       count(pl.child_id) AS absence_count
FROM (SELECT unnest(${bind(periodDays)}) AS day) pd
    JOIN placement pl
        ON pl.unit_id = ${bind(daycareId)}
            AND daterange(pl.start_date, pl.end_date, '[]') @> pd.day
    JOIN absence ab 
        ON ab.child_id = pl.child_id 
            AND ab.date = pd.day
GROUP BY pd.day;
        """
            )
        }
        .toList<DailyAbsenceCountRow>()
