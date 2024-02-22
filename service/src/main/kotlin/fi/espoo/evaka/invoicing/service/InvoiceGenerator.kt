// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.invoicing.data.deleteDraftInvoicesByDateRange
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.partnerIsCodebtor
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.operationalDays
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.math.abs
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Component

@Component
class InvoiceGenerator(private val draftInvoiceGenerator: DraftInvoiceGenerator) {
    fun createAndStoreAllDraftInvoices(tx: Database.Transaction, range: DateRange) {
        tx.setStatementTimeout(Duration.ofMinutes(10))
        tx.setLockTimeout(Duration.ofSeconds(15))
        @Suppress("DEPRECATION") tx.createUpdate("LOCK TABLE invoice IN EXCLUSIVE MODE").execute()
        val invoiceCalculationData = calculateInvoiceData(tx, range)
        val invoices =
            draftInvoiceGenerator.generateDraftInvoices(
                tx,
                invoiceCalculationData.decisions,
                invoiceCalculationData.permanentPlacements,
                invoiceCalculationData.temporaryPlacements,
                invoiceCalculationData.period,
                invoiceCalculationData.areaIds,
                invoiceCalculationData.operationalDays,
                invoiceCalculationData.feeThresholds,
                invoiceCalculationData.absences,
                invoiceCalculationData.plannedAbsences,
                invoiceCalculationData.freeChildren,
                invoiceCalculationData.codebtors
            )
        val invoicesWithCorrections =
            applyCorrections(tx, invoices, range, invoiceCalculationData.areaIds)
        tx.deleteDraftInvoicesByDateRange(range)
        tx.upsertInvoices(invoicesWithCorrections)
    }

    fun calculateInvoiceData(tx: Database.Transaction, range: DateRange): InvoiceCalculationData {
        val feeThresholds =
            tx.getFeeThresholds(range.start).find { it.validDuring.includes(range.start) }
                ?: error(
                    "Missing prices for period ${range.start} - ${range.end}, cannot generate invoices"
                )

        val effectiveDecisions = tx.getInvoiceableFeeDecisions(range).groupBy { it.headOfFamilyId }
        val permanentPlacements = tx.getInvoiceablePlacements(range, PlacementType.invoiced)
        val temporaryPlacements = tx.getInvoiceableTemporaryPlacements(range)
        val invoicedHeadsOfFamily = tx.getInvoicedHeadsOfFamily(range)

        val unhandledDecisions =
            effectiveDecisions.filterNot { invoicedHeadsOfFamily.contains(it.key) }
        val areaIds = tx.getAreaIds()
        val operationalDays = tx.operationalDays(range.start.year, range.start.month)

        val allAbsences = tx.getAbsenceStubs(range, setOf(AbsenceCategory.BILLABLE))
        val plannedAbsences =
            allAbsences
                .filter { it.absenceType == AbsenceType.PLANNED_ABSENCE }
                .groupBy { it.childId }
                .map { (childId, absences) -> childId to absences.map { it.date }.toSet() }
                .toMap()

        val freeChildren =
            if (
                range.start.month == Month.JULY &&
                    (range.end?.month == Month.JULY && range.start.year == range.end.year)
            ) {
                tx.getFreeJulyChildren(range.start.year)
            } else {
                emptyList()
            }

        val codebtors =
            unhandledDecisions.mapValues { (_, decisions) ->
                getInvoiceCodebtor(tx, decisions, range)
            }

        return InvoiceCalculationData(
            decisions = unhandledDecisions,
            permanentPlacements = permanentPlacements,
            temporaryPlacements = temporaryPlacements,
            period = range,
            areaIds = areaIds,
            operationalDays = operationalDays,
            feeThresholds = feeThresholds,
            absences = allAbsences,
            plannedAbsences = plannedAbsences,
            freeChildren = freeChildren,
            codebtors = codebtors
        )
    }

    data class InvoiceCalculationData(
        val decisions: Map<PersonId, List<FeeDecision>>,
        val permanentPlacements: Map<ChildId, List<Pair<DateRange, PlacementStub>>>,
        val temporaryPlacements: Map<PersonId, List<Pair<DateRange, PlacementStub>>>,
        val period: DateRange,
        val areaIds: Map<DaycareId, AreaId>,
        val operationalDays: OperationalDays,
        val feeThresholds: FeeThresholds,
        val absences: List<AbsenceStub> = listOf(),
        val plannedAbsences: Map<ChildId, Set<LocalDate>> = mapOf(),
        val freeChildren: List<ChildId> = listOf(),
        val codebtors: Map<PersonId, PersonId?> = mapOf()
    )

    private fun getInvoiceCodebtor(
        tx: Database.Transaction,
        decisions: List<FeeDecision>,
        dateRange: DateRange
    ): PersonId? {
        val partners = decisions.map { it.partnerId }.distinct()
        if (partners.size != 1) return null

        return partners.first().takeIf {
            decisions.all { decision ->
                if (decision.partnerId == null) {
                    false
                } else {
                    tx.partnerIsCodebtor(
                        decision.headOfFamilyId,
                        decision.partnerId,
                        decision.children.map { it.child.id },
                        decision.validDuring.intersection(dateRange)
                            ?: error("Decision is not valid during invoice period $dateRange")
                    )
                }
            }
        }
    }

    fun applyCorrections(
        tx: Database.Read,
        invoices: List<Invoice>,
        invoicePeriod: DateRange,
        areaIds: Map<DaycareId, AreaId>
    ): List<Invoice> {
        val corrections = getUninvoicedCorrections(tx)

        val invoicesWithCorrections =
            corrections
                .map { (headOfFamily, headOfFamilyCorrections) ->
                    val invoice =
                        invoices.find { it.headOfFamily == headOfFamily }
                            ?: Invoice(
                                id = InvoiceId(UUID.randomUUID()),
                                status = InvoiceStatus.DRAFT,
                                periodStart = invoicePeriod.start,
                                periodEnd = invoicePeriod.end!!,
                                areaId =
                                    headOfFamilyCorrections.first().unitId.let {
                                        areaIds[it] ?: error("No areaId found for unit $it")
                                    },
                                headOfFamily = headOfFamily,
                                codebtor = null,
                                rows = listOf()
                            )

                    val (additions, subtractions) =
                        headOfFamilyCorrections.partition { it.unitPrice > 0 }
                    val withAdditions =
                        invoice.copy(rows = invoice.rows + additions.map { it.toInvoiceRow() })

                    subtractions
                        .sortedBy { it.period.start }
                        .fold(withAdditions) { invoiceWithSubtractions, subtraction ->
                            if (invoiceWithSubtractions.totalPrice == 0)
                                return@fold invoiceWithSubtractions

                            if ((invoiceWithSubtractions.totalPrice + subtraction.unitPrice) >= 0) {
                                // apply partial amount (also handles cases where the whole
                                // subtraction can be applied)
                                // integer division gives us the max amount of full refundable units
                                val maxApplicableAmount =
                                    invoiceWithSubtractions.totalPrice / abs(subtraction.unitPrice)
                                val subtractionWithMaxApplicableAmount =
                                    subtraction.copy(
                                        amount = minOf(subtraction.amount, maxApplicableAmount)
                                    )
                                invoiceWithSubtractions.copy(
                                    rows =
                                        invoiceWithSubtractions.rows +
                                            subtractionWithMaxApplicableAmount.toInvoiceRow()
                                )
                            } else {
                                // apply partial unit price
                                val maxUnitPrice =
                                    invoiceWithSubtractions.totalPrice / subtraction.amount
                                if (maxUnitPrice == 0) return@fold invoiceWithSubtractions

                                val subtractionWithMaxUnitPrice =
                                    subtraction.copy(unitPrice = -1 * maxUnitPrice)
                                invoiceWithSubtractions.copy(
                                    rows =
                                        invoiceWithSubtractions.rows +
                                            subtractionWithMaxUnitPrice.toInvoiceRow()
                                )
                            }
                        }
                }
                .filter { it.rows.isNotEmpty() }

        return invoicesWithCorrections +
            invoices.filterNot { invoice ->
                invoicesWithCorrections.any { correction -> invoice.id == correction.id }
            }
    }

    private fun getUninvoicedCorrections(
        tx: Database.Read
    ): Map<PersonId, List<InvoiceCorrection>> {
        val uninvoicedCorrectionsWithInvoicedTotals =
            @Suppress("DEPRECATION")
            tx.createQuery(
                    """
SELECT
    c.id,
    coalesce(
        jsonb_agg(jsonb_build_object('amount', r.amount, 'unitPrice', r.unit_price, 'periodStart', i.period_start)) FILTER (WHERE i.id IS NOT NULL),
        '[]'::jsonb
    ) AS invoiced_corrections
FROM invoice_correction c
LEFT JOIN invoice_row r ON c.id = r.correction_id
LEFT JOIN invoice i ON r.invoice_id = i.id AND i.status != 'DRAFT'
WHERE NOT c.applied_completely
GROUP BY c.id
HAVING c.amount * c.unit_price != coalesce(sum(r.amount * r.unit_price) FILTER (WHERE i.id IS NOT NULL), 0)
"""
                )
                .toMap {
                    column<InvoiceCorrectionId>("id") to
                        jsonColumn<List<InvoicedTotal>>("invoiced_corrections")
                }

        @Suppress("DEPRECATION")
        return tx.createQuery("SELECT * FROM invoice_correction WHERE id = ANY(:ids)")
            .bind("ids", uninvoicedCorrectionsWithInvoicedTotals.keys)
            .toList<InvoiceCorrection>()
            .groupBy { it.headOfFamilyId }
            .mapValues { (_, corrections) ->
                // Remove the already invoiced parts from corrections
                corrections.map { correction ->
                    val invoicedTotals = uninvoicedCorrectionsWithInvoicedTotals[correction.id]!!
                    invoicedTotals
                        .sortedBy { it.periodStart }
                        .fold(correction) { c, total ->
                            if (c.amount != total.amount) {
                                c.copy(amount = c.amount - total.amount)
                            } else {
                                c.copy(unitPrice = c.unitPrice - total.unitPrice)
                            }
                        }
                }
            }
    }

    private data class InvoicedTotal(
        val amount: Int,
        val unitPrice: Int,
        val periodStart: LocalDate
    )

    private data class InvoiceCorrection(
        val id: InvoiceCorrectionId,
        val headOfFamilyId: PersonId,
        val childId: ChildId,
        val unitId: DaycareId,
        val product: ProductKey,
        val period: FiniteDateRange,
        val amount: Int,
        val unitPrice: Int,
        val description: String
    ) {
        fun toInvoiceRow() =
            InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = childId,
                amount = amount,
                unitPrice = unitPrice,
                periodStart = period.start,
                periodEnd = period.end,
                product = product,
                unitId = unitId,
                description = description,
                correctionId = id
            )
    }
}

fun Database.Read.getInvoiceableFeeDecisions(dateRange: DateRange): List<FeeDecision> {
    @Suppress("DEPRECATION")
    return createQuery(
            feeDecisionQuery(
                Predicate {
                    where(
                        """
                            $it.valid_during && ${bind(dateRange)} AND
                            $it.status = ANY(${bind(FeeDecisionStatus.effective)}::fee_decision_status[])
                            """
                    )
                }
            )
        )
        .toList<FeeDecision>()
}

fun Database.Read.getInvoicedHeadsOfFamily(period: DateRange): List<PersonId> {
    val sql =
        "SELECT DISTINCT head_of_family FROM invoice WHERE period_start = :period_start AND period_end = :period_end AND status = ANY(:sent::invoice_status[])"

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("period_start", period.start)
        .bind("period_end", period.end)
        .bind("sent", listOf(InvoiceStatus.SENT, InvoiceStatus.WAITING_FOR_SENDING))
        .toList<PersonId>()
}

data class AbsenceStub(
    val childId: ChildId,
    val date: LocalDate,
    var category: AbsenceCategory,
    val absenceType: AbsenceType
)

fun Database.Read.getAbsenceStubs(
    spanningRange: DateRange,
    categories: Collection<AbsenceCategory>
): List<AbsenceStub> {
    val sql =
        """
        SELECT child_id, date, category, absence_type
        FROM absence
        WHERE between_start_and_end(:range, date)
        AND category = ANY(:categories)
        """

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("range", spanningRange)
        .bind("categories", categories)
        .toList<AbsenceStub>()
}

data class PlacementStub(
    @Nested("child") val child: ChildWithDateOfBirth,
    val unit: DaycareId,
    val type: PlacementType
)

private fun Database.Read.getInvoiceablePlacements(
    spanningPeriod: DateRange,
    placementTypes: List<PlacementType>
): Map<ChildId, List<Pair<DateRange, PlacementStub>>> {
    @Suppress("DEPRECATION")
    return createQuery(
            // language=sql
            """
SELECT p.child_id, c.date_of_birth AS child_date_of_birth, u.id AS unit, daterange(p.start_date, p.end_date, '[]') AS date_range, p.unit_id, p.type
FROM placement p
JOIN person c ON p.child_id = c.id
JOIN daycare u ON p.unit_id = u.id AND u.invoiced_by_municipality
WHERE daterange(start_date, end_date, '[]') && :period
AND p.type = ANY(:invoicedTypes::placement_type[])
"""
        )
        .bind("period", spanningPeriod)
        .bind("invoicedTypes", placementTypes)
        .toList { column<DateRange>("date_range") to row<PlacementStub>() }
        .groupBy { it.second.child.id }
}

private fun Database.Read.getInvoiceableTemporaryPlacements(
    spanningPeriod: DateRange
): Map<PersonId, List<Pair<DateRange, PlacementStub>>> {
    val placements = getInvoiceablePlacements(spanningPeriod, PlacementType.temporary)

    val familyCompositions =
        toFamilyCompositions(
            getChildrenWithHeadOfFamilies(placements.keys, spanningPeriod),
            spanningPeriod
        )

    return familyCompositions
        .map { (headOfFamily, families) ->
            val relevantPlacements =
                families.flatMap { (period, children) ->
                    children.flatMap { child ->
                        (placements[child.id] ?: listOf()).filter { it.first.overlaps(period) }
                    }
                }

            val allPeriods =
                families.map { (period, _) -> period } + relevantPlacements.map { it.first }

            val familyPlacementsSeries =
                asDistinctPeriods(allPeriods, spanningPeriod).mapNotNull { period ->
                    val family = families.find { it.first.contains(period) }

                    family?.let { (_, children) ->
                        period to
                            children
                                .sortedByDescending { it.dateOfBirth }
                                .mapNotNull { child ->
                                    relevantPlacements
                                        .filter { it.first.contains(period) }
                                        .find { child.id == it.second.child.id }
                                }
                                .map { it.second }
                    }
                }

            headOfFamily to
                mergePeriods(familyPlacementsSeries).flatMap { (period, placements) ->
                    placements.map { period to it }
                }
        }
        .toMap()
}

internal fun toFamilyCompositions(
    relationships: List<Triple<DateRange, PersonId, ChildWithDateOfBirth>>,
    spanningPeriod: DateRange
): Map<PersonId, List<Pair<DateRange, List<ChildWithDateOfBirth>>>> {
    return relationships
        .groupBy { (_, headOfFamily) -> headOfFamily }
        .mapValues { (_, value) -> value.map { it.first to it.third } }
        .mapValues { (_, children) ->
            asDistinctPeriods(children.map { it.first }, spanningPeriod)
                .map { period ->
                    period to
                        children
                            .filter { it.first.contains(period) }
                            .map { (_, child) -> child }
                            .sortedByDescending { it.dateOfBirth }
                }
                .let { mergePeriods(it) }
        }
}

fun Database.Read.getChildrenWithHeadOfFamilies(
    childIds: Collection<ChildId>,
    dateRange: DateRange
): List<Triple<DateRange, PersonId, ChildWithDateOfBirth>> {
    if (childIds.isEmpty()) return listOf()

    val sql =
        """
        SELECT
            fridge_child.head_of_child,
            child.id AS child_id,
            child.date_of_birth AS child_date_of_birth,
            fridge_child.start_date,
            fridge_child.end_date
        FROM fridge_child
        INNER JOIN person AS child ON fridge_child.child_id = child.id
        WHERE fridge_child.child_id = ANY(:childIds)
            AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && :dateRange
            AND conflict = false
    """

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("dateRange", dateRange).bind("childIds", childIds).toList {
        Triple(
            DateRange(column("start_date"), column("end_date")),
            column<PersonId>("head_of_child"),
            ChildWithDateOfBirth(
                id = column("child_id"),
                dateOfBirth = column("child_date_of_birth")
            )
        )
    }
}

fun Database.Read.getAreaIds(): Map<DaycareId, AreaId> {
    val sql =
        """
        SELECT daycare.id AS unit_id, area.id AS area_id
        FROM daycare INNER JOIN care_area AS area ON daycare.care_area_id = area.id
    """
    @Suppress("DEPRECATION") return createQuery(sql).toMap { columnPair("unit_id", "area_id") }
}

fun Database.Read.getFreeJulyChildren(year: Int): List<ChildId> {
    val sql =
        // language=sql
        """
WITH invoiced_placement AS (
    SELECT child_id, start_date, end_date FROM placement WHERE type = ANY(:invoicedTypes::placement_type[])
)
SELECT
  distinct(p09.child_id)
FROM
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year - 1, 9)}) p09,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year - 1, 10)}) p10,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year - 1, 11)}) p11,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year - 1, 12)}) p12,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 1)}) p01,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 2)}) p02,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 3)}) p03,
${if (year != 2020) {
            """
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 4)}) p04,
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 5)}) p05,
"""
        } else {
            ""
        }}
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 6)}) p06
WHERE
  p09.child_id = p10.child_id AND
  p09.child_id = p11.child_id AND
  p09.child_id = p12.child_id AND
  p09.child_id = p01.child_id AND
  p09.child_id = p02.child_id AND
  p09.child_id = p03.child_id AND
  ${if (year != 2020) {
            """
        p09.child_id = p04.child_id AND
        p09.child_id = p05.child_id AND
    """
        } else {
            ""
        }}
  p09.child_id = p06.child_id;
    """

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("invoicedTypes", PlacementType.invoiced).toList<ChildId>()
}

private fun placementOn(year: Int, month: Int): String {
    val firstOfMonth = "'$year-$month-01'"
    return "daterange(start_date, end_date, '[]') && daterange($firstOfMonth::date, ($firstOfMonth::date + INTERVAL '1 month -1 day')::date, '[]')"
}
