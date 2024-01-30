// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.occupancy.defaultOccupancyCoefficient
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.occupancy.workingDayHours
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RawReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/raw")
    fun getRawReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<RawReportRow> {
        if (to.isBefore(from)) throw BadRequest("Inverted time range")
        if (to.isAfter(from.plusDays(7))) throw BadRequest("Time range too long")

        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_RAW_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getRawRows(from, to)
                }
            }
            .also {
                Audit.RawReportRead.log(
                    meta = mapOf("from" to from, "to" to to, "count" to it.size)
                )
            }
    }
}

fun Database.Read.getRawRows(from: LocalDate, to: LocalDate): List<RawReportRow> {
    // language=sql
    val sql =
        """
WITH realtime_attendances AS (
    SELECT sar.group_id, ROUND(SUM(EXTRACT(EPOCH FROM (
            LEAST(sar.departed, timezone('Europe/Helsinki', (t::date + 1)::date::timestamp)) - GREATEST(sar.arrived, timezone('Europe/Helsinki', t::date::timestamp))
        )) / 3600 / $workingDayHours * sar.occupancy_coefficient / $defaultOccupancyCoefficient), 1) AS realized_caretakers
    FROM generate_series(:start_date, :end_date, '1 day') t
    JOIN (
        SELECT group_id, arrived, departed, occupancy_coefficient
        FROM staff_attendance_realtime
        WHERE departed IS NOT NULL
            AND type = ANY ('{"PRESENT", "OVERTIME", "JUSTIFIED_CHANGE"}')
        UNION ALL
        SELECT group_id, arrived, departed, occupancy_coefficient
        FROM staff_attendance_external
        WHERE departed IS NOT NULL
    ) sar ON (tstzrange(sar.arrived, sar.departed) && tstzrange(t::timestamp AT TIME ZONE 'Europe/Helsinki', (t::date+1)::timestamp AT TIME ZONE 'Europe/Helsinki'))
    GROUP BY sar.group_id
)
SELECT
    t::date as day,
    p.id as child_id,
    p.first_name,
    p.last_name,
    p.social_security_number IS NOT NULL AS has_social_security_number,
    p.date_of_birth,
    date_part('year', p.date_of_birth) as birth_year,
    date_part('year', age(t::date, p.date_of_birth)) as age,
    p.language,
    p.postal_code,
    p.post_office,

    pl.type as placement_type,

    pl.unit_id,
    u.name as unit_name,
    ca.name AS care_area,
    u.type as unit_type,
    u.provider_type as unit_provider_type,
    u.cost_center,

    dgp.daycare_group_id,
    dg.name as group_name,
    dc.amount as caretakers_planned,
    coalesce(sar.realized_caretakers, sa.count) as caretakers_realized,

    bc.unit_id as backup_unit_id,
    bc.group_id as backup_group_id,

    sn IS NOT NULL AS has_service_need,
    coalesce(sno.part_day, false) AS part_day,
    coalesce(sno.part_week, false) AS part_week,
    coalesce(sn.shift_care, 'NONE') = 'FULL' AS shift_care,
    coalesce(sno.daycare_hours_per_week, 0.0) AS hours_per_week,

    an IS NOT NULL as has_assistance_need,
    coalesce(an.capacity_factor, 1.0) as capacity_factor,
    coalesce(an.capacity_factor, 1.0) * (CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        WHEN date_part('year', age(t::date, p.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
        ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient)
    END) AS capacity,
    coalesce(an.capacity_factor, 1.0) * (CASE
        WHEN u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
        WHEN date_part('year', age(t::date, p.date_of_birth)) < 3 THEN coalesce(sno.realized_occupancy_coefficient_under_3y, default_sno.realized_occupancy_coefficient_under_3y)
        ELSE coalesce(sno.realized_occupancy_coefficient, default_sno.realized_occupancy_coefficient)
    END) AS realized_capacity,

    ab1.absence_type as absence_paid,
    ab2.absence_type as absence_free,
    7 as staff_dimensioning,
    holiday.date IS NOT NULL as is_holiday,
    date_part('isodow', t) = ANY(ARRAY[1, 2, 3, 4, 5]) as is_weekday
FROM generate_series(:start_date, :end_date, '1 day') t
JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> t::date
JOIN daycare u on u.id = pl.unit_id
JOIN care_area ca ON ca.id = u.care_area_id
JOIN person p ON p.id = pl.child_id
LEFT JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
LEFT JOIN daycare_group dg on dg.id = dgp.daycare_group_id
LEFT JOIN daycare_caretaker dc on dg.id = dc.group_id AND daterange(dc.start_date, dc.end_date, '[]') @> t::date
LEFT JOIN realtime_attendances sar ON dg.id = sar.group_id
LEFT JOIN staff_attendance sa on dg.id = sa.group_id AND sa.date = t::date
LEFT JOIN backup_care bc on bc.child_id = p.id AND daterange(bc.start_date, bc.end_date, '[]') @> t::date
LEFT JOIN daycare bcu on bc.unit_id = bcu.id
LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
LEFT JOIN service_need_option sno on sn.option_id = sno.id
LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
LEFT JOIN assistance_factor an ON an.child_id = p.id AND an.valid_during @> t::date
LEFT JOIN absence ab1 on ab1.child_id = p.id and ab1.date = t::date AND ab1.category = 'BILLABLE'
LEFT JOIN absence ab2 on ab2.child_id = p.id and ab2.date = t::date AND ab2.category = 'NONBILLABLE'
LEFT JOIN holiday ON t = holiday.date AND NOT (u.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7] OR bcu.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7])
WHERE (date_part('isodow', t) = ANY(u.operation_days) OR date_part('isodow', t) = ANY(bcu.operation_days)) AND holiday.date IS NULL
ORDER BY p.id, t
"""

    return createQuery(sql)
        .bind("start_date", from)
        .bind("end_date", to)
        .registerColumnMapper(UnitType.JDBI_COLUMN_MAPPER)
        .toList<RawReportRow>()
}

data class RawReportRow(
    val day: LocalDate,
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val hasSocialSecurityNumber: Boolean,
    val dateOfBirth: LocalDate,
    val postalCode: String,
    val postOffice: String,
    val age: Int,
    val language: String?,
    val placementType: PlacementType,
    val unitId: DaycareId,
    val unitName: String,
    val careArea: String,
    val unitType: UnitType?,
    val unitProviderType: ProviderType,
    val costCenter: String?,
    val daycareGroupId: GroupId?,
    val groupName: String?,
    val caretakersPlanned: Double?,
    val caretakersRealized: Double?,
    val backupUnitId: DaycareId?,
    val backupGroupId: GroupId?,
    val hasServiceNeed: Boolean,
    val partDay: Boolean,
    val partWeek: Boolean,
    val shiftCare: Boolean,
    val hoursPerWeek: Double,
    val hasAssistanceNeed: Boolean,
    val capacityFactor: Double,
    val capacity: Double,
    val realizedCapacity: Double,
    val absencePaid: AbsenceType?,
    val absenceFree: AbsenceType?,
    val staffDimensioning: Int,
    val isWeekday: Boolean,
    val isHoliday: Boolean
)
