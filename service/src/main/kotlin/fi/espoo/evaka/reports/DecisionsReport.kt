// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class DecisionsReportController {
    @GetMapping("/reports/decisions")
    fun getDecisionsReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<List<DecisionsReportRow>> {
        Audit.DecisionsReportRead.log()
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.DIRECTOR, UserRole.ADMIN)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")

        return db.read { it.getDecisionsRows(from, to) }.let(::ok)
    }
}

private fun Database.Read.getDecisionsRows(from: LocalDate, to: LocalDate): List<DecisionsReportRow> {
    // language=sql
    val sql =
        """
        WITH data AS (
            SELECT 
                ca.name AS care_area_name,
                u.id AS unit_id,
                u.name AS unit_name,
                u.provider_type,
                de.id decision_id,
                de.type AS decision_type,
                (
                    SELECT array_agg(e::UUID)
                    FROM jsonb_array_elements_text(af.document -> 'apply' -> 'preferredUnits') e
                ) AS preferred_units,
                (af.document ->> 'connectedDaycare') :: BOOLEAN AS connected_daycare,
                a.child_id,
                date_part('year', age(de.sent_date, ch.date_of_birth)) AS age
            FROM care_area ca
            JOIN daycare u ON u.care_area_id = ca.id
            LEFT JOIN decision de ON de.unit_id = u.id AND de.sent_date IS NOT NULL AND daterange(:from, :to, '[]') @> de.sent_date 
            LEFT JOIN application a ON a.id = de.application_id
            LEFT JOIN application_form af ON af.application_id = a.id AND af.latest IS TRUE
            LEFT JOIN person ch ON ch.id = a.child_id
        )
        SELECT
            care_area_name,
            unit_id,
            unit_name,
            provider_type,
            count(DISTINCT decision_id) FILTER ( WHERE age < 3 AND decision_type IN ('DAYCARE', 'DAYCARE_PART_TIME') ) AS daycare_under_3,
            count(DISTINCT decision_id) FILTER ( WHERE age >= 3 AND decision_type IN ('DAYCARE', 'DAYCARE_PART_TIME') ) AS daycare_over_3,
            count(DISTINCT decision_id) FILTER ( WHERE decision_type = 'PRESCHOOL' ) AS preschool,
            count(DISTINCT decision_id) FILTER ( WHERE decision_type = 'PRESCHOOL_DAYCARE' ) AS preschool_daycare,
            count(DISTINCT decision_id) FILTER ( WHERE decision_type = 'PREPARATORY_EDUCATION' AND connected_daycare IS FALSE ) AS preparatory,
            count(DISTINCT decision_id) FILTER ( WHERE decision_type = 'PREPARATORY_EDUCATION' AND connected_daycare IS TRUE ) AS preparatory_daycare,
            count(DISTINCT decision_id) FILTER ( WHERE decision_type = 'CLUB' ) AS club,
            count(DISTINCT decision_id) FILTER ( WHERE unit_id = preferred_units[1] ) AS preference_1,
            count(DISTINCT decision_id) FILTER ( WHERE unit_id = preferred_units[2] ) AS preference_2,
            count(DISTINCT decision_id) FILTER ( WHERE unit_id = preferred_units[3] ) AS preference_3,
            count(DISTINCT decision_id) FILTER ( WHERE unit_id != ANY(preferred_units) ) AS preference_none,
            count(DISTINCT decision_id) total
        FROM data
        GROUP BY care_area_name, unit_id, unit_name, provider_type
        ORDER BY care_area_name, unit_name
        """.trimIndent()
    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .mapTo<DecisionsReportRow>()
        .toList()
}

data class DecisionsReportRow(
    val careAreaName: String,
    val unitId: UUID,
    val unitName: String,
    val providerType: String,
    val daycareUnder3: Int,
    val daycareOver3: Int,
    val preschool: Int,
    val preschoolDaycare: Int,
    val preparatory: Int,
    val preparatoryDaycare: Int,
    val club: Int,
    val preference1: Int,
    val preference2: Int,
    val preference3: Int,
    val preferenceNone: Int,
    val total: Int
)
