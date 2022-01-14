// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.daycare.service.AbsenceCareType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.invoicing.data.deleteDraftInvoicesByDateRange
import fi.espoo.evaka.invoicing.data.feeDecisionQueryBase
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.data.isElementaryFamily
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.Placement
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.invoicing.domain.TemporaryPlacement
import fi.espoo.evaka.invoicing.domain.getFeeAlterationProduct
import fi.espoo.evaka.invoicing.domain.getProductFromActivity
import fi.espoo.evaka.invoicing.domain.invoiceRowTotal
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.operationalDays
import fi.espoo.evaka.shared.domain.orMax
import org.jdbi.v3.core.kotlin.mapTo
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

fun Database.Transaction.createAllDraftInvoices(range: DateRange = getPreviousMonthRange()) {
    createUpdate("LOCK TABLE invoice IN EXCLUSIVE MODE").execute()

    val effectiveDecisions = getInvoiceableFeeDecisions(range).groupBy { it.headOfFamilyId }
    val placements = getInvoiceablePlacements(range).groupBy { it.headOfFamily }
    val invoicedHeadsOfFamily = getInvoicedHeadsOfFamily(range)

    val unhandledDecisions = effectiveDecisions.filterNot { invoicedHeadsOfFamily.contains(it.key) }
    val unhandledPlacements = placements.filterNot { invoicedHeadsOfFamily.contains(it.key) }
    val daycareCodes = getDaycareCodes()
    val operationalDays = operationalDays(range.start.year, range.start.month)

    val absences: List<AbsenceStub> = getAbsenceStubs(range, listOf(AbsenceCareType.DAYCARE, AbsenceCareType.PRESCHOOL_DAYCARE))

    val freeChildren =
        if (range.start.month == Month.JULY && (range.end?.month == Month.JULY && range.start.year == range.end.year)) {
            getFreeJulyChildren(range.start.year)
        } else emptyList()

    val codebtors = unhandledDecisions.mapValues { (_, decisions) -> getInvoiceCodebtor(this, decisions) }

    val feeThresholds = getFeeThresholds(range.start).find { it.validDuring.contains(range) }
        ?: error("Missing prices for period ${range.start} - ${range.end}, cannot generate invoices")

    val invoices =
        generateDraftInvoices(
            unhandledDecisions,
            unhandledPlacements,
            range,
            daycareCodes,
            operationalDays,
            absences,
            freeChildren,
            codebtors,
            feeThresholds
        )

    deleteDraftInvoicesByDateRange(range)
    upsertInvoices(invoices)
}

internal fun generateDraftInvoices(
    decisions: Map<PersonId, List<FeeDecision>>,
    placements: Map<PersonId, List<Placements>>,
    period: DateRange,
    daycareCodes: Map<DaycareId, DaycareCodes>,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub> = listOf(),
    freeChildren: List<ChildId> = listOf(),
    codebtors: Map<PersonId, PersonId?> = mapOf(),
    feeThresholds: FeeThresholds
): List<Invoice> {
    return placements.keys.mapNotNull { headOfFamilyId ->
        try {
            generateDraftInvoice(
                decisions[headOfFamilyId] ?: listOf(),
                placements[headOfFamilyId] ?: listOf(),
                period,
                daycareCodes,
                operationalDays,
                absences,
                freeChildren,
                codebtors,
                feeThresholds
            )
        } catch (e: Exception) {
            error("Failed to generate invoice for head of family $headOfFamilyId: $e")
        }
    }
}

data class InvoiceRowStub(
    val child: ChildWithDateOfBirth,
    val placement: Placement,
    val priceBeforeFeeAlterations: Int,
    val feeAlterations: List<Pair<FeeAlteration.Type, Int>>
)

internal fun generateDraftInvoice(
    decisions: List<FeeDecision>,
    placements: List<Placements>,
    invoicePeriod: DateRange,
    daycareCodes: Map<DaycareId, DaycareCodes>,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub>,
    freeChildren: List<ChildId>,
    codebtors: Map<PersonId, PersonId?>,
    feeThresholds: FeeThresholds
): Invoice? {
    val headOfFamily = placements.first().headOfFamily

    val rowStubs = placements.flatMap { (placementsPeriod, _, childPlacementPairs) ->
        val relevantPeriod = DateRange(
            maxOf(invoicePeriod.start, placementsPeriod.start),
            minOf(orMax(invoicePeriod.end), orMax(placementsPeriod.end))
        )
        val periodDecisions = decisions.filter { placementsPeriod.overlaps(DateRange(it.validFrom, it.validTo)) }

        childPlacementPairs
            .sortedByDescending { (child) -> child.dateOfBirth }
            .mapIndexed { index, (child, placement) ->
                when (placement) {
                    is PlacementStub.Temporary -> listOf(
                        relevantPeriod to InvoiceRowStub(
                            ChildWithDateOfBirth(child.id, child.dateOfBirth),
                            TemporaryPlacement(placement.unit, placement.partDay),
                            feeThresholds.calculatePriceForTemporary(placement.partDay, index + 1),
                            listOf()
                        )
                    )
                    is PlacementStub.Permanent ->
                        periodDecisions
                            .mapNotNull { decision ->
                                decision.children.find { part -> part.child == child }
                                    ?.let { DateRange(decision.validFrom, decision.validTo) to it }
                            }
                            .filterNot { (_, part) -> part.finalFee == 0 }
                            .filterNot { (_, part) -> freeChildren.contains(part.child.id) }
                            .map { (decisionPeriod, part) ->
                                DateRange(
                                    maxOf(relevantPeriod.start, decisionPeriod.start),
                                    minOf(orMax(relevantPeriod.end), orMax(decisionPeriod.end))
                                ) to InvoiceRowStub(
                                    ChildWithDateOfBirth(part.child.id, part.child.dateOfBirth),
                                    PermanentPlacement(part.placement.unitId, part.placement.type),
                                    part.fee,
                                    part.feeAlterations.map { feeAlteration ->
                                        Pair(feeAlteration.type, feeAlteration.effect)
                                    }
                                )
                            }
                }
            }
            .flatten()
    }

    val rows = rowStubs
        .groupBy { (_, stub) -> stub.child }
        .flatMap { (_, childStubs) ->
            mergePeriods(childStubs).fold(listOf<InvoiceRow>()) { existingRows, (period, rowStub) ->
                val placementUnit = rowStub.placement.unit
                val codes = daycareCodes[placementUnit]
                    ?: error("Couldn't find invoice codes for daycare ($placementUnit)")
                existingRows + toInvoiceRows(
                    period,
                    existingRows.map { DateRange(it.periodStart, it.periodEnd) },
                    rowStub,
                    codes,
                    operationalDays,
                    absences.filter { it.childId == rowStub.child.id }
                )
            }
        }
        .let { rows -> applyRoundingRows(rows, decisions, invoicePeriod) }
        .filter { row -> row.price != 0 }

    if (rows.isEmpty()) return null

    val areaId = rowStubs
        .maxByOrNull { (_, stub) -> stub.child.dateOfBirth }!!
        .let { (_, stub) ->
            daycareCodes[stub.placement.unit]?.areaId
                ?: error("Couldn't find areaId for daycare (${stub.placement.unit})")
        }

    return Invoice(
        id = InvoiceId(UUID.randomUUID()),
        status = InvoiceStatus.DRAFT,
        periodStart = invoicePeriod.start,
        periodEnd = invoicePeriod.end!!,
        areaId = areaId,
        headOfFamily = headOfFamily,
        codebtor = codebtors[headOfFamily],
        rows = rows
    )
}

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

internal fun calculateDailyPriceForInvoiceRow(price: Int, operationalDays: Int): Int {
    return BigDecimal(price).divide(BigDecimal(operationalDays), 0, RoundingMode.HALF_UP).toInt()
}

internal fun toInvoiceRows(
    period: DateRange,
    processedPeriods: List<DateRange>,
    invoiceRowStub: InvoiceRowStub,
    codes: DaycareCodes,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub>
): List<InvoiceRow> {
    val (child, placement, price, feeAlterations) = invoiceRowStub

    return when (placement) {
        is PermanentPlacement -> toPermanentPlacementInvoiceRows(
            period,
            processedPeriods,
            child,
            placement,
            price,
            codes,
            operationalDays,
            feeAlterations,
            absences
        )
        is TemporaryPlacement -> toTemporaryPlacementInvoiceRows(
            period,
            child,
            price,
            codes,
            operationalDays.forUnit(placement.unit),
            absences
        )
    }
}

private fun toTemporaryPlacementInvoiceRows(
    period: DateRange,
    child: ChildWithDateOfBirth,
    price: Int,
    codes: DaycareCodes,
    operationalDays: List<LocalDate>,
    absences: List<AbsenceStub>
): List<InvoiceRow> {
    val amount = operationalDays
        .filter { day -> period.includes(day) }
        .filter { day -> absences.none { absence -> absence.date == day } }
        .size

    return if (amount == 0) listOf()
    else listOf(
        InvoiceRow(
            id = InvoiceRowId(UUID.randomUUID()),
            periodStart = period.start,
            periodEnd = period.end!!,
            child = child,
            amount = amount,
            unitPrice = price,
            costCenter = codes.costCenter!!,
            subCostCenter = codes.subCostCenter,
            product = Product.TEMPORARY_CARE
        )
    )
}

private fun toPermanentPlacementInvoiceRows(
    period: DateRange,
    processedPeriods: List<DateRange>,
    child: ChildWithDateOfBirth,
    placement: PermanentPlacement,
    price: Int,
    codes: DaycareCodes,
    operationalDays: OperationalDays,
    feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
    absences: List<AbsenceStub>
): List<InvoiceRow> {
    val relevantDays = run {
        val unitOperationalDays = operationalDays.forUnit(placement.unit)
        if (operationalDays.generalCase == unitOperationalDays) {
            operationalDays.generalCase.filter { day -> period.includes(day) }
        }

        val weeks = operationalDays.fullMonth.fold(listOf<List<LocalDate>>()) { weeks, date ->
            if (weeks.isEmpty()) listOf(listOf(date))
            else {
                if (date.dayOfWeek == DayOfWeek.MONDAY) weeks.plusElement(listOf(date))
                else weeks.subList(0, weeks.size - 1).plusElement(weeks.last() + date)
            }
        }

        weeks.flatMap { week ->
            val operationalWeekDays = week.filter { unitOperationalDays.contains(it) }
            val (alreadyProcessedDays, daysLeftToProcess) = operationalWeekDays.partition { day ->
                processedPeriods.any { it.includes(day) }
            }
            daysLeftToProcess
                .filter { period.includes(it) }
                .take(maxOf(0, operationalDays.generalCase.intersect(week).size - alreadyProcessedDays.size))
        }
    }
    if (relevantDays.isEmpty()) return listOf()

    val product = getProductFromActivity(placement.type)
    val (amount, unitPrice) = if (relevantDays.size == operationalDays.generalCase.size) Pair(
        1,
        { p: Int -> p }
    ) else Pair(
        relevantDays.size,
        { p: Int -> calculateDailyPriceForInvoiceRow(p, operationalDays.generalCase.size) }
    )

    fun getDailyAbsenceModifiers(price: Int): List<InvoiceRow> =
        if (price == 0) listOf()
        else getDailyDiscountDates(
            period,
            child.dateOfBirth,
            absences,
            operationalDays.forUnit(placement.unit),
            operationalDays.generalCase.size
        ).map { (dates, product) ->
            if (dates.size == operationalDays.generalCase.size) InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = child,
                periodStart = period.start,
                periodEnd = period.end!!,
                product = product,
                costCenter = codes.costCenter!!,
                subCostCenter = codes.subCostCenter,
                amount = 1,
                unitPrice = -price
            )
            else InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = child,
                periodStart = period.start,
                periodEnd = period.end!!,
                product = product,
                costCenter = codes.costCenter!!,
                subCostCenter = codes.subCostCenter,
                amount = dates.size,
                unitPrice = -getDailyDiscount(period, price, operationalDays.generalCase)
            )
        }

    fun getMonthlyAbsenceModifiers(price: Int): List<InvoiceRow> =
        if (price == 0) listOf()
        else listOfNotNull(
            getAbsenceProduct(absences, operationalDays.forUnit(placement.unit), operationalDays.generalCase.size)?.let {
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    child = child,
                    periodStart = period.start,
                    periodEnd = period.end!!,
                    product = it,
                    costCenter = codes.costCenter!!,
                    subCostCenter = codes.subCostCenter,
                    amount = amount,
                    unitPrice = BigDecimal(
                        when (it) {
                            Product.ABSENCE,
                            Product.SICK_LEAVE_50 ->
                                BigDecimal(-price).divide(BigDecimal(2), 0, RoundingMode.HALF_UP).toInt()
                            Product.SICK_LEAVE_100 -> -price
                            else -> 0
                        }
                    ).divide(BigDecimal(amount), 0, RoundingMode.HALF_UP).toInt()
                )
            }
        )

    val initialRows = listOf(
        InvoiceRow(
            id = InvoiceRowId(UUID.randomUUID()),
            child = child,
            periodStart = period.start,
            periodEnd = period.end!!,
            amount = amount,
            unitPrice = unitPrice(price),
            costCenter = codes.costCenter!!,
            subCostCenter = codes.subCostCenter,
            product = product
        )
    ) + feeAlterations.map { (feeAlterationType, feeAlterationEffect) ->
        InvoiceRow(
            id = InvoiceRowId(UUID.randomUUID()),
            periodStart = period.start,
            periodEnd = period.end,
            child = child,
            product = getFeeAlterationProduct(product, feeAlterationType),
            costCenter = codes.costCenter,
            subCostCenter = codes.subCostCenter,
            amount = amount,
            unitPrice = unitPrice(feeAlterationEffect)
        )
    }

    val withDailyDiscountModifiers = initialRows + getDailyAbsenceModifiers(invoiceRowTotal(initialRows))
    return withDailyDiscountModifiers + getMonthlyAbsenceModifiers(invoiceRowTotal(withDailyDiscountModifiers))
}

internal fun getAbsenceProduct(
    absences: List<AbsenceStub>,
    unitOperationalDays: List<LocalDate>,
    monthlyOperationalDays: Int
): Product? {
    val relevantAbsences = absences.filter { unitOperationalDays.contains(it.date) }
    val sickAbsences = relevantAbsences.filter { it.absenceType == AbsenceType.SICKLEAVE }.map { it.date }
    val allAbsences = relevantAbsences.map { it.date }

    return when {
        sickAbsences.size == monthlyOperationalDays -> Product.SICK_LEAVE_100
        sickAbsences.size >= 11 -> Product.SICK_LEAVE_50
        allAbsences.size == monthlyOperationalDays -> Product.ABSENCE
        else -> null
    }
}

internal fun getDailyDiscountDates(
    period: DateRange,
    childDob: LocalDate,
    absences: List<AbsenceStub>,
    operationalDays: List<LocalDate>,
    monthlyOperationalDays: Int
): List<Pair<List<LocalDate>, Product>> {
    val relevantOperationalDays = operationalDays.filter { period.includes(it) }

    val forceMajeureAbsences = absences.filter { it.absenceType == AbsenceType.FORCE_MAJEURE }
    val forceMajeureDays =
        relevantOperationalDays.filter { day -> forceMajeureAbsences.find { day == it.date } != null }

    val parentLeaveAbsences = absences.filter { it.absenceType == AbsenceType.PARENTLEAVE }
    val parentLeaveDays = relevantOperationalDays
        .filter { ChronoUnit.YEARS.between(childDob, it) < 2 }
        .filter { day -> parentLeaveAbsences.find { day == it.date } != null }

    val combinedDays = (forceMajeureDays + parentLeaveDays).distinct().take(monthlyOperationalDays)

    return listOfNotNull(
        if (combinedDays.isNotEmpty()) combinedDays to Product.FREE_OF_CHARGE else null
    )
}

internal fun getDailyDiscount(period: DateRange, totalSoFar: Int, operationalDays: List<LocalDate>): Int {
    val dayAmount = operationalDays.filter { period.includes(it) }.size
    return BigDecimal(totalSoFar).divide(BigDecimal(dayAmount), 0, RoundingMode.HALF_UP).toInt()
}

internal fun getPreviousMonthRange(): DateRange {
    val lastMonth = LocalDate.now().minusMonths(1)
    val from = lastMonth.with(TemporalAdjusters.firstDayOfMonth())
    val to = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
    return DateRange(from, to)
}

/*
 An extra invoice row is added for a child in case their invoice row sum is within 0.5€ of the monthly fee.
 These are typically used only when the child changes placement units and has for accounting reasons their monthly fee
 split into two invoice rows with daily prices. Daily prices are always rounded to whole cents so rounding mismatch
 is inevitable.

 A difference of 0.2€ is chosen because it's a bit over the maximum rounding error, which is 0.005€ * 31 (max amount of days in a month)
 */
internal fun applyRoundingRows(invoiceRows: List<InvoiceRow>, feeDecisions: List<FeeDecision>, invoicePeriod: DateRange): List<InvoiceRow> {
    return invoiceRows
        .groupBy { it.child }
        .flatMap { (child, rows) ->
            val uniqueChildFees = feeDecisions
                .flatMap { it.children }
                .filter { it.child.id == child.id }
                .map { it.finalFee }
                .distinct()

            val invoiceRowSum = rows.sumOf { it.price }

            val roundingRow = if (uniqueChildFees.size == 1) {
                val difference = uniqueChildFees.first() - invoiceRowSum

                if (difference != 0 && -20 < difference && difference < 20) {
                    rows.first().copy(
                        id = InvoiceRowId(UUID.randomUUID()),
                        periodStart = invoicePeriod.start,
                        periodEnd = invoicePeriod.end!!,
                        amount = 1,
                        unitPrice = difference
                    )
                } else null
            } else null

            if (roundingRow != null) rows + roundingRow else rows
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
    var careType: AbsenceCareType,
    val absenceType: AbsenceType
)

// PLANNED_ABSENCE is used to indicate when a child is not even supposed to be present, it's not an actual absence
private val absenceTypesWithNoEffectOnInvoices = arrayOf(AbsenceType.PLANNED_ABSENCE)

fun Database.Read.getAbsenceStubs(spanningRange: DateRange, careTypes: List<AbsenceCareType>): List<AbsenceStub> {
    val sql =
        """
        SELECT child_id, date, care_type, absence_type
        FROM absence
        WHERE between_start_and_end(:range, date)
        AND NOT absence_type = ANY(:absenceTypes)
        AND care_type = ANY(:careTypes)
        """

    return createQuery(sql)
        .bind("range", spanningRange)
        .bind("absenceTypes", absenceTypesWithNoEffectOnInvoices)
        .bind("careTypes", careTypes.toTypedArray())
        .mapTo<AbsenceStub>()
        .toList()
}

sealed class PlacementStub(open val unit: DaycareId) {
    data class Temporary(override val unit: DaycareId, val partDay: Boolean) : PlacementStub(unit)
    data class Permanent(override val unit: DaycareId) : PlacementStub(unit)
}

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
                            val placement = when (placementType) {
                                PlacementType.TEMPORARY_DAYCARE ->
                                    PlacementStub.Temporary(placementUnit, partDay = false)
                                PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
                                    PlacementStub.Temporary(placementUnit, partDay = true)
                                else -> PlacementStub.Permanent(placementUnit)
                            }
                            Triple(placementPeriod, child, placement)
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
        SELECT daycare.id, daycare.cost_center, area.id AS area_id, area.sub_cost_center
        FROM daycare INNER JOIN care_area AS area ON daycare.care_area_id = area.id
    """
    return createQuery(sql)
        .map { row -> row.mapColumn<DaycareId>("id") to row.mapRow<DaycareCodes>() }
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
