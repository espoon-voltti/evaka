// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.invoicing.data.deleteDraftInvoicesByDateRange
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.insertInvoices
import fi.espoo.evaka.invoicing.data.partnerIsCodebtor
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.withSpan
import io.opentelemetry.api.trace.Tracer
import java.time.DayOfWeek
import java.time.Duration
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Component

@Component
class InvoiceGenerator(
    private val draftInvoiceGenerator: DraftInvoiceGenerator,
    private val featureConfig: FeatureConfig,
    private val tracer: Tracer = noopTracer(),
) {
    fun createAndStoreAllDraftInvoices(tx: Database.Transaction, month: YearMonth) {
        val range = FiniteDateRange.ofMonth(month)

        tx.setStatementTimeout(Duration.ofMinutes(10))
        tx.setLockTimeout(Duration.ofSeconds(15))
        tx.createUpdate { sql("LOCK TABLE invoice IN EXCLUSIVE MODE") }.execute()
        val invoiceCalculationData =
            tracer.withSpan("calculateInvoiceData") { calculateInvoiceData(tx, range) }
        val invoices = draftInvoiceGenerator.generateDraftInvoices(tx, invoiceCalculationData)
        val invoicesWithCorrections =
            tracer.withSpan("applyCorrections") {
                applyCorrections(tx, invoices, month, invoiceCalculationData.areaIds)
            }
        tx.deleteDraftInvoicesByDateRange(range)
        tx.insertInvoices(
            invoices = invoicesWithCorrections,
            relatedFeeDecisions =
                invoicesWithCorrections.associate { invoice ->
                    invoice.id to
                        invoiceCalculationData.decisions
                            .getOrDefault(invoice.headOfFamily, emptyList())
                            .map { it.id }
                },
        )
    }

    fun calculateInvoiceData(
        tx: Database.Read,
        range: FiniteDateRange,
    ): DraftInvoiceGenerator.InvoiceCalculationData {
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

        val absences = tx.getBillableAbsencesInRange(range)

        val freeChildren =
            if (
                range.start.month == Month.JULY &&
                    (range.end.month == Month.JULY && range.start.year == range.end.year)
            ) {
                tx.getFreeJulyChildren(range.start.year, featureConfig.freeJulyStartOnSeptember)
            } else {
                emptySet()
            }

        val codebtors =
            unhandledDecisions.mapValues { (_, decisions) ->
                getInvoiceCodebtor(tx, decisions, range)
            }

        val allChildren =
            unhandledDecisions.values
                .flatMap { feeDecisions ->
                    feeDecisions.flatMap { feeDecision -> feeDecision.children.map { it.child.id } }
                }
                .toSet() +
                permanentPlacements.keys +
                temporaryPlacements.values.flatMap { pairs -> pairs.map { it.second.child.id } }
        val operationalDaysByChild =
            tx.getOperationalDatesForChildren(range, allChildren).mapValues {
                DateSet.ofDates(it.value)
            }
        val holidays = tx.getHolidays(range)
        val businessDays =
            DateSet.ofDates(
                range.dates().filter {
                    it.dayOfWeek != DayOfWeek.SATURDAY &&
                        it.dayOfWeek != DayOfWeek.SUNDAY &&
                        !holidays.contains(it)
                }
            )

        val defaultServiceNeedOptions =
            tx.getServiceNeedOptions()
                .filter { it.defaultOption }
                .associateBy { it.validPlacementType }

        return DraftInvoiceGenerator.InvoiceCalculationData(
            decisions = unhandledDecisions,
            permanentPlacements = permanentPlacements,
            temporaryPlacements = temporaryPlacements,
            invoicePeriod = range,
            areaIds = areaIds,
            operationalDaysByChild = operationalDaysByChild,
            businessDays = businessDays,
            feeThresholds = feeThresholds,
            absences = absences,
            freeChildren = freeChildren,
            codebtors = codebtors,
            defaultServiceNeedOptions = defaultServiceNeedOptions,
        )
    }

    private fun getInvoiceCodebtor(
        tx: Database.Read,
        decisions: List<FeeDecision>,
        dateRange: FiniteDateRange,
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
                            ?: error("Decision is not valid during invoice period $dateRange"),
                    )
                }
            }
        }
    }

    fun applyCorrections(
        tx: Database.Read,
        invoices: List<Invoice>,
        targetMonth: YearMonth,
        areaIds: Map<DaycareId, AreaId>,
    ): List<Invoice> {
        val corrections = tx.getUnappliedInvoiceCorrections().groupBy { it.headOfFamilyId }

        val invoicesWithCorrections =
            corrections
                .map { (headOfFamily, headOfFamilyCorrections) ->
                    val invoice =
                        invoices.find { it.headOfFamily == headOfFamily }
                            ?: Invoice(
                                id = InvoiceId(UUID.randomUUID()),
                                status = InvoiceStatus.DRAFT,
                                periodStart = targetMonth.atDay(1),
                                periodEnd = targetMonth.atEndOfMonth(),
                                areaId =
                                    headOfFamilyCorrections.first().unitId.let {
                                        areaIds[it] ?: error("No areaId found for unit $it")
                                    },
                                headOfFamily = headOfFamily,
                                codebtor = null,
                                rows = listOf(),
                            )

                    val (additions, subtractions) =
                        headOfFamilyCorrections
                            .sortedByDescending { abs(it.amount * it.unitPrice) }
                            .partition { it.unitPrice > 0 }
                    val withAdditions =
                        invoice.copy(rows = invoice.rows + additions.map { it.toInvoiceRow() })

                    subtractions.fold(withAdditions) { invoiceWithSubtractions, subtraction ->
                        if (invoiceWithSubtractions.totalPrice == 0)
                            return@fold invoiceWithSubtractions

                        if (
                            (invoiceWithSubtractions.totalPrice + subtraction.unitPrice) >= 0
                        ) { // apply partial amount (also handles cases where the whole
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
                        } else { // apply partial unit price
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
}

fun Database.Read.getInvoiceableFeeDecisions(dateRange: FiniteDateRange): List<FeeDecision> {
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

fun Database.Read.getInvoicedHeadsOfFamily(period: FiniteDateRange): Set<PersonId> {
    val sent = listOf(InvoiceStatus.SENT, InvoiceStatus.WAITING_FOR_SENDING)
    return createQuery {
            sql(
                "SELECT DISTINCT head_of_family FROM invoice WHERE period_start = ${bind(period.start)} AND period_end = ${bind(period.end)} AND status = ANY(${bind(sent)}::invoice_status[])"
            )
        }
        .toSet<PersonId>()
}

fun Database.Read.getBillableAbsencesInRange(
    range: FiniteDateRange
): Map<AbsenceType, Map<ChildId, DateSet>> {
    return createQuery {
            sql(
                """
SELECT absence_type, child_id, range_agg(daterange(date, date, '[]')) AS dates
FROM absence
WHERE between_start_and_end(${bind(range)}, date)
AND category = 'BILLABLE'
GROUP BY child_id, absence_type
"""
            )
        }
        .toList {
            Triple(
                column<AbsenceType>("absence_type"),
                column<ChildId>("child_id"),
                column<DateSet>("dates"),
            )
        }
        .groupBy({ it.first }, { it.second to it.third })
        .mapValues { (_, values) -> values.associateBy({ it.first }, { it.second }) }
}

data class PlacementStub(
    @Nested("child") val child: ChildWithDateOfBirth,
    val unit: DaycareId,
    val type: PlacementType,
)

private fun Database.Read.getInvoiceablePlacements(
    spanningPeriod: FiniteDateRange,
    placementTypes: List<PlacementType>,
): Map<ChildId, List<Pair<FiniteDateRange, PlacementStub>>> {
    return createQuery {
            sql(
                """
SELECT p.child_id, c.date_of_birth AS child_date_of_birth, u.id AS unit, daterange(p.start_date, p.end_date, '[]') AS date_range, p.unit_id, p.type
FROM placement p
JOIN person c ON p.child_id = c.id
JOIN daycare u ON p.unit_id = u.id AND u.invoiced_by_municipality
WHERE daterange(start_date, end_date, '[]') && ${bind(spanningPeriod)}
AND p.type = ANY(${bind(placementTypes)}::placement_type[])
"""
            )
        }
        .toList { column<FiniteDateRange>("date_range") to row<PlacementStub>() }
        .groupBy { it.second.child.id }
}

private fun Database.Read.getInvoiceableTemporaryPlacements(
    spanningPeriod: FiniteDateRange
): Map<PersonId, List<Pair<FiniteDateRange, PlacementStub>>> {
    val placements = getInvoiceablePlacements(spanningPeriod, PlacementType.temporary)

    val familyCompositions =
        toFamilyCompositions(
            getChildrenWithHeadOfFamilies(placements.keys, spanningPeriod),
            spanningPeriod,
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
    relationships: List<Triple<FiniteDateRange, PersonId, ChildWithDateOfBirth>>,
    spanningPeriod: FiniteDateRange,
): Map<PersonId, List<Pair<FiniteDateRange, List<ChildWithDateOfBirth>>>> {
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
    dateRange: FiniteDateRange,
): List<Triple<FiniteDateRange, PersonId, ChildWithDateOfBirth>> {
    if (childIds.isEmpty()) return listOf()

    return createQuery {
            sql(
                """
SELECT
    fridge_child.head_of_child,
    child.id AS child_id,
    child.date_of_birth AS child_date_of_birth,
    fridge_child.start_date,
    fridge_child.end_date
FROM fridge_child
INNER JOIN person AS child ON fridge_child.child_id = child.id
WHERE fridge_child.child_id = ANY(${bind(childIds)})
    AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && ${bind(dateRange)}
    AND conflict = false
"""
            )
        }
        .toList {
            Triple(
                FiniteDateRange(column("start_date"), column("end_date")),
                column<PersonId>("head_of_child"),
                ChildWithDateOfBirth(
                    id = column("child_id"),
                    dateOfBirth = column("child_date_of_birth"),
                ),
            )
        }
}

fun Database.Read.getAreaIds(): Map<DaycareId, AreaId> =
    createQuery {
            sql(
                """
SELECT daycare.id AS unit_id, daycare.care_area_id AS area_id
FROM daycare
"""
            )
        }
        .toMap { columnPair("unit_id", "area_id") }

fun Database.Read.getFreeJulyChildren(year: Int, freeJulyStartOnSeptember: Boolean): Set<ChildId> =
    createQuery {
            val where =
                Predicate.allNotNull(
                    placementOn(year - 1, 8).takeUnless { freeJulyStartOnSeptember },
                    placementOn(year - 1, 9),
                    placementOn(year - 1, 10),
                    placementOn(year - 1, 11),
                    placementOn(year - 1, 12),
                    placementOn(year, 1),
                    placementOn(year, 2),
                    placementOn(year, 3),
                    placementOn(year, 4).takeIf { year != 2020 },
                    placementOn(year, 5).takeIf { year != 2020 },
                    placementOn(year, 6),
                )
            sql("SELECT id FROM child c WHERE ${predicate(where.forTable("c"))}")
        }
        .toSet<ChildId>()

private fun placementOn(year: Int, month: Int) = Predicate {
    where(
        """
EXISTS (
    SELECT 1 FROM placement
    WHERE
        child_id = $it.id AND
        type = ANY(${bind(PlacementType.invoiced)}) AND
        daterange(start_date, end_date, '[]') && ${bind(FiniteDateRange.ofMonth(year, Month.of(month)))}
)
"""
    )
}
