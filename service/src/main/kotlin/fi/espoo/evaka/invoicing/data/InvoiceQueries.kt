// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.InvoiceDistinctiveParams
import fi.espoo.evaka.invoicing.controller.InvoiceSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.DraftInvoice
import fi.espoo.evaka.invoicing.domain.DraftInvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceReplacementReason
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.db.personFreeTextSearchPredicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import java.time.LocalDate
import java.time.YearMonth

fun invoiceDetailedQuery(where: Predicate) = QuerySql {
    sql(
        """
    SELECT
        invoice.id,
        invoice.status,
        invoice.period_start,
        invoice.period_end,
        invoice.due_date,
        invoice.invoice_date,
        care_area.area_code AS agreement_type,
        invoice.area_id,
        invoice.number,
        invoice.sent_at,
        invoice.revision_number,
        invoice.replaced_invoice_id,
        invoice.replacement_reason,
        invoice.replacement_notes,
        
        invoice.sent_by AS sent_by_id,
        sent_by.name AS sent_by_name,
        sent_by.type AS sent_by_type,
    
        head.id as head_id,
        head.date_of_birth as head_date_of_birth,
        head.first_name as head_first_name,
        head.last_name as head_last_name,
        head.social_security_number as head_ssn,
        head.street_address as head_street_address,
        head.postal_code as head_postal_code,
        head.post_office as head_post_office,
        head.phone as head_phone,
        head.email as head_email,
        head.language as head_language,
        head.invoice_recipient_name as head_invoice_recipient_name,
        head.invoicing_street_address as head_invoicing_street_address,
        head.invoicing_postal_code as head_invoicing_postal_code,
        head.invoicing_post_office as head_invoicing_post_office,
        head.restricted_details_enabled as head_restricted_details_enabled,
        
        codebtor.id as codebtor_id,
        codebtor.date_of_birth as codebtor_date_of_birth,
        codebtor.first_name as codebtor_first_name,
        codebtor.last_name as codebtor_last_name,
        codebtor.social_security_number as codebtor_ssn,
        codebtor.street_address as codebtor_street_address,
        codebtor.postal_code as codebtor_postal_code,
        codebtor.post_office as codebtor_post_office,
        codebtor.phone as codebtor_phone,
        codebtor.email as codebtor_email,
        codebtor.language as codebtor_language,
        codebtor.restricted_details_enabled as codebtor_restricted_details_enabled,
        
        coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'id', row.id,
                'child', jsonb_build_object(
                    'id', child.id,
                    'dateOfBirth', child.date_of_birth,
                    'firstName', child.first_name,
                    'lastName', child.last_name,
                    'ssn', child.social_security_number,
                    'streetAddress', child.street_address,
                    'postalCode', child.postal_code,
                    'postOffice', child.post_office,
                    'restrictedDetailsEnabled', child.restricted_details_enabled
                ),
                'amount', row.amount,
                'unitPrice', row.unit_price,
                'periodStart', row.period_start,
                'periodEnd', row.period_end,
                'product', row.product,
                'unitId', row.unit_id,
                'unitName', daycare.name,
                'unitProviderType', daycare.provider_type,
                'daycareType', daycare.type,
                'costCenter', daycare.cost_center,
                'subCostCenter', row_care_area.sub_cost_center,
                'savedCostCenter', row.saved_cost_center,
                'description', row.description,
                'correctionId', row.correction_id,
                'note', correction.note
            ) ORDER BY row.idx)
            FROM invoice_row as row 
            LEFT JOIN invoice_correction as correction ON row.correction_id = correction.id
            LEFT JOIN daycare ON row.unit_id = daycare.id
            LEFT JOIN care_area AS row_care_area ON daycare.care_area_id = row_care_area.id
            LEFT JOIN person as child ON row.child = child.id
            WHERE invoice.id = row.invoice_id
        ), '[]'::jsonb) as rows,
        
        coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'id', fd.id,
                'decisionNumber', fd.decision_number
            ) ORDER BY fd.decision_number)
            FROM invoiced_fee_decision
            JOIN fee_decision fd ON invoiced_fee_decision.fee_decision_id = fd.id
            WHERE invoiced_fee_decision.invoice_id = invoice.id AND fd.decision_number IS NOT NULL
        ), '[]'::jsonb) as related_fee_decisions,

        coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'id', id,
                'name', name,
                'contentType', content_type
            ) ORDER BY a.created)
            FROM attachment a
            WHERE a.invoice_id = invoice.id
        ), '[]'::jsonb) as attachments

    FROM invoice
    JOIN care_area ON invoice.area_id = care_area.id
    LEFT JOIN person as head ON invoice.head_of_family = head.id
    LEFT JOIN person as codebtor ON invoice.codebtor = codebtor.id
    LEFT JOIN evaka_user as sent_by ON invoice.sent_by = sent_by.id
    WHERE ${predicate(where.forTable("invoice"))}
    """
    )
}

fun Database.Read.getInvoice(id: InvoiceId): InvoiceDetailed? =
    getInvoicesByIds(listOf(id)).firstOrNull()

fun Database.Read.getInvoicesByIds(ids: List<InvoiceId>): List<InvoiceDetailed> {
    if (ids.isEmpty()) return listOf()

    return createQuery { invoiceDetailedQuery(Predicate { where("$it.id = ANY(${bind(ids)})") }) }
        .toList()
}

fun Database.Read.getHeadOfFamilyInvoices(headOfFamilyId: PersonId): List<InvoiceDetailed> {
    return createQuery {
            sql(
                """
${subquery(invoiceDetailedQuery(Predicate { where("$it.head_of_family = ${bind(headOfFamilyId)}") }))}
ORDER BY invoice.period_start DESC, invoice.revision_number DESC NULLS LAST
"""
            )
        }
        .toList()
}

fun Database.Read.getDetailedInvoice(id: InvoiceId): InvoiceDetailed? {
    return createQuery { invoiceDetailedQuery(Predicate { where("invoice.id = ${bind(id)}") }) }
        .exactlyOneOrNull()
}

fun Database.Read.getReplacingInvoiceFor(id: InvoiceId): InvoiceDetailed? {
    return createQuery {
            invoiceDetailedQuery(Predicate { where("invoice.replaced_invoice_id = ${bind(id)}") })
        }
        .exactlyOneOrNull()
}

fun Database.Read.getSentInvoicesOfMonth(month: YearMonth): List<InvoiceDetailed> {
    val periodStart = month.atDay(1)
    val periodEnd = month.atEndOfMonth()
    val statuses = listOf(InvoiceStatus.SENT, InvoiceStatus.WAITING_FOR_SENDING)
    return createQuery {
            invoiceDetailedQuery(
                Predicate {
                    where(
                        """
                            $it.period_start = ${bind(periodStart)} AND
                            $it.period_end = ${bind(periodEnd)} AND
                            $it.status = ANY (${bind(statuses)})
                            """
                    )
                }
            )
        }
        .toList()
}

fun Database.Read.getSentInvoiceOfMonth(
    month: YearMonth,
    headOfFamilyId: PersonId,
): InvoiceDetailed? {
    val periodStart = month.atDay(1)
    val periodEnd = month.atEndOfMonth()
    val statuses = listOf(InvoiceStatus.SENT, InvoiceStatus.WAITING_FOR_SENDING)
    return createQuery {
            invoiceDetailedQuery(
                Predicate {
                    where(
                        """
                        $it.head_of_family = ${bind(headOfFamilyId)} AND
                        $it.period_start = ${bind(periodStart)} AND
                        $it.period_end = ${bind(periodEnd)} AND
                        $it.status = ANY (${bind(statuses)})
                        """
                    )
                }
            )
        }
        .exactlyOneOrNull()
}

fun Database.Read.getInvoiceIdsByDates(
    range: FiniteDateRange,
    areas: List<String>,
): List<InvoiceId> {
    return createQuery {
            sql(
                """
                SELECT id FROM invoice
                WHERE between_start_and_end(${bind(range)}, invoice_date)
                AND area_id IN (SELECT id FROM care_area WHERE short_name = ANY(${bind(areas)}))
                AND status = ${bind(InvoiceStatus.DRAFT)}::invoice_status
                """
            )
        }
        .toList<InvoiceId>()
}

data class PagedInvoiceSummaries(val data: List<InvoiceSummary>, val total: Int, val pages: Int)

fun Database.Read.paginatedSearch(
    page: Int = 1,
    pageSize: Int = 200,
    sortBy: InvoiceSortParam = InvoiceSortParam.STATUS,
    sortDirection: SortDirection = SortDirection.ASC,
    status: InvoiceStatus = InvoiceStatus.DRAFT,
    areas: List<String> = listOf(),
    unit: DaycareId? = null,
    distinctiveParams: List<InvoiceDistinctiveParams> = listOf(),
    searchTerms: String = "",
    periodStart: LocalDate? = null,
    periodEnd: LocalDate? = null,
): PagedInvoiceSummaries {
    val sortColumn =
        when (sortBy) {
            InvoiceSortParam.HEAD_OF_FAMILY -> Pair("max(head.last_name)", "head.last_name")
            InvoiceSortParam.CHILDREN -> Pair("max(child.last_name)", "child.last_name")
            InvoiceSortParam.START -> Pair("max(invoice.period_start)", "invoice.period_start")
            InvoiceSortParam.END -> Pair("max(invoice.period_end)", "invoice.period_end")
            InvoiceSortParam.SUM -> Pair("sum", "invoice_ids.sum")
            InvoiceSortParam.STATUS -> Pair("max(invoice.status)", "invoice.status")
            InvoiceSortParam.CREATED_AT -> Pair("max(invoice.created_at)", "invoice.created_at")
        }

    val withMissingAddress = distinctiveParams.contains(InvoiceDistinctiveParams.MISSING_ADDRESS)

    val conditions =
        PredicateSql.allNotNull(
            PredicateSql { where("invoice.status = ${bind(status)}") },
            if (areas.isNotEmpty())
                PredicateSql {
                    where(
                        "invoice.area_id IN (SELECT id FROM care_area WHERE short_name = ANY(${bind(areas)}))"
                    )
                }
            else null,
            if (unit != null) PredicateSql { where("row.unit_id = ${bind(unit)}") } else null,
            if (withMissingAddress)
                PredicateSql {
                    where(
                        "COALESCE(NULLIF(head.invoicing_street_address, ''), NULLIF(head.street_address, '')) IS NULL"
                    )
                }
            else null,
            if (searchTerms.isNotBlank())
                personFreeTextSearchPredicate(listOf("head", "child"), searchTerms)
            else null,
            if (periodStart != null) PredicateSql { where("invoice_date  >= ${bind(periodStart)}") }
            else null,
            if (periodEnd != null) PredicateSql { where("invoice_date  <= ${bind(periodEnd)}") }
            else null,
        )

    return createQuery {
            sql(
                """
WITH invoice_ids AS (
    SELECT invoice.id, sum(amount * unit_price), count(*) OVER ()
    FROM invoice
    LEFT JOIN invoice_row AS row ON invoice.id = row.invoice_id
    LEFT JOIN person AS head ON invoice.head_of_family = head.id
    LEFT JOIN person AS child ON row.child = child.id
    WHERE ${predicate(conditions)}
    GROUP BY invoice.id
    ORDER BY ${sortColumn.first} ${sortDirection.name}, invoice.id
    LIMIT ${bind(pageSize)} OFFSET (${bind(page)} - 1) * ${bind(pageSize)}
)
SELECT
    invoice_ids.count,
    invoice.id,
    invoice.status,
    invoice.period_start,
    invoice.period_end,
    invoice.sent_at,
    invoice.sent_by,
    invoice.created_at,
    invoice.revision_number,
    jsonb_build_object(
        'id', invoice.head_of_family,
        'dateOfBirth', head.date_of_birth,
        'firstName', head.first_name,
        'lastName', head.last_name,
        'ssn', head.social_security_number,
        'streetAddress', head.street_address,
        'postalCode', head.postal_code,
        'postOffice', head.post_office,
        'restrictedDetailsEnabled', head.restricted_details_enabled
    ) as head_of_family,
    rows_summary.children,
    rows_summary.total_price
FROM invoice_ids
JOIN invoice ON invoice_ids.id = invoice.id
JOIN person as head ON invoice.head_of_family = head.id
JOIN LATERAL (
    SELECT
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', row.child,
            'dateOfBirth', child.date_of_birth,
            'firstName', child.first_name,
            'lastName', child.last_name,
            'ssn', child.social_security_number
        )), '[]'::jsonb) AS children,
        coalesce(sum(row.amount * row.unit_price), 0) AS total_price
    FROM invoice_row AS row
    JOIN person AS child ON row.child = child.id
    WHERE row.invoice_id = invoice.id
) AS rows_summary ON true
ORDER BY ${sortColumn.second} ${sortDirection.name}, invoice.id
"""
            )
        }
        .mapToPaged(::PagedInvoiceSummaries, pageSize)
}

fun Database.Read.searchInvoices(
    status: InvoiceStatus? = null,
    period: FiniteDateRange? = null,
): List<InvoiceDetailed> {
    val predicate =
        Predicate.all(
            listOfNotNull(
                if (status != null) Predicate { where("$it.status = ${bind(status)}") } else null,
                if (period != null)
                    Predicate {
                        where(
                            "$it.period_start = ${bind(period.start)} AND $it.period_end = ${bind(period.end)}"
                        )
                    }
                else null,
            )
        )

    return createQuery {
            sql(
                """
${subquery(invoiceDetailedQuery(predicate))}
ORDER BY status DESC, due_date, invoice.id
"""
            )
        }
        .toList()
}

fun Database.Read.getMaxInvoiceNumber(): Long {
    return createQuery { sql("SELECT max(number) FROM invoice") }.exactlyOneOrNull<Long?>() ?: 0
}

fun Database.Transaction.deleteDraftInvoices(draftIds: List<InvoiceId>) {
    if (draftIds.isEmpty()) return

    createUpdate {
            sql(
                "DELETE FROM invoice WHERE status = ${bind(InvoiceStatus.DRAFT.toString())}::invoice_status AND id = ANY(${bind(draftIds)})"
            )
        }
        .execute()
}

fun Database.Transaction.setDraftsSent(
    now: HelsinkiDateTime,
    invoices: List<InvoiceDetailed>,
    sentBy: EvakaUserId,
) {
    if (invoices.isEmpty()) return

    executeBatch(invoices) {
        sql(
            """
UPDATE invoice
SET
    number = ${bind { it.number }},
    invoice_date = ${bind { it.invoiceDate }},
    due_date = ${bind { it.dueDate }},
    sent_at = ${bind { now }},
    sent_by = ${bind(sentBy)},
    status = ${bind(InvoiceStatus.SENT)}
WHERE id = ${bind { it.id }}
"""
        )
    }
}

fun Database.Transaction.saveCostCenterFields(invoiceIds: List<InvoiceId>) =
    createUpdate {
            sql(
                """
UPDATE invoice_row
SET saved_cost_center = daycare.cost_center, saved_sub_cost_center = care_area.sub_cost_center
FROM daycare, care_area WHERE invoice_row.unit_id = daycare.id
AND daycare.care_area_id = care_area.id
AND invoice_id = ANY(${bind(invoiceIds)})
"""
            )
        }
        .execute()

fun Database.Transaction.setDraftsWaitingForManualSending(invoices: List<InvoiceDetailed>) {
    if (invoices.isEmpty()) return

    executeBatch(invoices) {
        sql(
            """
UPDATE invoice
SET
    status = ${bind(InvoiceStatus.WAITING_FOR_SENDING)},
    number = ${bind { it.number }},
    invoice_date = ${bind { it.invoiceDate }},
    due_date = ${bind { it.dueDate }}
WHERE id = ${bind { it.id }}
"""
        )
    }
}

fun Database.Transaction.deleteDraftInvoices(
    month: YearMonth,
    status: InvoiceStatus,
    headOfFamilyId: PersonId? = null, // null means all
): Int {
    require(status == InvoiceStatus.DRAFT || status == InvoiceStatus.REPLACEMENT_DRAFT)
    val headOfFamilyFilter =
        if (headOfFamilyId != null) {
            PredicateSql { where("head_of_family = ${bind(headOfFamilyId)}") }
        } else {
            PredicateSql.alwaysTrue()
        }
    return execute {
        sql(
            "DELETE FROM invoice WHERE status = ${bind(status)} AND period_start = ${bind(month)} AND ${predicate(headOfFamilyFilter)}"
        )
    }
}

fun Database.Transaction.lockInvoices(ids: List<InvoiceId>) {
    createUpdate { sql("SELECT id FROM invoice WHERE id = ANY(${bind(ids)}) FOR UPDATE") }.execute()
}

fun Database.Transaction.insertDraftInvoices(
    invoices: List<DraftInvoice>,
    status: InvoiceStatus,
    relatedFeeDecisions: Map<PersonId, List<FeeDecisionId>> = emptyMap(),
): List<InvoiceId> {
    // Only allow inserting drafts
    require(status == InvoiceStatus.DRAFT || status == InvoiceStatus.REPLACEMENT_DRAFT)

    val invoiceIds = insertInvoicesWithoutRows(status, invoices)
    check(invoiceIds.size == invoices.size)

    insertInvoiceRows(
        invoices.zip(invoiceIds).map { (invoice, invoiceId) -> invoiceId to invoice.rows }
    )
    insertInvoicedFeeDecisions(
        invoices.zip(invoiceIds).flatMap { (invoice, invoiceId) ->
            (relatedFeeDecisions[invoice.headOfFamily] ?: emptyList()).map { feeDecisionId ->
                invoiceId to feeDecisionId
            }
        }
    )
    return invoiceIds
}

private fun Database.Transaction.insertInvoicesWithoutRows(
    status: InvoiceStatus,
    invoices: List<DraftInvoice>,
): List<InvoiceId> =
    prepareBatch(invoices) {
            sql(
                """
INSERT INTO invoice (
    status,
    period_start,
    period_end,
    due_date,
    invoice_date,
    area_id,
    head_of_family,
    codebtor,
    revision_number,
    replaced_invoice_id
) VALUES (
    ${bind(status)},
    ${bind { it.periodStart }},
    ${bind { it.periodEnd }},
    ${bind { it.dueDate }},
    ${bind { it.invoiceDate }},
    ${bind { it.areaId }},
    ${bind { it.headOfFamily }},
    ${bind { it.codebtor }},
    ${bind { it.revisionNumber }},
    ${bind { it.replacedInvoiceId }}
) RETURNING id
"""
            )
        }
        .executeAndReturn()
        .toList()

private fun Database.Transaction.insertInvoiceRows(
    invoiceRows: List<Pair<InvoiceId, List<DraftInvoiceRow>>>
) {
    val batchRows: Sequence<Triple<InvoiceId, Int, DraftInvoiceRow>> =
        invoiceRows.asSequence().flatMap { (invoiceId, rows) ->
            rows.withIndex().map { (idx, row) -> Triple(invoiceId, idx, row) }
        }
    executeBatch(batchRows) {
        sql(
            """
INSERT INTO invoice_row (
    invoice_id,
    idx,
    child,
    amount,
    unit_price,
    period_start,
    period_end,
    product,
    unit_id,
    description,
    correction_id
) VALUES (
    ${bind { (invoiceId, _, _) -> invoiceId }},
    ${bind { (_, idx, _) -> idx }},
    ${bind { (_, _, row) -> row.childId }},
    ${bind { (_, _, row) -> row.amount }},
    ${bind { (_, _, row) -> row.unitPrice }},
    ${bind { (_, _, row) -> row.periodStart }},
    ${bind { (_, _, row) -> row.periodEnd }},
    ${bind { (_, _, row) -> row.product }},
    ${bind { (_, _, row) -> row.unitId }},
    ${bind { (_, _, row) -> row.description }},
    ${bind { (_, _, row) -> row.correctionId }}
)
"""
        )
    }
}

private fun Database.Transaction.insertInvoicedFeeDecisions(
    relations: List<Pair<InvoiceId, FeeDecisionId>>
) {
    if (relations.isEmpty()) return

    executeBatch(relations) {
        sql(
            """
        INSERT INTO invoiced_fee_decision (invoice_id, fee_decision_id) 
        VALUES (${bind { pair -> pair.first }}, ${bind { pair -> pair.second }})
    """
        )
    }
}

fun Database.Transaction.setReplacementDraftSent(
    invoiceId: InvoiceId,
    sentAt: HelsinkiDateTime,
    sentBy: EvakaUserId,
    reason: InvoiceReplacementReason,
    notes: String,
) {
    val replacedInvoiceId =
        createQuery {
                sql(
                    """
UPDATE invoice
SET status = 'SENT',
    replacement_reason = ${bind(reason)},
    replacement_notes = ${bind(notes)},
    sent_at = ${bind(sentAt)},
    sent_by = ${bind(sentBy)}
WHERE id = ${bind(invoiceId)} AND status = 'REPLACEMENT_DRAFT'
RETURNING replaced_invoice_id
"""
                )
            }
            .exactlyOne<InvoiceId?>()
    if (replacedInvoiceId != null) {
        createUpdate {
                sql(
                    """
UPDATE invoice
SET status = ${bind(InvoiceStatus.REPLACED)}
WHERE id = ${bind(replacedInvoiceId)}
"""
                )
            }
            .updateExactlyOne()
    }
}

fun Database.Read.getLastInvoicedMonth(): YearMonth? =
    createQuery { sql("SELECT MAX(invoice_date) AS month FROM invoice WHERE status = 'SENT'") }
        .exactlyOneOrNull<YearMonth>()
        // Invoices of month M are sent in month M+1
        ?.minusMonths(1)
