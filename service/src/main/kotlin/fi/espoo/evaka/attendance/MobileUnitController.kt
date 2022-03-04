// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
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

@RestController
@RequestMapping("/mobile/units")
class MobileUnitController(private val accessControl: AccessControl) {
    @GetMapping("/{unitId}")
    fun getUnitInfo(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): UnitInfo {
        Audit.UnitRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_MOBILE_INFO, unitId)
        return db.connect { dbc -> dbc.read { tx -> tx.fetchUnitInfo(unitId, evakaClock.today()) } }
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
        return db.connect { dbc -> dbc.read { tx -> tx.fetchUnitStats(unitIds, evakaClock.today(), useRealtimeStaffAttendance) } }
    }
}

data class UnitInfo(
    val id: DaycareId,
    val name: String,
    val groups: List<GroupInfo>,
    val staff: List<Staff>,
    val features: List<PilotFeature>
)

data class GroupInfo(
    val id: GroupId,
    val name: String
)

data class Staff(
    val id: EmployeeId,
    val firstName: String,
    val lastName: String,
    val pinSet: Boolean,
    val pinLocked: Boolean,
    val groups: List<GroupId>
)

fun Database.Read.fetchUnitInfo(unitId: DaycareId, date: LocalDate): UnitInfo {
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
        SELECT g.id, g.name
        FROM daycare u
        JOIN daycare_group g on u.id = g.daycare_id AND daterange(g.start_date, g.end_date, '[]') @> :date
        WHERE u.id = :unitId
        """.trimIndent()

    val groups = createQuery(groupsSql)
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<GroupInfo>()
        .list()

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
        features = unit.features
    )
}

data class UnitStats(
    val id: DaycareId,
    val name: String,
    val presentChildren: Int,
    val totalChildren: Int,
    val presentStaff: Int,
    val totalStaff: Int
)

fun Database.Read.fetchUnitStats(
    unitIds: List<DaycareId>,
    date: LocalDate,
    useRealtimeStaffAttendance: Boolean
): List<UnitStats> {
    return createQuery(
        """
WITH present_children AS (
    SELECT ca.unit_id, count(*)
    FROM child_attendance ca
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
    SELECT g.daycare_id AS unit_id, sum(sa.count) AS count
    FROM daycare_group g
    JOIN (
        SELECT sa.group_id, sa.count + sa.count_other AS count FROM staff_attendance sa
        WHERE NOT :useRealtimeStaffAttendance AND sa.date = :date

        UNION ALL

        SELECT sa.group_id, 1 AS count FROM staff_attendance_realtime sa
        WHERE :useRealtimeStaffAttendance AND sa.arrived IS NOT NULL AND sa.departed IS NULL

        UNION ALL

        SELECT sa.group_id, 1 AS count FROM staff_attendance_external sa
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
    coalesce(ts.count, 0) AS total_staff
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
