// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import kotlin.math.roundToInt

@RestController
@RequestMapping("/mobile/units")
class MobileUnitController(private val accessControl: AccessControl) {
    @GetMapping("/{unitId}")
    fun getUnitInfo(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestParam useRealtimeStaffAttendance: Boolean?
    ): UnitInfo {
        Audit.UnitRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_MOBILE_INFO, unitId)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.fetchUnitInfo(
                    unitId,
                    evakaClock.today(),
                    useRealtimeStaffAttendance ?: false
                )
            }
        }
    }

    @GetMapping("/stats")
    fun getUnitStats(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestParam unitIds: List<DaycareId>,
        @RequestParam useRealtimeStaffAttendance: Boolean
    ): List<UnitStats> {
        Audit.UnitRead.log(targetId = unitIds)
        accessControl.requirePermissionFor(user, Action.Unit.READ_MOBILE_STATS, unitIds)
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.fetchUnitStats(
                    unitIds,
                    evakaClock.today(),
                    useRealtimeStaffAttendance
                )
            }
        }
    }
}

data class UnitInfo(
    val id: DaycareId,
    val name: String,
    val groups: List<GroupInfo>,
    val staff: List<Staff>,
    val features: List<PilotFeature>,
    val utilization: Double
)

data class GroupInfo(
    val id: GroupId,
    val name: String,
    val utilization: Double
)

data class Staff(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val pinSet: Boolean,
    val pinLocked: Boolean,
    val groups: List<GroupId>
)

fun Database.Read.fetchUnitInfo(unitId: DaycareId, date: LocalDate, useRealtimeStaffAttendance: Boolean): UnitInfo {
    data class UnitBasics(
        val id: DaycareId,
        val name: String,
        val features: List<PilotFeature>
    )
    // language=sql
    val unitSql =
        """
        SELECT u.id, u.name, u.enabled_pilot_features AS features
        FROM daycare u
        WHERE u.id = :unitId
        """.trimIndent()

    val unit = createQuery(unitSql)
        .bind("unitId", unitId)
        .mapTo<UnitBasics>()
        .list().firstOrNull() ?: throw NotFound("Unit $unitId not found")

    // language=sql
    val groupsSql =
        """
        WITH child AS (
            SELECT
                dgp.daycare_group_id as group_id,
                SUM(COALESCE(an.capacity_factor, 1) * CASE
                    WHEN dc.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
                    WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
                    ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient)
                END) AS capacity
            FROM child_attendance ca
                JOIN daycare dc ON dc.id = ca.unit_id
                JOIN person ch ON ch.id = ca.child_id
                JOIN placement pl on pl.child_id = ca.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> :date
                LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> :date
                LEFT JOIN service_need_option sno on sn.option_id = sno.id
                LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
                LEFT JOIN assistance_need an on an.child_id = ca.child_id AND daterange(an.start_date, an.end_date, '[]') @> :date
                JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = pl.id
            WHERE ca.unit_id = :unitId AND ca.end_time IS NULL
            GROUP BY dgp.daycare_group_id
        ), staff AS (
            SELECT sa.group_id as group_id, SUM(sa.capacity) AS capacity
            FROM daycare_group g
            JOIN (
                SELECT sa.group_id, 7 * (sa.count + sa.count_other) AS capacity FROM staff_attendance sa
                WHERE NOT :useRealtimeStaffAttendance AND sa.date = :date
        
                UNION ALL
        
                SELECT sa.group_id, sa.occupancy_coefficient AS capacity FROM staff_attendance_realtime sa
                WHERE :useRealtimeStaffAttendance AND sa.arrived IS NOT NULL AND sa.departed IS NULL
        
                UNION ALL
        
                SELECT sa.group_id, sa.occupancy_coefficient AS capacity FROM staff_attendance_external sa
                WHERE :useRealtimeStaffAttendance AND sa.arrived IS NOT NULL AND sa.departed IS NULL
            ) sa ON sa.group_id = g.id
            WHERE g.daycare_id = :unitId AND daterange(g.start_date, g.end_date, '[]') @> :date
            GROUP BY sa.group_id
        )
        SELECT g.id, g.name, s.capacity AS staff_capacity, c.capacity AS child_capacity,
               CASE
                   WHEN c.capacity IS NULL OR c.capacity = 0 THEN 0.0
                   WHEN s.capacity IS NULL OR s.capacity = 0 THEN 'Infinity'::REAL
                   ELSE round(c.capacity / s.capacity * 100, 1)
               END AS utilization
        FROM daycare u
            JOIN daycare_group g ON u.id = g.daycare_id AND daterange(g.start_date, g.end_date, '[]') @> :date
            LEFT JOIN child c ON c.group_id = g.id
            LEFT JOIN staff s ON s.group_id = g.id
        WHERE u.id = :unitId
        """.trimIndent()

    data class TempGroupInfo(
        val id: GroupId,
        val name: String,
        val utilization: Double,
        val staffCapacity: Int,
        val childCapacity: Int
    )

    val tmpGroups = createQuery(groupsSql)
        .bind("unitId", unitId)
        .bind("date", date)
        .bind("useRealtimeStaffAttendance", useRealtimeStaffAttendance)
        .mapTo<TempGroupInfo>()
        .list()

    val totalChildCapacity = tmpGroups.sumOf { it.childCapacity }.toDouble()
    val totalStaffCapacity = tmpGroups.sumOf { it.staffCapacity }.toDouble()
    val unitUtilization =
        if (totalStaffCapacity > 0) {
            (totalChildCapacity / totalStaffCapacity * 1000).roundToInt() / 10.0
        } else {
            Double.POSITIVE_INFINITY
        }
    val groups = tmpGroups.map { GroupInfo(it.id, it.name, it.utilization) }

    val staff = createQuery(
        """
        SELECT
            e.first_name,
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
            WHERE dg.daycare_id = :id
            GROUP BY employee_id
        ) group_acl ON acl.employee_id = group_acl.employee_id
        WHERE acl.daycare_id = :id
        """.trimIndent()
    )
        .bind("id", unitId)
        .mapTo<Staff>()
        .list()

    return UnitInfo(
        id = unit.id,
        name = unit.name,
        groups = groups,
        staff = staff,
        features = unit.features,
        utilization = unitUtilization
    )
}

data class UnitStats(
    val id: DaycareId,
    val name: String,
    val presentChildren: Int,
    val totalChildren: Int,
    val presentStaff: Int,
    val totalStaff: Int,
    val utilization: Double
)

fun Database.Read.fetchUnitStats(
    unitIds: List<DaycareId>,
    date: LocalDate,
    useRealtimeStaffAttendance: Boolean
): List<UnitStats> {
    return createQuery(
        """
WITH present_children AS (
    SELECT
        ca.unit_id,
        SUM(COALESCE(an.capacity_factor, 1) * CASE 
            WHEN dc.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] THEN $familyUnitPlacementCoefficient
            WHEN extract(YEARS FROM age(ca.date, ch.date_of_birth)) < 3 THEN coalesce(sno.occupancy_coefficient_under_3y, default_sno.occupancy_coefficient_under_3y)
            ELSE coalesce(sno.occupancy_coefficient, default_sno.occupancy_coefficient)
        END) AS capacity,
        count(*) as count
    FROM child_attendance ca
    JOIN daycare dc ON dc.id = ca.unit_id
    JOIN person ch ON ch.id = ca.child_id
    LEFT JOIN placement pl on pl.child_id = ca.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> :date
    LEFT JOIN service_need sn on sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') @> :date
    LEFT JOIN service_need_option sno on sn.option_id = sno.id
    LEFT JOIN service_need_option default_sno on pl.type = default_sno.valid_placement_type AND default_sno.default_option
    LEFT JOIN assistance_need an on an.child_id = ca.child_id AND daterange(an.start_date, an.end_date, '[]') @> :date
    WHERE ca.unit_id = ANY(:unitIds) AND ca.end_time IS NULL
    GROUP BY ca.unit_id
), total_children AS (
    SELECT p.unit_id, count(*)
    FROM (
        SELECT p.unit_id
        FROM placement p
        LEFT JOIN backup_care bc ON p.child_id = bc.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> :date
        WHERE p.unit_id = ANY(:unitIds) AND daterange(p.start_date, p.end_date, '[]') @> :date AND bc.id IS NULL

        UNION ALL

        SELECT bc.unit_id
        FROM backup_care bc
        WHERE bc.unit_id = ANY(:unitIds) AND daterange(bc.start_date, bc.end_date, '[]') @> :date
    ) p
    GROUP BY p.unit_id
), present_staff AS (
    SELECT g.daycare_id AS unit_id, sum(sa.capacity) AS capacity, sum(sa.count) AS count
    FROM daycare_group g
    JOIN (
        SELECT sa.group_id, 7 * (sa.count + sa.count_other) as capacity, sa.count + sa.count_other AS count FROM staff_attendance sa
        WHERE NOT :useRealtimeStaffAttendance AND sa.date = :date

        UNION ALL

        SELECT sa.group_id, sa.occupancy_coefficient as capacity, 1 AS count FROM staff_attendance_realtime sa
        WHERE :useRealtimeStaffAttendance AND sa.arrived IS NOT NULL AND sa.departed IS NULL

        UNION ALL

        SELECT sa.group_id, sa.occupancy_coefficient as capacity, 1 AS count FROM staff_attendance_external sa
        WHERE :useRealtimeStaffAttendance AND sa.arrived IS NOT NULL AND sa.departed IS NULL
    ) sa ON sa.group_id = g.id
    WHERE g.daycare_id = ANY(:unitIds) AND daterange(g.start_date, g.end_date, '[]') @> :date
    GROUP BY g.daycare_id
), total_staff AS (
    SELECT g.daycare_id AS unit_id, sum(dc.amount) AS count
    FROM daycare_group g
    JOIN daycare_caretaker dc ON dc.group_id = g.id AND daterange(dc.start_date, dc.end_date, '[]') @> :date
    WHERE g.daycare_id = ANY(:unitIds) AND daterange(g.start_date, g.end_date, '[]') @> :date
    GROUP BY g.daycare_id
)
SELECT
    u.id, u.name,
    coalesce(pc.count, 0) AS present_children,
    coalesce(tc.count, 0) AS total_children,
    coalesce(ps.count, 0) AS present_staff,
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
WHERE u.id = ANY(:unitIds)
"""
    )
        .bind("unitIds", unitIds.toTypedArray())
        .bind("date", date)
        .bind("useRealtimeStaffAttendance", useRealtimeStaffAttendance)
        .mapTo<UnitStats>()
        .toList()
}
