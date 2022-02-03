// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.invoicing.data.deleteDraftInvoicesByDateRange
import fi.espoo.evaka.invoicing.data.feeDecisionQueryBase
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.isElementaryFamily
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.operationalDays
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

class InvoiceGenerator(private val draftInvoiceGenerator: DraftInvoiceGenerator) {

    fun createAndStoreAllDraftInvoices(tx: Database.Transaction, range: DateRange = getPreviousMonthRange()) {
        tx.createUpdate("LOCK TABLE invoice IN EXCLUSIVE MODE").execute()
        val invoiceCalculationData = calculateInvoiceData(tx, range)
        val invoices = draftInvoiceGenerator.generateDraftInvoices(
            invoiceCalculationData.decisions,
            invoiceCalculationData.placements,
            invoiceCalculationData.period,
            invoiceCalculationData.daycareCodes,
            invoiceCalculationData.operationalDays,
            invoiceCalculationData.feeThresholds,
            invoiceCalculationData.absences,
            invoiceCalculationData.plannedAbsences,
            invoiceCalculationData.freeChildren,
            invoiceCalculationData.codebtors
        )
        tx.deleteDraftInvoicesByDateRange(range)
        tx.upsertInvoices(invoices)
    }

    fun calculateInvoiceData(tx: Database.Transaction, range: DateRange): InvoiceCalculationData {
        val feeThresholds = tx.getFeeThresholds(range.start).find { it.validDuring.includes(range.start) }
            ?: error("Missing prices for period ${range.start} - ${range.end}, cannot generate invoices")

        val effectiveDecisions = tx.getInvoiceableFeeDecisions(range).groupBy { it.headOfFamilyId }
        val placements = tx.getInvoiceablePlacements(range).groupBy { it.headOfFamily }
        val invoicedHeadsOfFamily = tx.getInvoicedHeadsOfFamily(range)

        val unhandledDecisions = effectiveDecisions.filterNot { invoicedHeadsOfFamily.contains(it.key) }
        val unhandledPlacements = placements.filterNot { invoicedHeadsOfFamily.contains(it.key) }
        val daycareCodes = tx.getDaycareCodes()
        val operationalDays = tx.operationalDays(range.start.year, range.start.month)

        val allAbsences = tx.getAbsenceStubs(range, setOf(AbsenceCategory.BILLABLE))

        val absences = allAbsences.filter { it.absenceType != AbsenceType.PLANNED_ABSENCE }
        val plannedAbsences =
            allAbsences.filter { it.absenceType == AbsenceType.PLANNED_ABSENCE }.groupBy { it.childId }
                .map { (childId, absences) -> childId to absences.map { it.date }.toSet() }.toMap()

        val freeChildren =
            if (range.start.month == Month.JULY && (range.end?.month == Month.JULY && range.start.year == range.end.year)) {
                tx.getFreeJulyChildren(range.start.year)
            } else emptyList()

        val codebtors = unhandledDecisions.mapValues { (_, decisions) -> getInvoiceCodebtor(tx, decisions) }

        return InvoiceCalculationData(
            decisions = unhandledDecisions,
            placements = unhandledPlacements,
            period = range,
            daycareCodes = daycareCodes,
            operationalDays = operationalDays,
            feeThresholds = feeThresholds,
            absences = absences,
            plannedAbsences = plannedAbsences,
            freeChildren = freeChildren,
            codebtors = codebtors
        )
    }

    data class InvoiceCalculationData(
        val decisions: Map<PersonId, List<FeeDecision>>,
        val placements: Map<PersonId, List<Placements>>,
        val period: DateRange,
        val daycareCodes: Map<DaycareId, DaycareCodes>,
        val operationalDays: OperationalDays,
        val feeThresholds: FeeThresholds,
        val absences: List<AbsenceStub> = listOf(),
        val plannedAbsences: Map<ChildId, Set<LocalDate>> = mapOf(),
        val freeChildren: List<ChildId> = listOf(),
        val codebtors: Map<PersonId, PersonId?> = mapOf()
    )

    private fun getInvoiceCodebtor(tx: Database.Transaction, decisions: List<FeeDecision>): PersonId? {
        val partners = decisions.map { it.partnerId }.distinct()
        if (partners.size != 1) return null

        val familyCompositions = decisions.map { Triple(it.headOfFamilyId, it.partnerId, it.children.map { it.child }) }
        return partners.first().takeIf {
            familyCompositions.all { (head, partner, children) ->
                if (partner == null) false
                else tx.isElementaryFamily(head, partner, children.map { it.id })
            }
        }
    }

    private fun getPreviousMonthRange(): DateRange {
        val lastMonth = LocalDate.now().minusMonths(1)
        val from = lastMonth.with(TemporalAdjusters.firstDayOfMonth())
        val to = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
        return DateRange(from, to)
    }

    fun generateDraftInvoiceDiffBetweenOldAndNewInvoiceGenerator() {
    }
}

fun Database.Read.getInvoiceableFeeDecisions(dateRange: DateRange): List<FeeDecision> {
    val sql =
        """
            $feeDecisionQueryBase
            WHERE decision.valid_during && :dateRange
                AND decision.status = ANY(:effective::fee_decision_status[])
        """

    return createQuery(sql)
        .bind("dateRange", dateRange)
        .bind("effective", FeeDecisionStatus.effective)
        .mapTo<FeeDecision>()
        .merge()
}

fun Database.Read.getInvoicedHeadsOfFamily(period: DateRange): List<PersonId> {
    val sql =
        "SELECT DISTINCT head_of_family FROM invoice WHERE period_start = :period_start AND period_end = :period_end AND status = :sent::invoice_status"

    return createQuery(sql)
        .bind("period_start", period.start)
        .bind("period_end", period.end)
        .bind("sent", InvoiceStatus.SENT)
        .mapTo<PersonId>()
        .list()
}

data class AbsenceStub(
    val childId: ChildId,
    val date: LocalDate,
    var category: AbsenceCategory,
    val absenceType: AbsenceType
)

fun Database.Read.getAbsenceStubs(spanningRange: DateRange, categories: Collection<AbsenceCategory>): List<AbsenceStub> {
    val sql =
        """
        SELECT child_id, date, category, absence_type
        FROM absence
        WHERE between_start_and_end(:range, date)
        AND category = ANY(:categories)
        """

    return createQuery(sql)
        .bind("range", spanningRange)
        .bind("categories", categories.toTypedArray())
        .mapTo<AbsenceStub>()
        .toList()
}

data class PlacementStub(val unit: DaycareId, val type: PlacementType)

data class Placements(
    val period: DateRange,
    val headOfFamily: PersonId,
    val placements: List<Pair<ChildWithDateOfBirth, PlacementStub>>
)

internal fun Database.Read.getInvoiceablePlacements(
    spanningPeriod: DateRange
): List<Placements> {
    data class PlacementRow(val dateRange: DateRange, val unitId: DaycareId, val type: PlacementType)

    val placements = createQuery(
        // language=sql
        """
            SELECT p.child_id, daterange(p.start_date, p.end_date, '[]') AS date_range, p.unit_id, p.type FROM placement p
            JOIN daycare u ON p.unit_id = u.id AND u.invoiced_by_municipality
            WHERE daterange(start_date, end_date, '[]') && :period
            AND p.type = ANY(:invoicedTypes::placement_type[])
        """.trimIndent()
    )
        .bind("period", spanningPeriod)
        .bind("invoicedTypes", PlacementType.invoiced().toTypedArray())
        .map { row -> row.mapColumn<ChildId>("child_id") to row.mapRow<PlacementRow>() }
        .groupBy { (childId) -> childId }
        .mapValues { it.value.map { (_, placements) -> placements } }

    val familyCompositions = toFamilyCompositions(
        getChildrenWithHeadOfFamilies(placements.keys, spanningPeriod),
        spanningPeriod
    )

    return familyCompositions
        .map { (headOfFamily, families) ->
            val relevantPlacements = families.flatMap { (period, children) ->
                children.flatMap { child ->
                    (placements[child.id] ?: listOf())
                        .filter { it.dateRange.overlaps(period) }
                        .map { (placementPeriod, placementUnit, placementType) ->
                            Triple(placementPeriod, child, PlacementStub(placementUnit, placementType))
                        }
                }
            }

            val allPeriods = families.map { (period, _) -> period } +
                relevantPlacements.map { (period) -> period }

            val familyPlacementsSeries = asDistinctPeriods(allPeriods, spanningPeriod).mapNotNull { period ->
                val family = families.find { it.first.contains(period) }

                family?.let { (_, children) ->
                    period to children
                        .sortedByDescending { it.dateOfBirth }
                        .mapNotNull { child ->
                            relevantPlacements.filter { it.first.contains(period) }.find { child.id == it.second.id }
                                ?.let { it.second to it.third }
                        }
                }
            }

            headOfFamily to mergePeriods(familyPlacementsSeries)
        }
        .flatMap { (headOfFamily, allPlacements) ->
            allPlacements.map { (period, placements) ->
                Placements(
                    period,
                    headOfFamily,
                    placements
                )
            }
        }
}

internal fun toFamilyCompositions(
    relationships: List<Triple<DateRange, PersonId, ChildWithDateOfBirth>>,
    spanningPeriod: DateRange
): Map<PersonId, List<Pair<DateRange, List<ChildWithDateOfBirth>>>> {
    return relationships
        .groupBy { (_, headOfFamily) -> headOfFamily }
        .mapValues { (_, value) -> value.map { it.first to it.third } }
        .mapValues { (_, children) ->
            asDistinctPeriods(children.map { it.first }, spanningPeriod).map { period ->
                period to children
                    .filter { it.first.contains(period) }
                    .map { (_, child) -> child }
                    .sortedByDescending { it.dateOfBirth }
            }.let { mergePeriods(it) }
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

    return createQuery(sql)
        .bind("dateRange", dateRange)
        .bind("childIds", childIds.toTypedArray())
        .map { rv ->
            Triple(
                DateRange(rv.mapColumn("start_date"), rv.mapColumn("end_date")),
                rv.mapColumn<PersonId>("head_of_child"),
                ChildWithDateOfBirth(
                    id = rv.mapColumn("child_id"),
                    dateOfBirth = rv.mapColumn("child_date_of_birth")
                )
            )
        }
        .list()
}

fun Database.Read.getDaycareCodes(): Map<DaycareId, DaycareCodes> {
    val sql =
        """
        SELECT daycare.id AS unit_id, area.id AS area_id
        FROM daycare INNER JOIN care_area AS area ON daycare.care_area_id = area.id
    """
    return createQuery(sql)
        .map { row -> row.mapColumn<DaycareId>("unit_id") to row.mapRow<DaycareCodes>() }
        .toMap()
}

fun Database.Read.getFreeJulyChildren(year: Int): List<ChildId> {
    val sql =
        //language=sql
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
        } else ""}
  (SELECT child_id FROM invoiced_placement WHERE ${placementOn(year, 6)}) p06
WHERE
  p09.child_id = p10.child_id AND
  p09.child_id = p11.child_id AND
  p09.child_id = p12.child_id AND
  p09.child_id = p01.child_id AND
  p09.child_id = p02.child_id AND
  p09.child_id = p03.child_id AND
  ${if (year != 2020)
            """
        p09.child_id = p04.child_id AND
        p09.child_id = p05.child_id AND
    """ else ""}
  p09.child_id = p06.child_id;
    """

    return createQuery(sql)
        .bind("invoicedTypes", PlacementType.invoiced().toTypedArray())
        .mapTo<ChildId>()
        .list()
}

private fun placementOn(year: Int, month: Int): String {
    val firstOfMonth = "'$year-$month-01'"
    return "daterange(start_date, end_date, '[]') && daterange($firstOfMonth::date, ($firstOfMonth::date + INTERVAL '1 month -1 day')::date, '[]')"
}

interface DraftInvoiceGenerator {
    fun generateDraftInvoices(
        decisions: Map<PersonId, List<FeeDecision>>,
        placements: Map<PersonId, List<Placements>>,
        period: DateRange,
        daycareCodes: Map<DaycareId, DaycareCodes>,
        operationalDays: OperationalDays,
        feeThresholds: FeeThresholds,
        absences: List<AbsenceStub> = listOf(),
        plannedAbsences: Map<ChildId, Set<LocalDate>> = mapOf(),
        freeChildren: List<ChildId> = listOf(),
        codebtors: Map<PersonId, PersonId?> = mapOf()
    ): List<Invoice>
}
