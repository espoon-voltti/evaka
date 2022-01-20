// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.InvoiceDistinctiveParams
import fi.espoo.evaka.invoicing.controller.InvoiceSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import java.time.LocalDate

val invoiceQueryBase =
    """
    SELECT
        invoice.*,
        row.id as invoice_row_id,
        row.child,
        row.date_of_birth,
        row.amount,
        row.unit_price,
        row.price,
        row.period_start as invoice_row_period_start,
        row.period_end as invoice_row_period_end,
        row.product,
        row.cost_center,
        row.sub_cost_center,
        row.description
    FROM invoice LEFT JOIN invoice_row as row ON invoice.id = row.invoice_id
    """.trimIndent()

val invoiceDetailedQueryBase =
    """
    SELECT
        invoice.*,
        care_area.area_code AS agreement_type,
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
        row.id as invoice_row_id,
        row.child,
        row.date_of_birth,
        row.amount,
        row.unit_price,
        row.price,
        row.period_start as invoice_row_period_start,
        row.period_end as invoice_row_period_end,
        row.product,
        row.cost_center,
        row.sub_cost_center,
        row.description,
        child.date_of_birth as child_date_of_birth,
        child.first_name as child_first_name,
        child.last_name as child_last_name,
        child.social_security_number as child_ssn,
        child.street_address as child_street_address,
        child.postal_code as child_postal_code,
        child.post_office as child_post_office,
        child.restricted_details_enabled as child_restricted_details_enabled
    FROM invoice
        JOIN care_area ON invoice.area_id = care_area.id
        LEFT JOIN person as head ON invoice.head_of_family = head.id
        LEFT JOIN person as codebtor ON invoice.codebtor = codebtor.id
        LEFT JOIN invoice_row as row ON invoice.id = row.invoice_id
        LEFT JOIN person as child ON row.child = child.id
    """.trimIndent()

fun Database.Read.getInvoicesByIds(ids: List<InvoiceId>): List<InvoiceDetailed> {
    if (ids.isEmpty()) return listOf()

    val sql =
        """
        $invoiceDetailedQueryBase
        WHERE invoice.id = ANY(:ids)
    """

    return createQuery(sql)
        .bind("ids", ids.toTypedArray())
        .map(toDetailedInvoice)
        .let(::flattenDetailed)
}

fun Database.Read.getInvoice(id: InvoiceId): Invoice? {
    val sql =
        """
        $invoiceQueryBase
        WHERE invoice.id = :id
    """

    return createQuery(sql)
        .bind("id", id)
        .map(toInvoice)
        .let(::flatten)
        .firstOrNull()
}

fun Database.Read.getDetailedInvoice(id: InvoiceId): InvoiceDetailed? {
    val sql =
        """
        $invoiceDetailedQueryBase
        WHERE invoice.id = :id
    """
    return createQuery(sql)
        .bind("id", id)
        .map(toDetailedInvoice)
        .let(::flattenDetailed)
        .firstOrNull()
}

fun Database.Read.getHeadOfFamilyInvoices(headOfFamilyUuid: PersonId): List<Invoice> {
    val sql =
        """
        $invoiceQueryBase
        WHERE invoice.head_of_family = :headOfFamilyId
    """
    return createQuery(sql)
        .bind("headOfFamilyId", headOfFamilyUuid)
        .map(toInvoice)
        .let(::flatten)
}

fun Database.Read.getInvoiceIdsByDates(range: FiniteDateRange, areas: List<String>): List<InvoiceId> {
    val sql =
        """
        SELECT id FROM invoice
        WHERE between_start_and_end(:range, invoice_date)
        AND area_id IN (SELECT id FROM care_area WHERE short_name = ANY(:areas))
        AND status = :draft::invoice_status
    """

    return createQuery(sql)
        .bind("range", range)
        .bind("areas", areas.toTypedArray())
        .bind("draft", InvoiceStatus.DRAFT)
        .mapTo<InvoiceId>()
        .toList()
}

fun Database.Read.paginatedSearch(
    page: Int = 1,
    pageSize: Int = 200,
    sortBy: InvoiceSortParam = InvoiceSortParam.STATUS,
    sortDirection: SortDirection = SortDirection.ASC,
    statuses: List<InvoiceStatus> = listOf(),
    areas: List<String> = listOf(),
    unit: DaycareId? = null,
    distinctiveParams: List<InvoiceDistinctiveParams> = listOf(),
    searchTerms: String = "",
    periodStart: LocalDate? = null,
    periodEnd: LocalDate? = null
): Paged<InvoiceSummary> {
    val sortColumn = when (sortBy) {
        InvoiceSortParam.HEAD_OF_FAMILY -> Pair("max(head.last_name)", "head.last_name")
        InvoiceSortParam.CHILDREN -> Pair("max(child.last_name)", "child.last_name")
        InvoiceSortParam.START -> Pair("max(invoice.period_start)", "invoice.period_start")
        InvoiceSortParam.END -> Pair("max(invoice.period_end)", "invoice.period_end")
        InvoiceSortParam.SUM -> Pair("sum", "invoice_ids.sum")
        InvoiceSortParam.STATUS -> Pair("max(invoice.status)", "invoice.status")
        InvoiceSortParam.CREATED_AT -> Pair("max(invoice.created_at)", "invoice.created_at")
    }

    val params = mapOf<String, Any>()
        .let { ps -> ps + ("page" to page) }
        .let { ps -> ps + ("pageSize" to pageSize) }
        .let { ps -> if (areas.isNotEmpty()) ps + ("area" to areas.toTypedArray()) else ps }
        .let { ps -> if (statuses.isNotEmpty()) ps + ("status" to statuses.map { it.toString() }.toTypedArray()) else ps }
        .let { ps -> if (unit != null) ps + ("unit" to unit) else ps }
        .let { ps -> ps + ("periodStart" to periodStart) }
        .let { ps -> ps + ("periodEnd" to periodEnd) }

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("head", "child"), searchTerms)

    val withMissingAddress = distinctiveParams.contains(InvoiceDistinctiveParams.MISSING_ADDRESS)

    val conditions = listOfNotNull(
        if (statuses.isNotEmpty()) "invoice.status = ANY(:status::invoice_status[])" else null,
        if (areas.isNotEmpty()) "invoice.area_id IN (SELECT id FROM care_area WHERE short_name = ANY(:area))" else null,
        if (unit != null) "row.cost_center = (SELECT cost_center FROM daycare WHERE id = :unit)" else null,
        if (withMissingAddress) "COALESCE(NULLIF(head.invoicing_street_address, ''), NULLIF(head.street_address, '')) IS NULL" else null,
        if (searchTerms.isNotBlank()) freeTextQuery else null,
        if (periodStart != null) "invoice_date  >= :periodStart" else null,
        if (periodEnd != null) "invoice_date  <= :periodEnd" else null
    )

    val sql =
        """
        WITH invoice_ids AS (
            SELECT invoice.id, sum(amount * unit_price), count(*) OVER ()
            FROM invoice
            LEFT JOIN invoice_row AS row ON invoice.id = row.invoice_id
            LEFT JOIN person AS head ON invoice.head_of_family = head.id
            LEFT JOIN person AS child ON row.child = child.id
            ${if (conditions.isNotEmpty()) """
            WHERE ${conditions.joinToString("\nAND ")}
        """.trimIndent() else ""}
            GROUP BY invoice.id
            ORDER BY ${sortColumn.first} ${sortDirection.name}, invoice.id
            LIMIT :pageSize OFFSET (:page - 1) * :pageSize
        )
        SELECT
            invoice_ids.count,
            invoice.id,
            invoice.status,
            invoice.period_start as invoice_period_start,
            invoice.period_end as invoice_period_end,
            invoice.head_of_family,
            invoice.codebtor,
            invoice.sent_at,
            invoice.sent_by,
            invoice.area_id,
            invoice.created_at,
            head.date_of_birth as head_date_of_birth,
            head.first_name as head_first_name,
            head.last_name as head_last_name,
            head.social_security_number as head_ssn,
            head.street_address as head_street_address,
            head.postal_code as head_postal_code,
            head.post_office as head_post_office,
            head.restricted_details_enabled as head_restricted_details_enabled,
            codebtor.date_of_birth as codebtor_date_of_birth,
            codebtor.first_name as codebtor_first_name,
            codebtor.last_name as codebtor_last_name,
            codebtor.social_security_number as codebtor_ssn,
            codebtor.street_address as codebtor_street_address,
            codebtor.postal_code as codebtor_postal_code,
            codebtor.post_office as codebtor_post_office,
            codebtor.restricted_details_enabled as codebtor_restricted_details_enabled,
            row.id as invoice_row_id,
            row.child,
            row.amount,
            row.unit_price,
            row.price,
            child.date_of_birth as child_date_of_birth,
            child.first_name as child_first_name,
            child.last_name as child_last_name,
            child.social_security_number as child_ssn
        FROM invoice_ids
            LEFT JOIN invoice as invoice ON invoice_ids.id = invoice.id
            LEFT JOIN person as head ON invoice.head_of_family = head.id
            LEFT JOIN person as codebtor ON invoice.codebtor = codebtor.id
            LEFT JOIN invoice_row as row ON invoice.id = row.invoice_id
            LEFT JOIN person as child ON row.child = child.id
        ORDER BY ${sortColumn.second} ${sortDirection.name}, invoice.id
        """.trimIndent()

    return createQuery(sql)
        .bindMap(params + freeTextParams)
        .mapToPaged(pageSize, toInvoiceSummary)
        .let { it.copy(data = flattenSummary(it.data)) }
}

fun Database.Read.searchInvoices(
    statuses: List<InvoiceStatus> = listOf(),
    areas: List<String> = listOf(),
    unit: DaycareId? = null,
    distinctiveParams: List<InvoiceDistinctiveParams> = listOf(),
    sentAt: DateRange? = null
): List<InvoiceDetailed> {
    val params = mapOf<String, Any>()
        .let { ps -> if (areas.isNotEmpty()) ps + ("area" to areas.toTypedArray()) else ps }
        .let { ps -> if (statuses.isNotEmpty()) ps + ("status" to statuses.map { it.toString() }.toTypedArray()) else ps }
        .let { ps -> if (unit != null) ps + ("unit" to unit) else ps }
        .let { ps -> if (sentAt != null) ps + mapOf("sentAtStart" to sentAt.start, "sentAtEnd" to sentAt.end) else ps }

    val withMissingAddress = distinctiveParams.contains(InvoiceDistinctiveParams.MISSING_ADDRESS)

    val conditions = listOfNotNull(
        if (statuses.isNotEmpty()) "invoice.status = ANY(:status::invoice_status[])" else null,
        if (areas.isNotEmpty()) "invoice.area_id IN (SELECT id FROM care_area WHERE short_name = ANY(:area))" else null,
        if (unit != null) "row.cost_center = (SELECT cost_center FROM daycare WHERE id = :unit)" else null,
        if (withMissingAddress) "COALESCE(NULLIF(head.invoicing_street_address, ''), NULLIF(head.street_address, '')) IS NULL" else null,
        if (sentAt != null) "daterange(:sentAtStart, :sentAtEnd) @> invoice.sent_at::date" else null
    )

    val where =
        if (conditions.isEmpty()) ""
        else """
            WHERE invoice.id IN (
                SELECT invoice.id
                FROM invoice
                LEFT JOIN invoice_row AS row ON invoice.id = row.invoice_id
                LEFT JOIN person AS head ON invoice.head_of_family = head.id
                LEFT JOIN person AS child ON row.child = child.id
                WHERE ${conditions.joinToString("\nAND ")}
            )
        """.trimIndent()

    val sql =
        """
        $invoiceDetailedQueryBase
        $where
        ORDER BY status DESC, due_date
        """.trimIndent()

    return createQuery(sql)
        .bindMap(params)
        .map(toDetailedInvoice)
        .let(::flattenDetailed)
}

fun Database.Read.getMaxInvoiceNumber(): Long {
    return createQuery("SELECT max(number) FROM invoice")
        .mapTo(Long::class.java)
        .first()
}

fun Database.Transaction.deleteDraftInvoices(draftIds: List<InvoiceId>) {
    if (draftIds.isEmpty()) return

    createUpdate("DELETE FROM invoice WHERE status = :status::invoice_status AND id = ANY(:draftIds)")
        .bind("status", InvoiceStatus.DRAFT.toString())
        .bind("draftIds", draftIds.toTypedArray())
        .execute()
}

fun Database.Transaction.setDraftsSent(idNumberPairs: List<Pair<InvoiceId, Long>>, sentBy: EvakaUserId) {
    val sql =
        """
        UPDATE invoice
        SET
            number = :number,
            sent_at = now(),
            sent_by = :sentBy,
            status = :status::invoice_status
        WHERE id = :id
    """

    val batch = prepareBatch(sql)
    idNumberPairs.forEach { (id, number) ->
        batch
            .bind("id", id)
            .bind("number", number)
            .bind("status", InvoiceStatus.SENT.toString())
            .bind("sentBy", sentBy)
            .add()
    }
    batch.execute()
}

fun Database.Transaction.updateToWaitingForSending(invoiceIds: List<InvoiceId>) {
    if (invoiceIds.isEmpty()) return

    createUpdate("UPDATE invoice SET status = :status::invoice_status WHERE id = ANY(:ids)")
        .bind("ids", invoiceIds.toTypedArray())
        .bind("status", InvoiceStatus.WAITING_FOR_SENDING.toString())
        .execute()
}

fun Database.Transaction.updateInvoiceDates(invoiceIds: List<InvoiceId>, invoiceDate: LocalDate, dueDate: LocalDate) {
    if (invoiceIds.isEmpty()) return

    val sql = "UPDATE invoice SET invoice_date = :invoiceDate, due_date = :dueDate WHERE id = ANY(:ids)"
    createUpdate(sql)
        .bind("ids", invoiceIds.toTypedArray())
        .bind("invoiceDate", invoiceDate)
        .bind("dueDate", dueDate)
        .execute()
}

fun Database.Transaction.deleteDraftInvoicesByDateRange(range: DateRange) {
    val sql =
        """
            DELETE FROM invoice
            WHERE status = :status::invoice_status
            AND daterange(period_start, period_end, '[]') && :range
        """

    createUpdate(sql)
        .bind("range", range)
        .bind("status", InvoiceStatus.DRAFT.toString())
        .execute()
}

fun Database.Transaction.upsertInvoices(invoices: List<Invoice>) {
    upsertInvoicesWithoutRows(invoices)
    val rowsWithInvoiceIds = invoices.map { it.id to it.rows }
    deleteInvoiceRows(rowsWithInvoiceIds.map { it.first })
    insertInvoiceRows(rowsWithInvoiceIds)
}

private fun Database.Transaction.upsertInvoicesWithoutRows(invoices: List<Invoice>) {
    val sql =
        """
        INSERT INTO invoice (
            id,
            number,
            status,
            period_start,
            period_end,
            due_date,
            invoice_date,
            area_id,
            head_of_family,
            codebtor
        ) VALUES (
            :id,
            :number,
            :status::invoice_status,
            :periodStart,
            :periodEnd,
            :dueDate,
            :invoiceDate,
            :areaId,
            :headOfFamily,
            :codebtor
        ) ON CONFLICT (id) DO NOTHING
    """

    val batch = prepareBatch(sql)
        .also { batch ->
            invoices.forEach { invoice ->
                batch.bindKotlin(invoice).add()
            }
        }

    batch.execute()
}

private fun Database.Transaction.deleteInvoiceRows(invoiceIds: List<InvoiceId>) {
    if (invoiceIds.isEmpty()) return

    createUpdate("DELETE FROM invoice_row WHERE invoice_id in (<invoiceIds>)")
        .bindList("invoiceIds", invoiceIds)
        .execute()
}

private fun Database.Transaction.insertInvoiceRows(invoiceRows: List<Pair<InvoiceId, List<InvoiceRow>>>) {
    val sql =
        """
            INSERT INTO invoice_row (
                invoice_id,
                id,
                child,
                date_of_birth,
                amount,
                unit_price,
                period_start,
                period_end,
                product,
                cost_center,
                sub_cost_center,
                description
            ) VALUES (
                :invoice_id,
                :id,
                :child,
                :date_of_birth,
                :amount,
                :unit_price,
                :period_start,
                :period_end,
                :product,
                :cost_center,
                :sub_cost_center,
                :description
            )
        """

    prepareBatch(sql)
        .also { batch ->
            invoiceRows.forEach { (invoiceId, rows) ->
                rows.forEach { row ->
                    batch
                        .bind("invoice_id", invoiceId)
                        .bind("id", row.id)
                        .bind("child", row.child.id)
                        .bind("date_of_birth", row.child.dateOfBirth)
                        .bind("amount", row.amount)
                        .bind("unit_price", row.unitPrice)
                        .bind("period_start", row.periodStart)
                        .bind("period_end", row.periodEnd)
                        .bind("product", row.product)
                        .bind("cost_center", row.costCenter)
                        .bind("sub_cost_center", row.subCostCenter)
                        .bind("description", row.description)
                        .add()
                }
            }
        }
        .execute()
}

val toInvoice = { rv: RowView ->
    Invoice(
        id = rv.mapColumn("id"),
        number = rv.mapColumn("number"),
        status = rv.mapColumn("status"),
        periodStart = rv.mapColumn("period_start"),
        periodEnd = rv.mapColumn("period_end"),
        dueDate = rv.mapColumn("due_date"),
        invoiceDate = rv.mapColumn("invoice_date"),
        areaId = rv.mapColumn("area_id"),
        rows = rv.mapColumn<InvoiceRowId?>("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRow(
                    id = rowId,
                    child = ChildWithDateOfBirth(
                        id = rv.mapColumn("child"),
                        dateOfBirth = rv.mapColumn("date_of_birth")
                    ),
                    amount = rv.mapColumn("amount"),
                    unitPrice = rv.mapColumn("unit_price"),
                    periodStart = rv.mapColumn("invoice_row_period_start"),
                    periodEnd = rv.mapColumn("invoice_row_period_end"),
                    product = rv.mapColumn("product"),
                    costCenter = rv.mapColumn("cost_center"),
                    subCostCenter = rv.mapColumn("sub_cost_center"),
                    description = rv.mapColumn("description")
                )
            )
        } ?: listOf(),
        headOfFamily = rv.mapColumn("head_of_family"),
        codebtor = rv.mapColumn("codebtor"),
        sentBy = rv.mapColumn("sent_by"),
        sentAt = rv.mapColumn("sent_at")
    )
}

val toDetailedInvoice = { rv: RowView ->
    InvoiceDetailed(
        id = rv.mapColumn("id"),
        number = rv.mapColumn("number"),
        status = rv.mapColumn("status"),
        periodStart = rv.mapColumn("period_start"),
        periodEnd = rv.mapColumn("period_end"),
        dueDate = rv.mapColumn("due_date"),
        invoiceDate = rv.mapColumn("invoice_date"),
        agreementType = rv.mapColumn("agreement_type"),
        areaId = rv.mapColumn("area_id"),
        rows = rv.mapColumn<InvoiceRowId?>("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRowDetailed(
                    id = rowId,
                    child = PersonDetailed(
                        id = rv.mapColumn("child"),
                        dateOfBirth = rv.mapColumn("child_date_of_birth"),
                        firstName = rv.mapColumn("child_first_name"),
                        lastName = rv.mapColumn("child_last_name"),
                        ssn = rv.mapColumn("child_ssn"),
                        streetAddress = rv.mapColumn("child_street_address"),
                        postalCode = rv.mapColumn("child_postal_code"),
                        postOffice = rv.mapColumn("child_post_office"),
                        restrictedDetailsEnabled = rv.mapColumn("child_restricted_details_enabled")
                    ),
                    amount = rv.mapColumn("amount"),
                    unitPrice = rv.mapColumn("unit_price"),
                    periodStart = rv.mapColumn("invoice_row_period_start"),
                    periodEnd = rv.mapColumn("invoice_row_period_end"),
                    product = rv.mapColumn("product"),
                    costCenter = rv.mapColumn("cost_center"),
                    subCostCenter = rv.mapColumn("sub_cost_center"),
                    description = rv.mapColumn("description")
                )
            )
        } ?: listOf(),
        headOfFamily = PersonDetailed(
            id = rv.mapColumn("head_of_family"),
            dateOfBirth = rv.mapColumn("head_date_of_birth"),
            firstName = rv.mapColumn("head_first_name"),
            lastName = rv.mapColumn("head_last_name"),
            ssn = rv.mapColumn("head_ssn"),
            streetAddress = rv.mapColumn("head_street_address"),
            postalCode = rv.mapColumn("head_postal_code"),
            postOffice = rv.mapColumn("head_post_office"),
            phone = rv.mapColumn("head_phone"),
            email = rv.mapColumn("head_email"),
            language = rv.mapColumn("head_language"),
            invoiceRecipientName = rv.mapColumn("head_invoice_recipient_name"),
            invoicingStreetAddress = rv.mapColumn("head_invoicing_street_address"),
            invoicingPostalCode = rv.mapColumn("head_invoicing_postal_code"),
            invoicingPostOffice = rv.mapColumn("head_invoicing_post_office"),
            restrictedDetailsEnabled = rv.mapColumn("head_restricted_details_enabled")
        ),
        codebtor = rv.mapColumn<PersonId?>("codebtor")?.let { id ->
            PersonDetailed(
                id = id,
                dateOfBirth = rv.mapColumn("codebtor_date_of_birth"),
                firstName = rv.mapColumn("codebtor_first_name"),
                lastName = rv.mapColumn("codebtor_last_name"),
                ssn = rv.mapColumn("codebtor_ssn"),
                streetAddress = rv.mapColumn("codebtor_street_address"),
                postalCode = rv.mapColumn("codebtor_postal_code"),
                postOffice = rv.mapColumn("codebtor_post_office"),
                phone = rv.mapColumn("codebtor_phone"),
                email = rv.mapColumn("codebtor_email"),
                language = rv.mapColumn("codebtor_language"),
                restrictedDetailsEnabled = rv.mapColumn("codebtor_restricted_details_enabled")
            )
        },
        sentBy = rv.mapColumn("sent_by"),
        sentAt = rv.mapColumn("sent_at")
    )
}

val toInvoiceSummary = { row: RowView ->
    InvoiceSummary(
        id = row.mapColumn("id"),
        status = row.mapColumn("status"),
        periodStart = row.mapColumn("invoice_period_start"),
        periodEnd = row.mapColumn("invoice_period_end"),
        rows = row.mapColumn<InvoiceRowId?>("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRowSummary(
                    id = rowId,
                    child = PersonBasic(
                        id = row.mapColumn("child"),
                        dateOfBirth = row.mapColumn("child_date_of_birth"),
                        firstName = row.mapColumn("child_first_name"),
                        lastName = row.mapColumn("child_last_name"),
                        ssn = row.mapColumn("child_ssn")
                    ),
                    amount = row.mapColumn("amount"),
                    unitPrice = row.mapColumn("unit_price")
                )
            )
        } ?: listOf(),
        headOfFamily = PersonDetailed(
            id = row.mapColumn("head_of_family"),
            dateOfBirth = row.mapColumn("head_date_of_birth"),
            firstName = row.mapColumn("head_first_name"),
            lastName = row.mapColumn("head_last_name"),
            ssn = row.mapColumn("head_ssn"),
            streetAddress = row.mapColumn("head_street_address"),
            postalCode = row.mapColumn("head_postal_code"),
            postOffice = row.mapColumn("head_post_office"),
            restrictedDetailsEnabled = row.mapColumn("head_restricted_details_enabled")
        ),
        codebtor = row.mapColumn<PersonId?>("codebtor")?.let { id ->
            PersonDetailed(
                id = id,
                dateOfBirth = row.mapColumn("codebtor_date_of_birth"),
                firstName = row.mapColumn("codebtor_first_name"),
                lastName = row.mapColumn("codebtor_last_name"),
                ssn = row.mapColumn("codebtor_ssn"),
                streetAddress = row.mapColumn("codebtor_street_address"),
                postalCode = row.mapColumn("codebtor_postal_code"),
                postOffice = row.mapColumn("codebtor_post_office"),
                restrictedDetailsEnabled = row.mapColumn("codebtor_restricted_details_enabled")
            )
        },
        sentBy = row.mapColumn("sent_by"),
        sentAt = row.mapColumn("sent_at"),
        createdAt = row.mapColumn("created_at")
    )
}

fun flattenSummary(invoices: List<InvoiceSummary>): List<InvoiceSummary> {
    val map = mutableMapOf<InvoiceId, InvoiceSummary>()
    for (invoice in invoices) {
        val id = invoice.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.copy(rows = existing.rows + invoice.rows)
        } else {
            map[id] = invoice
        }
    }
    return map.values.toList()
}

fun flatten(invoices: Iterable<Invoice>): List<Invoice> {
    val map = mutableMapOf<InvoiceId, Invoice>()
    for (invoice in invoices) {
        val id = invoice.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.copy(rows = existing.rows + invoice.rows)
        } else {
            map[id] = invoice
        }
    }
    return map.values.toList()
}

fun flattenDetailed(invoices: Iterable<InvoiceDetailed>): List<InvoiceDetailed> {
    val map = mutableMapOf<InvoiceId, InvoiceDetailed>()
    for (invoice in invoices) {
        val id = invoice.id
        if (map.containsKey(id)) {
            val existing = map.getValue(id)
            map[id] = existing.copy(rows = existing.rows + invoice.rows)
        } else {
            map[id] = invoice
        }
    }
    return map.values.toList()
}
