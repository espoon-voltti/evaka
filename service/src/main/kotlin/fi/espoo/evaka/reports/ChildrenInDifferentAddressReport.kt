// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildrenInDifferentAddressReportController(
    private val acl: AccessControlList,
    private val accessControl: AccessControl
) {
    @GetMapping("/reports/children-in-different-address")
    fun getChildrenInDifferentAddressReport(
        db: Database,
        user: AuthenticatedUser
    ): List<ChildrenInDifferentAddressReportRow> {
        Audit.ChildrenInDifferentAddressReportRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT)
        return db.connect { dbc ->
            dbc.read {
                it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                it.getChildrenInDifferentAddressRows(acl.getAuthorizedUnits(user))
            }
        }
    }
}

private fun Database.Read.getChildrenInDifferentAddressRows(authorizedUnits: AclAuthorization): List<ChildrenInDifferentAddressReportRow> {
    val units = authorizedUnits.ids
    // language=sql
    val sql =
        """
        SELECT
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name AS unit_name,
            p.id AS parent_id,
            p.first_name AS first_name_parent,
            p.last_name AS last_name_parent,
            p.street_address AS address_parent,
            ch.id AS child_id,
            ch.first_name AS first_name_child,
            ch.last_name AS last_name_child,
            ch.street_address AS address_child
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
    return createQuery(sql)
        .bindNullable("units", units?.toTypedArray())
        .mapTo<ChildrenInDifferentAddressReportRow>()
        .toList()
}

data class ChildrenInDifferentAddressReportRow(
    val careAreaName: String,
    val unitId: DaycareId,
    val unitName: String,
    val parentId: PersonId,
    val firstNameParent: String?,
    val lastNameParent: String?,
    val addressParent: String,
    val childId: ChildId,
    val firstNameChild: String?,
    val lastNameChild: String?,
    val addressChild: String
)
