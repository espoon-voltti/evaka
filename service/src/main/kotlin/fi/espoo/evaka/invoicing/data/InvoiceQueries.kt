// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.controller.InvoiceDistinctiveParams
import fi.espoo.evaka.invoicing.controller.InvoiceSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.WithCount
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

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
        LEFT JOIN person as head ON invoice.head_of_family = head.id
        LEFT JOIN invoice_row as row ON invoice.id = row.invoice_id
        LEFT JOIN person as child ON row.child = child.id
    """.trimIndent()

fun Database.Read.getInvoicesByIds(ids: List<UUID>): List<InvoiceDetailed> {
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

fun Database.Read.getInvoice(uuid: UUID): Invoice? {
    val sql =
        """
        $invoiceQueryBase
        WHERE invoice.id = :id
    """

    return createQuery(sql)
        .bind("id", uuid)
        .map(toInvoice)
        .let(::flatten)
        .firstOrNull()
}

fun Database.Read.getDetailedInvoice(id: UUID): InvoiceDetailed? {
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

fun Database.Read.getHeadOfFamilyInvoices(headOfFamilyUuid: UUID): List<Invoice> {
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

fun Database.Read.getInvoiceIdsByDates(from: LocalDate, to: LocalDate, areas: List<String>): List<UUID> {
    val sql =
        """
        SELECT id FROM invoice
        WHERE daterange(:from, :to, '[]') @> invoice_date
        AND agreement_type IN (SELECT area_code FROM care_area WHERE short_name = ANY(:areas))
        AND status = :draft::invoice_status
    """

    return createQuery(sql)
        .bind("from", from)
        .bind("to", to)
        .bind("areas", areas.toTypedArray())
        .bind("draft", InvoiceStatus.DRAFT)
        .mapTo<UUID>()
        .toList()
}

fun Database.Read.paginatedSearch(
    page: Int = 1,
    pageSize: Int = 200,
    sortBy: InvoiceSortParam = InvoiceSortParam.STATUS,
    sortDirection: SortDirection = SortDirection.ASC,
    statuses: List<InvoiceStatus> = listOf(),
    areas: List<String> = listOf(),
    unit: UUID? = null,
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
        if (areas.isNotEmpty()) "invoice.agreement_type IN (SELECT area_code FROM care_area WHERE short_name = ANY(:area))" else null,
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
            invoice.sent_at,
            invoice.sent_by,
            invoice.agreement_type,
            head.date_of_birth as head_date_of_birth,
            head.first_name as head_first_name,
            head.last_name as head_last_name,
            head.social_security_number as head_ssn,
            head.street_address as head_street_address,
            head.postal_code as head_postal_code,
            head.post_office as head_post_office,
            head.restricted_details_enabled as head_restricted_details_enabled,
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
            LEFT JOIN invoice_row as row ON invoice.id = row.invoice_id
            LEFT JOIN person as child ON row.child = child.id
        ORDER BY ${sortColumn.second} ${sortDirection.name}, invoice.id
        """.trimIndent()

    return createQuery(sql)
        .bindMap(params + freeTextParams)
        .map { rs, ctx ->
            WithCount(rs.getInt("count"), toInvoiceSummary(rs, ctx))
        }
        .let(mapToPaged(pageSize))
        .let { it.copy(data = flattenSummary(it.data)) }
}

fun Database.Read.searchInvoices(
    statuses: List<InvoiceStatus> = listOf(),
    areas: List<String> = listOf(),
    unit: UUID? = null,
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
        if (areas.isNotEmpty()) "invoice.agreement_type IN (SELECT area_code FROM care_area WHERE short_name = ANY(:area))" else null,
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

fun Database.Transaction.deleteDraftInvoices(draftIds: List<UUID>) {
    if (draftIds.isEmpty()) return

    createUpdate("DELETE FROM invoice WHERE status = :status::invoice_status AND id = ANY(:draftIds)")
        .bind("status", InvoiceStatus.DRAFT.toString())
        .bind("draftIds", draftIds.toTypedArray())
        .execute()
}

fun Database.Transaction.setDraftsSent(idNumberPairs: List<Pair<UUID, Long>>, sentBy: UUID) {
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

fun Database.Transaction.updateToWaitingForSending(invoiceIds: List<UUID>) {
    if (invoiceIds.isEmpty()) return

    createUpdate("UPDATE invoice SET status = :status::invoice_status WHERE id = ANY(:ids)")
        .bind("ids", invoiceIds.toTypedArray())
        .bind("status", InvoiceStatus.WAITING_FOR_SENDING.toString())
        .execute()
}

fun Database.Transaction.updateInvoiceDates(invoiceIds: List<UUID>, invoiceDate: LocalDate, dueDate: LocalDate) {
    if (invoiceIds.isEmpty()) return

    val sql = "UPDATE invoice SET invoice_date = :invoiceDate, due_date = :dueDate WHERE id = ANY(:ids)"
    createUpdate(sql)
        .bind("ids", invoiceIds.toTypedArray())
        .bind("invoiceDate", invoiceDate)
        .bind("dueDate", dueDate)
        .execute()
}

fun Database.Transaction.deleteDraftInvoicesByPeriod(period: DateRange) {
    val sql =
        """
            DELETE FROM invoice
            WHERE status = :status::invoice_status
            AND daterange(:periodStart, :periodEnd, '[]') && daterange(period_start, period_end, '[]')
        """

    createUpdate(sql)
        .bind("periodStart", period.start)
        .bind("periodEnd", period.end)
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
            agreement_type,
            head_of_family
        ) VALUES (
            :id,
            :number,
            :status::invoice_status,
            :period_start,
            :period_end,
            :due_date,
            :invoice_date,
            :agreement_type,
            :head_of_family
        ) ON CONFLICT (id) DO NOTHING
    """

    val batch = prepareBatch(sql)
        .also { batch ->
            invoices.forEach { invoice ->
                batch
                    .bind("id", invoice.id)
                    .bind("number", invoice.number)
                    .bind("status", invoice.status.toString())
                    .bind("period_start", invoice.periodStart)
                    .bind("period_end", invoice.periodEnd)
                    .bind("due_date", invoice.dueDate)
                    .bind("invoice_date", invoice.invoiceDate)
                    .bind("agreement_type", invoice.agreementType)
                    .bind("head_of_family", invoice.headOfFamily.id)
                    .add()
            }
        }

    batch.execute()
}

private fun Database.Transaction.deleteInvoiceRows(invoiceIds: List<UUID>) {
    if (invoiceIds.isEmpty()) return

    createUpdate("DELETE FROM invoice_row WHERE invoice_id in (<invoiceIds>)")
        .bindList("invoiceIds", invoiceIds)
        .execute()
}

private fun Database.Transaction.insertInvoiceRows(invoiceRows: List<Pair<UUID, List<InvoiceRow>>>) {
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

    val batch = prepareBatch(sql)
        .also { batch ->
            invoiceRows.forEach { (invoiceId, rows) ->
                rows.map { row ->
                    batch
                        .bind("invoice_id", invoiceId)
                        .bind("id", row.id)
                        .bind("child", row.child.id)
                        .bind("date_of_birth", row.child.dateOfBirth)
                        .bind("amount", row.amount)
                        .bind("unit_price", row.unitPrice)
                        .bind("period_start", row.periodStart)
                        .bind("period_end", row.periodEnd)
                        .bind("product", row.product.toString())
                        .bind("cost_center", row.costCenter)
                        .bind("sub_cost_center", row.subCostCenter)
                        .bind("description", row.description)
                        .add()
                }
            }
        }

    batch.execute()
}

val toInvoice = { rs: ResultSet, _: StatementContext ->
    Invoice(
        id = UUID.fromString(rs.getString(rs.findColumn("id"))),
        number = rs.getObject("number") as Long?,
        status = InvoiceStatus.valueOf(rs.getString(rs.findColumn("status"))),
        periodStart = rs.getDate(rs.findColumn("period_start")).toLocalDate(),
        periodEnd = rs.getDate(rs.findColumn("period_end")).toLocalDate(),
        dueDate = rs.getDate(rs.findColumn("due_date")).toLocalDate(),
        invoiceDate = rs.getDate(rs.findColumn("invoice_date")).toLocalDate(),
        agreementType = rs.getInt("agreement_type"),
        rows = rs.getString("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRow(
                    id = UUID.fromString(rowId),
                    child = PersonData.WithDateOfBirth(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate()
                    ),
                    amount = rs.getInt("amount"),
                    unitPrice = rs.getInt("unit_price"),
                    periodStart = rs.getDate("invoice_row_period_start").toLocalDate(),
                    periodEnd = rs.getDate("invoice_row_period_end").toLocalDate(),
                    product = Product.valueOf(rs.getString("product")),
                    costCenter = rs.getString("cost_center"),
                    subCostCenter = rs.getString("sub_cost_center"),
                    description = rs.getString("description")
                )
            )
        } ?: listOf(),
        headOfFamily = PersonData.JustId(UUID.fromString(rs.getString("head_of_family"))),
        sentBy = rs.getString(rs.findColumn("sent_by"))?.let { UUID.fromString(it) },
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

val toDetailedInvoice = { rs: ResultSet, _: StatementContext ->
    InvoiceDetailed(
        id = UUID.fromString(rs.getString(rs.findColumn("id"))),
        number = rs.getObject("number") as Long?,
        status = InvoiceStatus.valueOf(rs.getString(rs.findColumn("status"))),
        periodStart = rs.getDate(rs.findColumn("period_start")).toLocalDate(),
        periodEnd = rs.getDate(rs.findColumn("period_end")).toLocalDate(),
        dueDate = rs.getDate(rs.findColumn("due_date")).toLocalDate(),
        invoiceDate = rs.getDate(rs.findColumn("invoice_date")).toLocalDate(),
        agreementType = rs.getInt("agreement_type"),
        rows = rs.getString("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRowDetailed(
                    id = UUID.fromString(rowId),
                    child = PersonData.Detailed(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn"),
                        streetAddress = rs.getString("child_street_address"),
                        postalCode = rs.getString("child_postal_code"),
                        postOffice = rs.getString("child_post_office"),
                        restrictedDetailsEnabled = rs.getBoolean("child_restricted_details_enabled")
                    ),
                    amount = rs.getInt("amount"),
                    unitPrice = rs.getInt("unit_price"),
                    periodStart = rs.getDate("invoice_row_period_start").toLocalDate(),
                    periodEnd = rs.getDate("invoice_row_period_end").toLocalDate(),
                    product = Product.valueOf(rs.getString("product")),
                    costCenter = rs.getString("cost_center"),
                    subCostCenter = rs.getString("sub_cost_center"),
                    description = rs.getString("description")
                )
            )
        } ?: listOf(),
        headOfFamily = PersonData.Detailed(
            id = UUID.fromString(rs.getString("head_of_family")),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn"),
            streetAddress = rs.getString("head_street_address"),
            postalCode = rs.getString("head_postal_code"),
            postOffice = rs.getString("head_post_office"),
            phone = rs.getString("head_phone"),
            email = rs.getString("head_email"),
            language = rs.getString("head_language"),
            invoiceRecipientName = rs.getString("head_invoice_recipient_name"),
            invoicingStreetAddress = rs.getString("head_invoicing_street_address"),
            invoicingPostalCode = rs.getString("head_invoicing_postal_code"),
            invoicingPostOffice = rs.getString("head_invoicing_post_office"),
            restrictedDetailsEnabled = rs.getBoolean("head_restricted_details_enabled")
        ),
        sentBy = rs.getString(rs.findColumn("sent_by"))?.let { UUID.fromString(it) },
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

val toInvoiceSummary = { rs: ResultSet, _: StatementContext ->
    InvoiceSummary(
        id = rs.getUUID("id"),
        status = rs.getEnum("status"),
        periodStart = rs.getDate("invoice_period_start").toLocalDate(),
        periodEnd = rs.getDate("invoice_period_end").toLocalDate(),
        rows = rs.getString("invoice_row_id")?.let { rowId ->
            listOf(
                InvoiceRowSummary(
                    id = UUID.fromString(rowId),
                    child = PersonData.Basic(
                        id = UUID.fromString(rs.getString("child")),
                        dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate(),
                        firstName = rs.getString("child_first_name"),
                        lastName = rs.getString("child_last_name"),
                        ssn = rs.getString("child_ssn")
                    ),
                    amount = rs.getInt("amount"),
                    unitPrice = rs.getInt("unit_price")
                )
            )
        } ?: listOf(),
        headOfFamily = PersonData.Detailed(
            id = rs.getUUID("head_of_family"),
            dateOfBirth = rs.getDate("head_date_of_birth").toLocalDate(),
            firstName = rs.getString("head_first_name"),
            lastName = rs.getString("head_last_name"),
            ssn = rs.getString("head_ssn"),
            streetAddress = rs.getString("head_street_address"),
            postalCode = rs.getString("head_postal_code"),
            postOffice = rs.getString("head_post_office"),
            restrictedDetailsEnabled = rs.getBoolean("head_restricted_details_enabled")
        ),
        sentBy = rs.getString(rs.findColumn("sent_by"))?.let { UUID.fromString(it) },
        sentAt = rs.getTimestamp("sent_at")?.toInstant()
    )
}

fun flattenSummary(invoices: List<InvoiceSummary>): List<InvoiceSummary> {
    val map = mutableMapOf<UUID, InvoiceSummary>()
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
    val map = mutableMapOf<UUID, Invoice>()
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
    val map = mutableMapOf<UUID, InvoiceDetailed>()
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
