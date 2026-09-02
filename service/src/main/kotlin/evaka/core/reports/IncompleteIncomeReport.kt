// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IncompleteIncomeReport(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/incomplete-income")
    fun getIncompleteIncomeReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<IncompleteIncomeDbRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_INCOMPLETE_INCOMES_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getIncompleteReport(clock.today())
                }
            }
            .also { Audit.IncompleteIncomeReportRead.log() }
    }
}

fun Database.Read.getIncompleteReport(today: LocalDate): List<IncompleteIncomeDbRow> {
    val dbRows = createQuery {
        sql(
            """
                SELECT DISTINCT pe.id as personId, pe.first_name as firstName, pe.last_name as lastName, ie.valid_from as validFrom, dg.name as daycareName, ca.name as careareaName
                FROM placement pl
                LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
                JOIN daycare dg ON dg.id = pl.unit_id
                JOIN care_area ca ON ca.id = dg.care_area_id
                JOIN fridge_child fc_head ON fc_head.child_id = pl.child_id
                    AND daterange(fc_head.start_date, fc_head.end_date, '[]') @> ${bind(today)}
                    AND fc_head.conflict = false
                LEFT JOIN fridge_partner fp ON fp.person_id = fc_head.head_of_child
                    AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)}
                    AND fp.conflict = false
                LEFT JOIN fridge_partner fp_partner ON fp_partner.partnership_id = fp.partnership_id
                    AND fp_partner.person_id <> fp.person_id
                    AND daterange(fp_partner.start_date, fp_partner.end_date, '[]') @> ${bind(today)}
                    AND fp_partner.conflict = false
                -- one row per adult (head and possible partner), each paired with the other adult
                -- for the max-fee check below. With no partner the NULL row is dropped by the person join
                CROSS JOIN LATERAL (VALUES (fc_head.head_of_child, fp_partner.person_id), (fp_partner.person_id, fc_head.head_of_child)) AS adult(id, other_id)
                JOIN person pe ON pe.id = adult.id
                JOIN income ie ON ie.person_id = pe.id
                WHERE ie.effect = 'INCOMPLETE'
                AND ie.valid_to IS NULL
                AND ie.modified_by = ${bind(AuthenticatedUser.SystemInternalUser.evakaUserId)}
                -- placement is paid if its current service need has a fee,
                -- or it has no current service need and the placement type's default option is paid
                AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}
                AND COALESCE(
                    (
                        SELECT bool_or(sno.fee_coefficient > 0)
                        FROM service_need sn
                        JOIN service_need_option sno ON sno.id = sn.option_id
                        WHERE sn.placement_id = pl.id
                        AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(today)}
                    ),
                    default_sno.fee_coefficient > 0
                )
                AND (dg.invoiced_by_municipality OR dg.provider_type = 'PRIVATE_SERVICE_VOUCHER')
                AND NOT EXISTS (
                    SELECT FROM income partner_income
                    WHERE partner_income.person_id = adult.other_id
                    AND partner_income.effect = 'MAX_FEE_ACCEPTED'
                    AND daterange(partner_income.valid_from, partner_income.valid_to, '[]') @> ${bind(today)}
                )
                ORDER BY ie.valid_from;
            """
                .trimIndent()
        )
    }
        .toList<IncompleteIncomeDbRow>()

    return dbRows
}

data class IncompleteIncomeDbRow(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val validFrom: LocalDate,
    val daycareName: String,
    val careareaName: String,
)
