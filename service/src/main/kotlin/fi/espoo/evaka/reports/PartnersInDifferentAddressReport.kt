// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PartnersInDifferentAddressReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/partners-in-different-address")
    fun getPartnersInDifferentAddressReport(db: Database, user: AuthenticatedUser): ResponseEntity<List<PartnersInDifferentAddressReportRow>> {
        Audit.PartnersInDifferentAddressReportRead.log()
        user.requireOneOfRoles(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR)
        return db.read { it.getPartnersInDifferentAddressRows(acl.getAuthorizedUnits(user)) }
            .let { ResponseEntity.ok(it) }
    }
}

private fun Database.Read.getPartnersInDifferentAddressRows(authorizedUnits: AclAuthorization): List<PartnersInDifferentAddressReportRow> {
    // language=sql
    val sql =
        """
        SELECT
            ca.name AS care_area_name,
            u.id AS unit_id,
            u.name AS unit_name,
            p1.id AS person_id_1,
            p1.first_name AS first_name_1,
            p1.last_name AS last_name_1,
            p1.street_address AS street_address_1,
            p2.id AS person_id_2,
            p2.first_name AS first_name_2,
            p2.last_name AS last_name_2,
            p2.street_address AS street_address_2
        FROM fridge_partner fp1
        JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.person_id < fp2.person_id
        JOIN person p1 ON p1.id = fp1.person_id
        JOIN person p2 ON p2.id = fp2.person_id
        LEFT JOIN primary_units_view pu1 ON pu1.head_of_child = p1.id
        LEFT JOIN primary_units_view pu2 ON pu2.head_of_child = p2.id
        JOIN daycare u ON u.id = coalesce(pu1.unit_id, pu2.unit_id)
        JOIN care_area ca ON ca.id = u.care_area_id
        WHERE
            (:units::uuid[] IS NULL OR u.id = ANY(:units)) AND
            daterange(fp1.start_date, fp1.end_date, '[]') @> current_date AND
            fp1.conflict = false AND 
            p1.residence_code <> p2.residence_code AND
            p1.residence_code IS NOT NULL AND
            p1.residence_code <> '' AND
            p1.street_address IS NOT NULL AND
            p1.street_address <> '' AND
            lower(p1.street_address) <> 'poste restante' AND
            p2.residence_code IS NOT NULL AND
            p2.residence_code <> '' AND
            p2.street_address IS NOT NULL AND
            p2.street_address <> '' AND
            lower(p2.street_address) <> 'poste restante'
        ORDER BY u.name, p1.last_name, p1.first_name, p2.last_name, p2.first_name;
        """.trimIndent()
    return createQuery(sql)
        .bindNullable("units", authorizedUnits.ids?.toTypedArray())
        .map { rs, _ ->
            PartnersInDifferentAddressReportRow(
                careAreaName = rs.getString("care_area_name"),
                unitId = rs.getUUID("unit_id"),
                unitName = rs.getString("unit_name"),
                personId1 = rs.getUUID("person_id_1"),
                firstName1 = rs.getString("first_name_1"),
                lastName1 = rs.getString("last_name_1"),
                address1 = rs.getString("street_address_1"),
                personId2 = rs.getUUID("person_id_2"),
                firstName2 = rs.getString("first_name_2"),
                lastName2 = rs.getString("last_name_2"),
                address2 = rs.getString("street_address_2")
            )
        }
        .toList()
}

data class PartnersInDifferentAddressReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val personId1: UUID,
    val firstName1: String?,
    val lastName1: String?,
    val address1: String,
    val personId2: UUID,
    val firstName2: String?,
    val lastName2: String?,
    val address2: String
)
