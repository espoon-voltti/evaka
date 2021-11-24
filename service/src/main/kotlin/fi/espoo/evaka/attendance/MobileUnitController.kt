// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
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
class MobileUnitController(
    private val acl: AccessControlList
) {
    @GetMapping("/{unitId}")
    fun getUnitInfo(
        db: Database.Connection,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @PathVariable unitId: DaycareId
    ): UnitInfo {
        Audit.UnitRead.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE)
        return db.read { tx -> tx.fetchUnitInfo(unitId, evakaClock.today()) }
    }

    @GetMapping("/stats")
    fun getUnitStats(
        db: Database.Connection,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestParam unitIds: List<DaycareId>
    ): List<UnitStats> {
        Audit.UnitRead.log(targetId = unitIds)
        unitIds.forEach { unitId -> acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.MOBILE) }
        return db.read { tx -> tx.fetchUnitStats(unitIds, evakaClock.today()) }
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
        WHERE acl.daycare_id = :id AND acl.role = ANY('{STAFF, UNIT_SUPERVISOR}')
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
    val totalChildren: Int
)

fun Database.Read.fetchUnitStats(unitIds: List<DaycareId>, date: LocalDate): List<UnitStats> {
    return createQuery(
        """
SELECT
    u.id, u.name,
    sum(1) FILTER (WHERE ca.id IS NOT NULL) AS present_children,
    sum(1) FILTER (WHERE p.child_id IS NOT NULL) AS total_children
FROM daycare u
LEFT JOIN LATERAL (
    SELECT p.child_id
    FROM placement p
    LEFT JOIN backup_care bc ON p.child_id = bc.child_id
        AND daterange(bc.start_date, bc.end_date, '[]') @> :date
    WHERE p.unit_id = u.id AND daterange(p.start_date, p.end_date, '[]') @> :date AND bc.id IS NULL

    UNION ALL

    SELECT bc.child_id
    FROM backup_care bc
    WHERE bc.unit_id = u.id AND daterange(bc.start_date, bc.end_date, '[]') @> :date

) p ON true
LEFT JOIN child_attendance ca ON ca.unit_id = u.id AND ca.child_id = p.child_id
    AND ca.arrived IS NOT NULL AND ca.departed IS NULL
WHERE u.id = ANY(:unitIds)
GROUP BY u.id, u.name
"""
    )
        .bind("unitIds", unitIds.toTypedArray())
        .bind("date", date)
        .mapTo<UnitStats>()
        .toList()
}
