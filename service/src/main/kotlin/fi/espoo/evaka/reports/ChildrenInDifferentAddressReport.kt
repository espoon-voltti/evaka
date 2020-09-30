// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.handle
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChildrenInDifferentAddressReportController(
    private val jdbi: Jdbi,
    private val acl: AccessControlList
) {
    @GetMapping("/reports/children-in-different-address")
    fun getChildrenInDifferentAddressReport(user: AuthenticatedUser): ResponseEntity<List<ChildrenInDifferentAddressReportRow>> {
        Audit.ChildrenInDifferentAddressReportRead.log()
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR)
        val authorizedUnits = acl.getAuthorizedUnits(user)
        val rows = jdbi.handle { getChildrenInDifferentAddressRows(it, authorizedUnits) }
        return ResponseEntity.ok(rows)
    }
}

fun getChildrenInDifferentAddressRows(
    h: Handle,
    authorizedUnits: AclAuthorization
): List<ChildrenInDifferentAddressReportRow> {
    val units = authorizedUnits.ids
    // language=sql
    val sql =
        """
        SELECT
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name AS unit_name,
            p.id AS person_id_parent,
            p.first_name AS first_name_parent,
            p.last_name AS last_name_parent,
            p.street_address AS street_address_parent,
            ch.id AS person_id_child,
            ch.first_name AS first_name_child,
            ch.last_name AS last_name_child,
            ch.street_address AS street_address_child
        FROM fridge_child fc
        JOIN person p ON p.id = fc.head_of_child
        JOIN person ch ON ch.id = fc.child_id
        JOIN placement pl ON pl.child_id = fc.child_id
          AND daterange(pl.start_date, pl.end_date, '[]') @> current_date
          AND pl.type != 'CLUB'::placement_type
        JOIN daycare u ON u.id = pl.unit_id
        JOIN care_area ca ON ca.id = u.care_area_id
        WHERE
            (:units::uuid[] IS NULL OR u.id = ANY(:units)) AND
            daterange(fc.start_date, fc.end_date, '[]') @> current_date AND
            fc.conflict = false AND
            p.residence_code <> ch.residence_code AND
            p.residence_code IS NOT NULL AND
            p.residence_code <> '' AND
            p.street_address IS NOT NULL AND
            p.street_address <> '' AND
            lower(p.street_address) <> 'poste restante' AND
            ch.residence_code IS NOT NULL AND
            ch.residence_code <> '' AND
            ch.street_address IS NOT NULL AND
            ch.street_address <> '' AND
            lower(ch.street_address) <> 'poste restante'
        ORDER BY u.name, p.last_name, p.first_name, ch.last_name, ch.first_name;
        """.trimIndent()
    return h.createQuery(sql)
        .bindNullable("units", units?.toTypedArray())
        .map { rs, _ ->
            ChildrenInDifferentAddressReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                parentId = rs.getUUID("person_id_parent"),
                firstNameParent = rs.getString("first_name_parent"),
                lastNameParent = rs.getString("last_name_parent"),
                addressParent = rs.getString("street_address_parent"),
                childId = rs.getUUID("person_id_child"),
                firstNameChild = rs.getString("first_name_child"),
                lastNameChild = rs.getString("last_name_child"),
                addressChild = rs.getString("street_address_child")
            )
        }
        .toList()
}

data class ChildrenInDifferentAddressReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val parentId: UUID,
    val firstNameParent: String?,
    val lastNameParent: String?,
    val addressParent: String,
    val childId: UUID,
    val firstNameChild: String?,
    val lastNameChild: String?,
    val addressChild: String
)
