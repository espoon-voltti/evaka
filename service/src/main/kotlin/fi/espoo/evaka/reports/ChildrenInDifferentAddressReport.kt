// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildrenInDifferentAddressReportController(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/children-in-different-address", // deprecated
        "/employee/reports/children-in-different-address",
    )
    fun getChildrenInDifferentAddressReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<ChildrenInDifferentAddressReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Unit.READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT,
                        )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getChildrenInDifferentAddressRows(clock, filter)
                }
            }
            .also {
                Audit.ChildrenInDifferentAddressReportRead.log(meta = mapOf("count" to it.size))
            }
    }
}

private fun Database.Read.getChildrenInDifferentAddressRows(
    clock: EvakaClock,
    unitFilter: AccessControlFilter<DaycareId>,
): List<ChildrenInDifferentAddressReportRow> =
    createQuery {
            sql(
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
          AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(clock.today())}
          AND pl.type != 'CLUB'::placement_type
        JOIN daycare u ON u.id = pl.unit_id
        JOIN care_area ca ON ca.id = u.care_area_id
        WHERE
            ${predicate(unitFilter.forTable("u"))} AND
            daterange(fc.start_date, fc.end_date, '[]') @> ${bind(clock.today())} AND
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
        """
                    .trimIndent()
            )
        }
        .toList<ChildrenInDifferentAddressReportRow>()

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
    val addressChild: String,
)
