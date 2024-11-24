// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import kotlin.math.roundToInt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee-mobile/units")
class MobileUnitController(private val accessControl: AccessControl) {
    @GetMapping("/{unitId}")
    fun getUnitInfo(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): UnitInfo {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_MOBILE_INFO,
                        unitId,
                    )
                    tx.fetchUnitInfo(unitId, clock.today())
                }
            }
            .also { Audit.UnitRead.log(targetId = AuditId(unitId)) }
    }

    @GetMapping("/stats")
    fun getUnitStats(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitIds: List<DaycareId> = emptyList(),
    ): List<UnitStats> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_MOBILE_STATS,
                        unitIds,
                    )
                    tx.fetchUnitStats(unitIds, clock.today())
                }
            }
            .also { Audit.UnitRead.log(targetId = AuditId(unitIds)) }
    }
}

data class UnitInfo(
    val id: DaycareId,
    val name: String,
    val groups: List<GroupInfo>,
    val staff: List<Staff>,
    val features: List<PilotFeature>,
    val utilization: Double,
    val isOperationalDate: Boolean,
)

data class GroupInfo(val id: GroupId, val name: String, val utilization: Double)

data class Staff(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val pinSet: Boolean,
    val pinLocked: Boolean,
    val groups: List<GroupId>,
)

fun Database.Read.fetchUnitInfo(unitId: DaycareId, date: LocalDate): UnitInfo {
    data class UnitBasics(
        val id: DaycareId,
        val name: String,
        val features: List<PilotFeature>,
        val operationDays: Set<Int>,
        val shiftCareOperationDays: Set<Int>?,
        val shiftCareOpenOnHolidays: Boolean,
    )

    val unit =
        createQuery {
                sql(
                    """
        SELECT id, name, enabled_pilot_features AS features, 
            operation_days, shift_care_operation_days, shift_care_open_on_holidays
        FROM daycare u
        WHERE u.id = ${bind(unitId)}
        """
                )
            }
            .exactlyOneOrNull<UnitBasics>() ?: throw NotFound("Unit $unitId not found")

    val isOperationalDate =
        (unit.shiftCareOperationDays ?: unit.operationDays).contains(date.dayOfWeek.value) &&
            (unit.shiftCareOpenOnHolidays || !getHolidays(date.toFiniteDateRange()).contains(date))

    data class TempGroupInfo(
        val id: GroupId,
        val name: String,
        val utilization: Double,
        val staffCapacity: Double,
        val childCapacity: Double,
    )

    val tmpGroups =
        createQuery {
                sql(
                    """
        WITH child AS (
            SELECT
                pl.group_id,
                SUM(COALESCE(an.capacity_factor, 1) * CASE
                    WHEN dc.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
                    WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.realized_occupancy_coefficient_under_3y, default_sno.realized_occupancy_coefficient_under_3y)
                    ELSE coalesce(sno.realized_occupancy_coefficient, default_sno.realized_occupancy_coefficient)
                END) AS capacity
            FROM child_attendance ca
                JOIN daycare dc ON dc.id = ca.unit_id
                JOIN person ch ON ch.id = ca.child_id
                JOIN (
                    -- children with backup care in this unit
                    SELECT bc.child_id, bc.group_id, pl.id AS placement_id, pl.type AS placement_type
                    FROM backup_care bc
                    JOIN placement pl ON pl.child_id = bc.child_id
                    WHERE bc.unit_id = ${bind(unitId)}
                        AND daterange(bc.start_date, bc.end_date, '[]') @> ${bind(date)}
                        AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
        
                    UNION ALL
        
                    -- children placed into this unit without backup care
                    SELECT pl.child_id, dgp.daycare_group_id AS group_id, pl.id AS placement_id, pl.type AS placement_type
                    FROM placement pl
                    JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = pl.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(date)}
                    WHERE pl.unit_id = ${bind(unitId)} AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)} AND NOT EXISTS (
                        SELECT 1 FROM backup_care bc
                        WHERE daterange(bc.start_date, bc.end_date, '[]') @> ${bind(date)} AND bc.child_id = pl.child_id
                    )
                ) pl ON pl.child_id = ca.child_id
                LEFT JOIN service_need sn on sn.placement_id = pl.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
                LEFT JOIN service_need_option sno on sn.option_id = sno.id
                LEFT JOIN service_need_option default_sno on placement_type = default_sno.valid_placement_type AND default_sno.default_option
                LEFT JOIN assistance_factor an ON an.child_id = ca.child_id AND an.valid_during @> ${bind(date)}
            WHERE ca.unit_id = ${bind(unitId)} AND ca.end_time IS NULL
            GROUP BY pl.group_id
        ), staff AS (
            SELECT sa.group_id as group_id, SUM(sa.capacity) AS capacity
            FROM (
                SELECT sa.group_id, 7 * sa.count AS capacity, FALSE AS realtime
                FROM staff_attendance sa
                    JOIN daycare_group dg ON dg.id = sa.group_id
                    JOIN daycare dc ON dc.id = dg.daycare_id
                WHERE sa.date = ${bind(date)} AND dc.id = ${bind(unitId)}
                  AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(date)}
                  AND NOT 'REALTIME_STAFF_ATTENDANCE' = ANY(dc.enabled_pilot_features)
        
                UNION ALL
        
                SELECT sa.group_id, sa.occupancy_coefficient AS capacity, TRUE AS realtime
                FROM staff_attendance_realtime sa
                    JOIN daycare_group dg ON dg.id = sa.group_id
                    JOIN daycare dc ON dc.id = dg.daycare_id
                WHERE sa.departed IS NULL AND dc.id = ${bind(unitId)}
                  AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(date)}
                  AND 'REALTIME_STAFF_ATTENDANCE' = ANY(dc.enabled_pilot_features)
                  AND sa.type NOT IN ('OTHER_WORK', 'TRAINING')
        
                UNION ALL
        
                SELECT sa.group_id, sa.occupancy_coefficient AS capacity, TRUE AS realtime
                FROM staff_attendance_external sa
                    JOIN daycare_group dg ON dg.id = sa.group_id
                    JOIN daycare dc ON dc.id = dg.daycare_id
                WHERE sa.departed IS NULL AND dc.id = ${bind(unitId)}
                  AND daterange(dg.start_date, dg.end_date, '[]') @> ${bind(date)}
                  AND 'REALTIME_STAFF_ATTENDANCE' = ANY(dc.enabled_pilot_features)
            ) sa
            GROUP BY sa.group_id
        )
        SELECT
            g.id,
            g.name,
            coalesce(s.capacity, 0) AS staff_capacity,
            coalesce(c.capacity, 0) AS child_capacity,
            CASE
                WHEN c.capacity IS NULL OR c.capacity = 0 THEN 0.0
                WHEN s.capacity IS NULL OR s.capacity = 0 THEN 'Infinity'::REAL
                ELSE round(c.capacity / s.capacity * 100, 1)
            END AS utilization
        FROM daycare u
            JOIN daycare_group g ON u.id = g.daycare_id AND daterange(g.start_date, g.end_date, '[]') @> ${bind(date)}
            LEFT JOIN child c ON c.group_id = g.id
            LEFT JOIN staff s ON s.group_id = g.id
        WHERE u.id = ${bind(unitId)};
        """
                )
            }
            .toList<TempGroupInfo>()

    val totalChildCapacity = tmpGroups.sumOf { it.childCapacity }
    val totalStaffCapacity = tmpGroups.sumOf { it.staffCapacity }
    val unitUtilization =
        if (totalStaffCapacity > 0) {
            (totalChildCapacity / totalStaffCapacity * 1000).roundToInt() / 10.0
        } else {
            Double.POSITIVE_INFINITY
        }
    val groups = tmpGroups.map { GroupInfo(it.id, it.name, it.utilization) }.sortedBy { it.name }

    val staff =
        createQuery {
                sql(
                    """
        SELECT
            COALESCE(e.preferred_first_name, e.first_name) AS first_name,
            e.last_name,
            e.id,
            char_length(COALESCE(pin.pin, '')) > 0 AS pin_set,
            COALESCE(pin.locked, FALSE) pin_locked,
            coalesce(groups, array[]::uuid[]) AS groups
        FROM daycare_acl acl
            LEFT JOIN employee e ON acl.employee_id = e.id
            LEFT JOIN employee_pin pin ON acl.employee_id = pin.user_id
        LEFT JOIN (
            SELECT employee_id, array_agg(daycare_group_id) AS groups
            FROM daycare_group_acl dga
            JOIN daycare_group dg ON dga.daycare_group_id = dg.id
            WHERE dg.daycare_id = ${bind(unitId)}
            GROUP BY employee_id
        ) group_acl ON acl.employee_id = group_acl.employee_id
        WHERE acl.daycare_id = ${bind(unitId)} AND e.active
        """
                )
            }
            .toList<Staff>()

    return UnitInfo(
        id = unit.id,
        name = unit.name,
        groups = groups,
        staff = staff,
        features = unit.features,
        utilization = unitUtilization,
        isOperationalDate,
    )
}

data class UnitStats(
    val id: DaycareId,
    val name: String,
    val presentChildren: Int,
    val totalChildren: Int,
    val presentStaff: Double,
    val presentStaffOther: Double,
    val totalStaff: Double,
    val utilization: Double,
)

fun Database.Read.fetchUnitStats(unitIds: List<DaycareId>, date: LocalDate): List<UnitStats> =
    createQuery {
            sql(
                """
WITH present_children AS (
    SELECT
        ca.unit_id,
        SUM(COALESCE(an.capacity_factor, 1) * CASE 
            WHEN dc.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
            WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.realized_occupancy_coefficient_under_3y, default_sno.realized_occupancy_coefficient_under_3y)
            ELSE coalesce(sno.realized_occupancy_coefficient, default_sno.realized_occupancy_coefficient)
        END) AS capacity,
        count(*) as count
    FROM child_attendance ca
    JOIN daycare dc ON dc.id = ca.unit_id
    JOIN person ch ON ch.id = ca.child_id
    LEFT JOIN placement pl on pl.child_id = ca.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
    LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
    LEFT JOIN service_need_option sno on sn.option_id = sno.id
    LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
    LEFT JOIN assistance_factor an ON an.child_id = ca.child_id AND an.valid_during @> ${bind(date)}
    WHERE ca.unit_id = ANY(${bind(unitIds)}) AND ca.end_time IS NULL
    GROUP BY ca.unit_id
), total_children AS (
    SELECT unit_id, count(child_id)
    FROM realized_placement_one(${bind(date)})
    WHERE unit_id = ANY(${bind(unitIds)})
    GROUP BY unit_id
), present_staff AS (
    SELECT g.daycare_id AS unit_id, sum(sa.capacity) AS capacity, sum(sa.count) AS count, sum(sa.count_other) AS count_other
    FROM daycare_group g
    JOIN daycare ON g.daycare_id = daycare.id
    JOIN (
        SELECT sa.group_id, 7 * sa.count as capacity, sa.count AS count, sa.count_other, FALSE AS realtime
        FROM staff_attendance sa
        WHERE sa.date = ${bind(date)}

        UNION ALL

        SELECT sa.group_id, sa.occupancy_coefficient as capacity, 1 AS count, 0 AS count_other, TRUE AS realtime
        FROM staff_attendance_realtime sa
        WHERE sa.departed IS NULL AND sa.occupancy_coefficient > 0

        UNION ALL

        SELECT sa.group_id, sa.occupancy_coefficient as capacity, 1 AS count, 0 AS count_other, TRUE AS realtime
        FROM staff_attendance_external sa
        WHERE sa.departed IS NULL AND sa.occupancy_coefficient > 0
    ) sa ON sa.group_id = g.id AND realtime = ('REALTIME_STAFF_ATTENDANCE' = ANY(daycare.enabled_pilot_features))
    WHERE g.daycare_id = ANY(${bind(unitIds)}) AND daterange(g.start_date, g.end_date, '[]') @> ${bind(date)}
    GROUP BY g.daycare_id
), total_staff AS (
    SELECT g.daycare_id AS unit_id, sum(dc.amount) AS count
    FROM daycare_group g
    JOIN daycare_caretaker dc ON dc.group_id = g.id AND daterange(dc.start_date, dc.end_date, '[]') @> ${bind(date)}
    WHERE g.daycare_id = ANY(${bind(unitIds)}) AND daterange(g.start_date, g.end_date, '[]') @> ${bind(date)}
    GROUP BY g.daycare_id
)
SELECT
    u.id, u.name,
    coalesce(pc.count, 0) AS present_children,
    coalesce(tc.count, 0) AS total_children,
    coalesce(ps.count, 0) AS present_staff,
    coalesce(ps.count_other, 0) AS present_staff_other,
    coalesce(ts.count, 0) AS total_staff,
    CASE
        WHEN pc.capacity IS NULL OR pc.capacity = 0 THEN 0.0
        WHEN ps.capacity IS NULL OR ps.capacity = 0 THEN 'Infinity'::REAL
        ELSE round(pc.capacity / ps.capacity * 100, 1)
    END AS utilization
FROM daycare u
LEFT JOIN present_children pc ON pc.unit_id = u.id
LEFT JOIN total_children tc ON tc.unit_id = u.id
LEFT JOIN present_staff ps ON ps.unit_id = u.id
LEFT JOIN total_staff ts ON ts.unit_id = u.id
WHERE u.id = ANY(${bind(unitIds)})
"""
            )
        }
        .toList()
