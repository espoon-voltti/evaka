// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class DuplicatePeopleReportController {
    @GetMapping("/reports/duplicate-people")
    fun getDuplicatePeopleReport(db: Database, user: AuthenticatedUser): ResponseEntity<List<DuplicatePeopleReportRow>> {
        Audit.DuplicatePeopleReportRead.log()
        user.requireOneOfRoles(Roles.ADMIN)
        return db.read { it.getDuplicatePeople() }.let(::ok)
    }
}

private fun Database.Read.getDuplicatePeople(): List<DuplicatePeopleReportRow> {
    // language=sql
    val sql =
        """
        WITH people AS (
            SELECT
                p.id,
                p.social_security_number,
                concat(
                    lower(unaccent(last_name)),
                    ',',
                    split_part(lower(unaccent(first_name)), ' ', 1),
                    ',',
                    date_of_birth
                ) AS key
            FROM person p
            WHERE p.first_name IS NOT NULL AND p.last_name IS NOT NULL 
                AND p.first_name <> '' AND p.last_name <> '' AND lower(last_name) <> 'testaaja'
        ), duplicate_keys AS (
            SELECT key
            FROM people p
            GROUP BY key
            HAVING count(id) > 1 AND array_position(array_agg(social_security_number), NULL) IS NOT NULL
        ), duplicate_ids AS (
            SELECT p.key, p.id
            FROM people p
            JOIN duplicate_keys d ON p.key = d.key
        )
        SELECT
            dense_rank() OVER (ORDER BY key) as group_index,
            row_number() OVER (PARTITION BY key ORDER BY social_security_number, p.id) AS duplicate_number,
            p.id,
            p.first_name,
            p.last_name,
            p.social_security_number,
            p.date_of_birth,
            p.street_address,
        
            (SELECT count(*) FROM application WHERE guardian_id = p.id) AS applications_guardian,
            (SELECT count(*) FROM fee_decision WHERE head_of_family = p.id) AS fee_decisions_head,
            (SELECT count(*) FROM fee_decision WHERE partner = p.id) AS fee_decisions_partner,
            (SELECT count(*) FROM fridge_child WHERE head_of_child = p.id) AS fridge_children,
            (SELECT count(*) FROM fridge_partner WHERE person_id = p.id) AS fridge_partners,
            (SELECT count(*) FROM income WHERE person_id = p.id) AS incomes,
            (SELECT count(*) FROM invoice WHERE head_of_family = p.id) AS invoices,
        
            (SELECT count(*) FROM absence WHERE child_id = p.id) AS absences,
            (SELECT count(*) FROM application WHERE child_id = p.id) AS applications_child,
            (SELECT count(*) FROM assistance_need WHERE child_id = p.id) AS assistance_needs,
            (SELECT count(*) FROM assistance_action WHERE child_id = p.id) AS assistance_actions,
            (SELECT count(*) FROM backup_care WHERE child_id = p.id) AS backups,
            (SELECT count(*) FROM fee_alteration WHERE person_id = p.id) AS fee_alterations,
            (SELECT count(*) FROM fee_decision_part WHERE child = p.id) AS fee_decision_parts,
            (SELECT count(*) FROM fridge_child WHERE child_id = p.id) AS fridge_parents,
            (SELECT count(*) FROM invoice_row WHERE child = p.id) AS invoice_rows,
            (SELECT count(*) FROM placement WHERE child_id = p.id) AS placements,
            (SELECT count(*) FROM service_need WHERE child_id = p.id) AS service_needs
        FROM duplicate_ids dups
        JOIN person p ON p.id = dups.id
        ORDER BY key, social_security_number, p.id;
        """.trimIndent()

    return createQuery(sql).map { rs, _ ->
        DuplicatePeopleReportRow(
            groupIndex = rs.getInt("group_index"),
            duplicateNumber = rs.getInt("duplicate_number"),
            id = rs.getUUID("id"),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            socialSecurityNumber = rs.getString("social_security_number"),
            dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
            streetAddress = rs.getString("street_address"),

            applicationsGuardian = rs.getInt("applications_guardian"),
            feeDecisionsHead = rs.getInt("fee_decisions_head"),
            feeDecisionsPartner = rs.getInt("fee_decisions_partner"),
            fridgeChildren = rs.getInt("fridge_children"),
            fridgePartners = rs.getInt("fridge_partners"),
            incomes = rs.getInt("incomes"),
            invoices = rs.getInt("invoices"),

            absences = rs.getInt("absences"),
            applicationsChild = rs.getInt("applications_child"),
            assistanceNeeds = rs.getInt("assistance_needs"),
            assistanceActions = rs.getInt("assistance_actions"),
            backups = rs.getInt("backups"),
            feeAlterations = rs.getInt("fee_alterations"),
            feeDecisionParts = rs.getInt("fee_decision_parts"),
            fridgeParents = rs.getInt("fridge_parents"),
            invoiceRows = rs.getInt("invoice_rows"),
            placements = rs.getInt("placements"),
            serviceNeeds = rs.getInt("service_needs")
        )
    }.toList()
}

data class DuplicatePeopleReportRow(
    val groupIndex: Int,
    val duplicateNumber: Int,
    val id: UUID,
    val firstName: String?,
    val lastName: String?,
    val socialSecurityNumber: String?,
    val dateOfBirth: LocalDate,
    val streetAddress: String?,

    val applicationsGuardian: Int,
    val feeDecisionsHead: Int,
    val feeDecisionsPartner: Int,
    val fridgeChildren: Int,
    val fridgePartners: Int,
    val incomes: Int,
    val invoices: Int,

    val absences: Int,
    val applicationsChild: Int,
    val assistanceNeeds: Int,
    val assistanceActions: Int,
    val backups: Int,
    val feeAlterations: Int,
    val feeDecisionParts: Int,
    val fridgeParents: Int,
    val invoiceRows: Int,
    val placements: Int,
    val serviceNeeds: Int
)
