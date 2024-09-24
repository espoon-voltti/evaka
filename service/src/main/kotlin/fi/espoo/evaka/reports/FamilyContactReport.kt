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
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FamilyContactReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/family-contacts")
    fun getFamilyContactsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam date: LocalDate,
    ): List<FamilyContactReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Unit.READ_FAMILY_CONTACT_REPORT,
                        unitId,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getFamilyContacts(date, unitId)
                }
            }
            .also { Audit.FamilyContactReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getFamilyContacts(
    date: LocalDate,
    unitId: DaycareId,
): List<FamilyContactReportRow> {
    return createQuery {
            sql(
                """
WITH all_placements AS (
    SELECT pl.child_id, dgp.daycare_group_id AS group_id
    FROM placement pl
    LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(date)}
    WHERE pl.unit_id = ${bind(unitId)} AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
    
    UNION DISTINCT 
    
    SELECT bc.child_id, bc.group_id
    FROM backup_care bc
    WHERE bc.unit_id = ${bind(unitId)} AND daterange(bc.start_date, bc.end_date, '[]') @> ${bind(date)}
)
SELECT
    ch.id,
    ch.first_name,
    ch.last_name,
    ch.social_security_number AS ssn,
    ch.street_address,
    ch.postal_code,
    ch.post_office,
    dg.name AS group_name,

    hoc.id AS hoc_id,
    hoc.first_name AS hoc_first_name,
    hoc.last_name AS hoc_last_name,
    hoc.phone AS hoc_phone,
    hoc.email AS hoc_email,

    gu1.id AS gu1_id,
    gu1.first_name AS gu1_first_name,
    gu1.last_name AS gu1_last_name,
    gu1.phone AS gu1_phone,
    gu1.email AS gu1_email,

    gu2.id AS gu2_id,
    gu2.first_name AS gu2_first_name,
    gu2.last_name AS gu2_last_name,
    gu2.phone AS gu2_phone,
    gu2.email AS gu2_email
FROM all_placements pl
JOIN person ch ON ch.id = pl.child_id
LEFT JOIN daycare_group dg ON dg.id = pl.group_id
LEFT JOIN fridge_child fc ON ch.id = fc.child_id AND daterange(fc.start_date, fc.end_date, '[]') @> ${bind(date)}
LEFT JOIN person hoc ON hoc.id = fc.head_of_child
LEFT JOIN guardian g1 ON ch.id = g1.child_id AND g1.guardian_id = (SELECT min(guardian_id::text)::uuid FROM guardian where child_id = ch.id)
LEFT JOIN guardian g2 ON ch.id = g2.child_id AND g2.guardian_id = (SELECT max(guardian_id::text)::uuid FROM guardian where child_id = ch.id) AND g2.guardian_id != (SELECT min(guardian_id::text)::uuid FROM guardian where child_id = ch.id)
LEFT JOIN person gu1 ON gu1.id = g1.guardian_id
LEFT JOIN person gu2 ON gu2.id = g2.guardian_id
ORDER BY dg.name, ch.last_name, ch.first_name
"""
            )
        }
        .toList<FamilyContactReportRow>()
}

data class FamilyContactReportRow(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val ssn: String?,
    val groupName: String?,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    @Nested("hoc_") val headOfChild: Contact?,
    @Nested("gu1_") val guardian1: Contact?,
    @Nested("gu2_") val guardian2: Contact?,
)

data class Contact(
    @PropagateNull val id: PersonId,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String?,
)
