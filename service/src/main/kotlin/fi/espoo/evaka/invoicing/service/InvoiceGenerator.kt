// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.children.getChildIdsByGuardians
import fi.espoo.evaka.children.getChildIdsByHeadsOfFamily
import fi.espoo.evaka.invoicing.data.deleteDraftInvoices
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.getLastInvoicedMonth
import fi.espoo.evaka.invoicing.data.getSentInvoicesOfMonth
import fi.espoo.evaka.invoicing.data.insertDraftInvoices
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DraftInvoice
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.withSpan
import io.opentelemetry.api.trace.Tracer
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import kotlin.math.abs
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Component

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@Component
class InvoiceGenerator(
    private val draftInvoiceGenerator: DraftInvoiceGenerator,
    private val featureConfig: FeatureConfig,
    private val env: EvakaEnv,
    private val invoiceGenerationLogicChooser: InvoiceGenerationLogicChooser,
    private val tracer: Tracer = noopTracer(),
) {
    fun generateAllDraftInvoices(tx: Database.Transaction, month: YearMonth) {
        tx.setStatementTimeout(Duration.ofMinutes(10))
        tx.setLockTimeout(Duration.ofSeconds(15))
        tx.createUpdate { sql("LOCK TABLE invoice IN EXCLUSIVE MODE") }.execute()
        val invoiceCalculationData =
            tracer.withSpan("calculateInvoiceData") {
                calculateInvoiceData(tx, month, excludeAlreadyInvoiced = true)
            }
        val invoices = draftInvoiceGenerator.generateDraftInvoices(invoiceCalculationData)

        val invoicesWithCorrections =
            tracer.withSpan("applyCorrections") {
                applyUnappliedCorrections(tx, month, invoices, invoiceCalculationData.areaIds)
            }

        tx.deleteDraftInvoices(month, InvoiceStatus.DRAFT)
        tx.insertDraftInvoices(
            status = InvoiceStatus.DRAFT,
            invoices = invoicesWithCorrections,
            relatedFeeDecisions = invoiceCalculationData.decisionIds,
        )
    }

    fun generateAllReplacementDraftInvoices(dbc: Database.Connection, today: LocalDate) {
        forReplaceableMonths(dbc, today) { tx, month ->
            val invoiceCalculationData =
                tracer.withSpan("calculateInvoiceData") {
                    calculateInvoiceData(tx, month, excludeAlreadyInvoiced = false)
                }
            val drafts = createReplacementDraftInvoices(tx, month, invoiceCalculationData)

            tx.deleteDraftInvoices(month, InvoiceStatus.REPLACEMENT_DRAFT)
            tx.insertDraftInvoices(
                status = InvoiceStatus.REPLACEMENT_DRAFT,
                invoices = drafts,
                relatedFeeDecisions = invoiceCalculationData.decisionIds,
            )
        }
    }

    fun generateReplacementDraftInvoicesForHeadOfFamily(
        dbc: Database.Connection,
        today: LocalDate,
        headOfFamilyId: PersonId,
    ) =
        forReplaceableMonths(dbc, today) { tx, month ->
            val invoiceCalculationData =
                tracer.withSpan("calculateInvoiceData") {
                    calculateInvoiceData(tx, month, headOfFamilyId)
                }
            val drafts =
                createReplacementDraftInvoices(tx, month, invoiceCalculationData, headOfFamilyId)
            check(drafts.size <= 1) {
                "Generated multiple replacement draft invoices for $headOfFamilyId in month $month"
            }
            val draft = drafts.firstOrNull()

            tx.deleteDraftInvoices(month, InvoiceStatus.REPLACEMENT_DRAFT, headOfFamilyId)
            if (draft != null) {
                check(draft.headOfFamily == headOfFamilyId) {
                    "Generated replacement draft invoice for wrong head of family (${draft.headOfFamily} instead of $headOfFamilyId)"
                }
                tx.insertDraftInvoices(
                    status = InvoiceStatus.REPLACEMENT_DRAFT,
                    invoices = listOf(draft),
                    relatedFeeDecisions = invoiceCalculationData.decisionIds,
                )
            }
        }

    private fun forReplaceableMonths(
        dbc: Database.Connection,
        today: LocalDate,
        fn: (Database.Transaction, month: YearMonth) -> Unit,
    ) {
        val replacementInvoicesStart = env.replacementInvoicesStart
        if (replacementInvoicesStart == null) {
            logger.info { "Replacement invoices are not enabled" }
            return
        }

        val monthsToGenerate = 12L

        val latestMonth: YearMonth =
            dbc.read { tx -> tx.getLastInvoicedMonth() }
                ?: YearMonth.of(today.year, today.month).minusMonths(1)
        val earliestMonth =
            maxOf(replacementInvoicesStart, latestMonth.minusMonths(monthsToGenerate - 1))

        var month = earliestMonth
        while (month <= latestMonth) {
            try {
                dbc.transaction { tx -> fn(tx, month) }
            } catch (e: Exception) {
                logger.error(e) { "Failed to create replacement draft invoices for $month" }
            }
            month = month.plusMonths(1)
        }
    }

    private fun createReplacementDraftInvoices(
        tx: Database.Transaction,
        month: YearMonth,
        invoiceCalculationData: DraftInvoiceGenerator.InvoiceGeneratorInput,
        headOfFamilyId: PersonId? = null, // null means generate for all heads of family
    ): List<DraftInvoice> {
        val invoices = draftInvoiceGenerator.generateDraftInvoices(invoiceCalculationData)

        val invoicesWithCorrections =
            tracer.withSpan("applyCorrections") {
                applyCorrectionsForMonth(tx, month, invoices, invoiceCalculationData.areaIds)
            }
        val headsOfFamilyWithInvoices = invoicesWithCorrections.map { it.headOfFamily }.toSet()
        val sentInvoices =
            tx.getSentInvoicesOfMonth(month, headOfFamilyId).associateBy { it.headOfFamily.id }

        val newOrReplacedInvoices =
            invoicesWithCorrections.mapNotNull { invoice ->
                val sentInvoice = sentInvoices[invoice.headOfFamily]
                if (sentInvoice == null) {
                    // No corresponding sent invoice -> add a new replacement draft. Revision number
                    // starts from 1 to distinguish replacement invoices (manual processing) from
                    // normal invoices (sent through invoice integration).
                    invoice.copy(revisionNumber = 1)
                } else if (invoice.totalPrice != sentInvoice.totalPrice) {
                    // The corresponding sent invoice has a different price -> replace it
                    invoice.copy(
                        replacedInvoiceId = sentInvoice.id,
                        revisionNumber = sentInvoice.revisionNumber + 1,
                    )
                } else {
                    // Price didn't change, no need to replace
                    null
                }
            }
        val zeroInvoices =
            // Sent non-zero invoices that don't have a corresponding draft invoice -> add a
            // zero-priced replacement draft
            sentInvoices.values
                .filterNot { it.totalPrice == 0 }
                .filterNot { headsOfFamilyWithInvoices.contains(it.headOfFamily.id) }
                .map { sentInvoice ->
                    DraftInvoice(
                        periodStart = month.atDay(1),
                        periodEnd = month.atEndOfMonth(),
                        areaId = sentInvoice.areaId,
                        headOfFamily = sentInvoice.headOfFamily.id,
                        codebtor = null,
                        rows = listOf(),
                        revisionNumber = sentInvoice.revisionNumber + 1,
                        replacedInvoiceId = sentInvoice.id,
                    )
                }
        return newOrReplacedInvoices + zeroInvoices
    }

    private fun calculateInvoiceData(
        tx: Database.Read,
        month: YearMonth,
        excludeAlreadyInvoiced: Boolean,
    ): DraftInvoiceGenerator.InvoiceGeneratorInput {
        val range = FiniteDateRange.ofMonth(month)
        val effectiveDecisions = tx.getInvoiceableFeeDecisions(range).groupBy { it.headOfFamilyId }
        val unhandledDecisions =
            if (excludeAlreadyInvoiced) {
                val invoicedHeadsOfFamily = tx.getInvoicedHeadsOfFamily(range)
                effectiveDecisions.filterNot { invoicedHeadsOfFamily.contains(it.key) }
            } else {
                effectiveDecisions
            }

        val temporaryPlacements =
            tx.getInvoiceableTemporaryPlacements(FiniteDateRange.ofMonth(month))

        return calculateInvoiceData(tx, month, unhandledDecisions, temporaryPlacements)
    }

    private fun calculateInvoiceData(
        tx: Database.Read,
        month: YearMonth,
        headOfFamilyId: PersonId,
    ): DraftInvoiceGenerator.InvoiceGeneratorInput {
        val range = FiniteDateRange.ofMonth(month)
        val effectiveDecisions = tx.getInvoiceableFeeDecisions(range, headOfFamilyId)

        // There are very few temporary placements in total, so we can just fetch all of them
        val temporaryPlacements =
            tx.getInvoiceableTemporaryPlacements(FiniteDateRange.ofMonth(month)).filterKeys {
                it == headOfFamilyId
            }

        return calculateInvoiceData(
            tx,
            month,
            mapOf(headOfFamilyId to effectiveDecisions),
            temporaryPlacements,
        )
    }

    private fun calculateInvoiceData(
        tx: Database.Read,
        month: YearMonth,
        feeDecisions: Map<PersonId, List<FeeDecision>>,
        temporaryPlacements: Map<PersonId, List<Pair<FiniteDateRange, PlacementStub>>>,
    ): DraftInvoiceGenerator.InvoiceGeneratorInput {
        val range = FiniteDateRange.ofMonth(month)

        val codebtors = getInvoiceCodebtors(tx, feeDecisions, range)

        val decisionChildIds =
            feeDecisions.values
                .asSequence()
                .flatten()
                .flatMap { feeDecision -> feeDecision.children.asSequence().map { it.child.id } }
                .toSet()
        val permanentPlacements =
            tx.getInvoiceablePlacements(range, PlacementType.invoiced, decisionChildIds)

        val temporaryPlacementChildIds =
            temporaryPlacements.values.flatMap { placementRanges ->
                placementRanges.map { (_, placement) -> placement.child.id }
            }
        val allChildIds = decisionChildIds + temporaryPlacementChildIds

        val julyFreeChildren =
            if (month.month == Month.JULY) {
                tx.getFreeJulyChildren(
                    year = month.year,
                    childIds = allChildIds,
                    freeJulyStartOnSeptember = featureConfig.freeJulyStartOnSeptember,
                )
            } else {
                emptySet()
            }
        val extraFreeChildren =
            invoiceGenerationLogicChooser.getFreeChildren(tx, month, allChildIds)
        val freeChildren = julyFreeChildren + extraFreeChildren

        val absences = tx.getBillableAbsencesInRange(allChildIds, range)
        val operationalDaysByChild =
            tx.getOperationalDatesForChildren(range, allChildIds).mapValues {
                DateSet.ofDates(it.value)
            }

        val feeThresholds =
            tx.getFeeThresholds(range.start).find { it.validDuring.includes(range.start) }
                ?: error(
                    "Missing prices for period ${range.start} - ${range.end}, cannot generate invoices"
                )

        val holidays = getHolidays(range)
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

        val areaIds = tx.getAreaIds()

        return DraftInvoiceGenerator.InvoiceGeneratorInput(
            invoicePeriod = range,
            decisions = feeDecisions,
            codebtors = codebtors,
            permanentPlacements = permanentPlacements,
            temporaryPlacements = temporaryPlacements,
            absences = absences,
            freeChildren = freeChildren,
            operationalDaysByChild = operationalDaysByChild,
            businessDays = businessDays,
            feeThresholds = feeThresholds,
            defaultServiceNeedOptions = defaultServiceNeedOptions,
            minimumInvoiceAmount = featureConfig.minimumInvoiceAmount,
            areaIds = areaIds,
        )
    }

    private fun getInvoiceCodebtors(
        tx: Database.Read,
        headOfFamilyDecisions: Map<PersonId, List<FeeDecision>>,
        range: FiniteDateRange,
    ): Map<PersonId, PersonId> {
        val partnerByHeadOfFamily =
            headOfFamilyDecisions
                .mapNotNull { (headOfFamilyId, decisions) ->
                    val partnersOfHead = decisions.map { it.partnerId }.distinct()
                    if (partnersOfHead.size == 1) {
                        val partnerId = partnersOfHead.first()
                        if (partnerId != null) {
                            headOfFamilyId to partnerId
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .toMap()

        if (partnerByHeadOfFamily.isEmpty()) return emptyMap()

        val partnerIds = partnerByHeadOfFamily.values.toSet()
        val childrenByGuardian = tx.getChildIdsByGuardians(partnerIds)
        val childrenByHead = tx.getChildIdsByHeadsOfFamily(partnerIds, range)

        return headOfFamilyDecisions
            .mapNotNull { (headOfFamilyId, decisions) ->
                val partnerId = partnerByHeadOfFamily[headOfFamilyId] ?: return@mapNotNull null
                val partnerAsGuardian = childrenByGuardian[partnerId] ?: emptyList()
                val partnerAsHead = childrenByHead[partnerId] ?: emptyMap()
                if (partnerAsGuardian.isEmpty() && partnerAsHead.isEmpty()) return@mapNotNull null

                val hasCommonChildrenOnAllDecisions =
                    decisions.all { decision ->
                        decision.children.any {
                            if (partnerAsGuardian.contains(it.child.id)) {
                                true
                            } else {
                                val partnerAsHeadRange = partnerAsHead[it.child.id]
                                partnerAsHeadRange != null &&
                                    partnerAsHeadRange.overlaps(decision.validDuring)
                            }
                        }
                    }

                if (hasCommonChildrenOnAllDecisions) {
                    headOfFamilyId to partnerId
                } else {
                    null
                }
            }
            .toMap()
    }

    fun applyUnappliedCorrections(
        tx: Database.Read,
        targetMonth: YearMonth,
        invoices: List<DraftInvoice>,
        areaIds: Map<DaycareId, AreaId>,
    ): List<DraftInvoice> {
        val unappliedCorrections = tx.getUnappliedInvoiceCorrections().groupBy { it.headOfFamilyId }
        return applyCorrections(targetMonth, invoices, unappliedCorrections, areaIds)
    }

    private fun applyCorrectionsForMonth(
        tx: Database.Read,
        targetMonth: YearMonth,
        invoices: List<DraftInvoice>,
        areaIds: Map<DaycareId, AreaId>,
    ): List<DraftInvoice> {
        val correctionsForMonth =
            tx.getInvoiceCorrectionsForMonth(targetMonth).groupBy { it.headOfFamilyId }
        return applyCorrections(targetMonth, invoices, correctionsForMonth, areaIds)
    }

    private fun applyCorrections(
        targetMonth: YearMonth,
        invoices: List<DraftInvoice>,
        corrections: Map<PersonId, List<InvoiceCorrection>>,
        areaIds: Map<DaycareId, AreaId>,
    ): List<DraftInvoice> {
        val invoicesWithCorrections =
            corrections
                .map { (headOfFamily, headOfFamilyCorrections) ->
                    val invoice =
                        invoices.find { it.headOfFamily == headOfFamily }
                            ?: DraftInvoice(
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
                        invoice.copy(rows = invoice.rows + additions.map { it.toDraftInvoiceRow() })

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
                                        subtractionWithMaxApplicableAmount.toDraftInvoiceRow()
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
                                        subtractionWithMaxUnitPrice.toDraftInvoiceRow()
                            )
                        }
                    }
                }
                .filter { it.rows.isNotEmpty() }

        val invoicesWithoutCorrections =
            invoices.filterNot { invoice ->
                invoicesWithCorrections.any { it.headOfFamily == invoice.headOfFamily }
            }

        return invoicesWithCorrections + invoicesWithoutCorrections
    }
}

fun Database.Read.getInvoiceableFeeDecisions(
    dateRange: FiniteDateRange,
    headOfFamilyId: PersonId? = null,
): List<FeeDecision> {
    val headOfFamilyFilter =
        if (headOfFamilyId != null) {
            Predicate { where("$it.head_of_family_id = ${bind(headOfFamilyId)}") }
        } else {
            Predicate.alwaysTrue()
        }
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
                    .and(headOfFamilyFilter)
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
    childIds: Set<ChildId>,
    range: FiniteDateRange,
): Map<ChildId, List<Pair<AbsenceType, DateSet>>> {
    return createQuery {
            sql(
                """
SELECT child_id, absence_type, range_agg(daterange(date, date, '[]')) AS dates
FROM absence
WHERE
    child_id = ANY(${bind(childIds)}) AND
    between_start_and_end(${bind(range)}, date) AND
    category = 'BILLABLE'
GROUP BY child_id, absence_type
"""
            )
        }
        .map {
            Triple(
                column<ChildId>("child_id"),
                column<AbsenceType>("absence_type"),
                column<DateSet>("dates"),
            )
        }
        .useSequence { rows -> rows.groupBy({ it.first }, { it.second to it.third }) }
}

data class PlacementStub(
    @Nested("child") val child: ChildWithDateOfBirth,
    val unit: DaycareId,
    val type: PlacementType,
)

private fun Database.Read.getInvoiceablePlacements(
    spanningPeriod: FiniteDateRange,
    placementTypes: List<PlacementType>,
    childIds: Set<ChildId>?,
): Map<ChildId, List<Pair<FiniteDateRange, PlacementStub>>> {
    val childFilter =
        if (childIds != null) Predicate { where("$it.child_id = ANY(${bind(childIds)})") }
        else Predicate.alwaysTrue()
    return createQuery {
            sql(
                """
SELECT p.child_id, c.date_of_birth AS child_date_of_birth, u.id AS unit, daterange(p.start_date, p.end_date, '[]') AS date_range, p.unit_id, p.type
FROM placement p
JOIN person c ON p.child_id = c.id
JOIN daycare u ON p.unit_id = u.id AND u.invoiced_by_municipality
WHERE
    daterange(start_date, end_date, '[]') && ${bind(spanningPeriod)} AND
    p.type = ANY(${bind(placementTypes)}::placement_type[]) AND
    ${predicate(childFilter.forTable("p"))}
"""
            )
        }
        .toList { column<FiniteDateRange>("date_range") to row<PlacementStub>() }
        .groupBy { it.second.child.id }
}

private fun Database.Read.getInvoiceableTemporaryPlacements(
    spanningPeriod: FiniteDateRange
): Map<PersonId, List<Pair<FiniteDateRange, PlacementStub>>> {
    val placements =
        getInvoiceablePlacements(spanningPeriod, PlacementType.temporary, childIds = null)

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
                DateMap.of(familyPlacementsSeries)
                    .entries()
                    .flatMap { (period, placements) -> placements.map { period to it } }
                    .toList()
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
                .let { DateMap.of(it).entries().toList() }
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

fun Database.Read.getFreeJulyChildren(
    year: Int,
    childIds: Set<ChildId>?,
    freeJulyStartOnSeptember: Boolean,
): Set<ChildId> =
    createQuery {
            val where =
                Predicate.allNotNull(
                    Predicate { where("$it.id = ANY(${bind(childIds)})") }
                        .takeIf { childIds != null },
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
            sql(
                """
                SELECT id FROM child c WHERE ${predicate(where.forTable("c"))}"""
            )
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
