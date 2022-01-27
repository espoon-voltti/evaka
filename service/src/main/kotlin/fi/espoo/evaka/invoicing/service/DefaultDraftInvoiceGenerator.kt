package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.invoiceRowTotal
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.orMax
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Primary
@Component
class DefaultDraftInvoiceGenerator(private val productProvider: InvoiceProductProvider) : DraftInvoiceGenerator {
    override fun generateDraftInvoices(decisions: Map<PersonId, List<FeeDecision>>, placements: Map<PersonId, List<Placements>>, period: DateRange, daycareCodes: Map<DaycareId, DaycareCodes>, operationalDays: OperationalDays, feeThresholds: FeeThresholds, absences: List<AbsenceStub>, freeChildren: List<ChildId>, codebtors: Map<PersonId, PersonId?>): List<Invoice> {
        return placements.keys.mapNotNull { headOfFamilyId ->
            try {
                generateDraftInvoice(
                    decisions[headOfFamilyId] ?: listOf(),
                    placements[headOfFamilyId] ?: listOf(),
                    period,
                    daycareCodes,
                    operationalDays,
                    feeThresholds,
                    absences,
                    freeChildren,
                    codebtors
                )
            } catch (e: Exception) {
                error("Failed to generate invoice for head of family $headOfFamilyId: $e")
            }
        }
    }

    private data class InvoiceRowStub(
        val child: ChildWithDateOfBirth,
        val placement: PlacementStub,
        val priceBeforeFeeAlterations: Int,
        val feeAlterations: List<Pair<FeeAlteration.Type, Int>>
    )

    private fun generateDraftInvoice(
        decisions: List<FeeDecision>,
        placements: List<Placements>,
        invoicePeriod: DateRange,
        daycareCodes: Map<DaycareId, DaycareCodes>,
        operationalDays: OperationalDays,
        feeThresholds: FeeThresholds,
        absences: List<AbsenceStub>,
        freeChildren: List<ChildId>,
        codebtors: Map<PersonId, PersonId?>
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
                    when (placement.type) {
                        PlacementType.TEMPORARY_DAYCARE,
                        PlacementType.TEMPORARY_DAYCARE_PART_DAY -> {
                            val partDay = placement.type == PlacementType.TEMPORARY_DAYCARE_PART_DAY
                            listOf(
                                relevantPeriod to InvoiceRowStub(
                                    ChildWithDateOfBirth(child.id, child.dateOfBirth),
                                    placement,
                                    feeThresholds.calculatePriceForTemporary(partDay, index + 1),
                                    listOf()
                                )
                            )
                        }
                        else ->
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
                                        PlacementStub(part.placement.unitId, part.placement.type),
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

    private fun toInvoiceRows(
        period: DateRange,
        processedPeriods: List<DateRange>,
        invoiceRowStub: InvoiceRowStub,
        codes: DaycareCodes,
        operationalDays: OperationalDays,
        absences: List<AbsenceStub>
    ): List<InvoiceRow> {
        val (child, placement, price, feeAlterations) = invoiceRowStub

        return when (placement.type) {
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY -> toTemporaryPlacementInvoiceRows(
                period,
                child,
                placement,
                price,
                codes,
                operationalDays.forUnit(placement.unit),
                absences
            )
            else -> toPermanentPlacementInvoiceRows(
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
        }
    }

    private fun calculateDailyPriceForInvoiceRow(price: Int, operationalDays: Int): Int {
        return BigDecimal(price).divide(BigDecimal(operationalDays), 0, RoundingMode.HALF_UP).toInt()
    }

    private fun toTemporaryPlacementInvoiceRows(
        period: DateRange,
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
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
                unitId = codes.unitId,
                product = productProvider.mapToProduct(placement.type)
            )
        )
    }

    private fun toPermanentPlacementInvoiceRows(
        period: DateRange,
        processedPeriods: List<DateRange>,
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
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

        val product = productProvider.mapToProduct(placement.type)
        val (amount, unitPrice) = if (relevantDays.size == operationalDays.generalCase.size) Pair(
            1,
            { p: Int -> p }
        ) else Pair(
            relevantDays.size,
            { p: Int -> calculateDailyPriceForInvoiceRow(p, operationalDays.generalCase.size) }
        )

        val initialRows = listOf(
            InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = child,
                periodStart = period.start,
                periodEnd = period.end!!,
                amount = amount,
                unitPrice = unitPrice(price),
                unitId = codes.unitId,
                product = product
            )
        ) + feeAlterations.map { (feeAlterationType, feeAlterationEffect) ->
            InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                periodStart = period.start,
                periodEnd = period.end,
                child = child,
                product = productProvider.mapToFeeAlterationProduct(product, feeAlterationType),
                unitId = codes.unitId,
                amount = amount,
                unitPrice = unitPrice(feeAlterationEffect)
            )
        }

        val withDailyModifiers = initialRows + dailyAbsenceRefund(
            initialRows,
            period,
            child,
            placement,
            absences,
            operationalDays
        ) { refundProduct, refundAmount, refundUnitPrice ->
            InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = child,
                periodStart = period.start,
                periodEnd = period.end,
                amount = refundAmount,
                unitPrice = refundUnitPrice,
                unitId = codes.unitId,
                product = refundProduct
            )
        }
        return withDailyModifiers + monthlyAbsenceDicount(
            withDailyModifiers,
            placement,
            absences,
            operationalDays
        ) { absenceProduct, absenceDiscount ->
            InvoiceRow(
                id = InvoiceRowId(UUID.randomUUID()),
                child = child,
                periodStart = period.start,
                periodEnd = period.end,
                product = absenceProduct,
                unitId = codes.unitId,
                amount = amount,
                unitPrice = BigDecimal(absenceDiscount).divide(BigDecimal(amount), 0, RoundingMode.HALF_UP).toInt()
            )
        }
    }

    private fun dailyAbsenceRefund(
        rows: List<InvoiceRow>,
        period: DateRange,
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
        absences: List<AbsenceStub>,
        operationalDays: OperationalDays,
        toInvoiceRow: (ProductKey, Int, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val refundedDates = getRefundedDays(period, child, placement, absences, operationalDays)
        if (refundedDates.isEmpty()) return listOf()

        val (amount, unitPrice) =
            if (refundedDates.size == operationalDays.generalCase.size) 1 to -total
            else refundedDates.size to -getDailyDiscount(period, total, operationalDays.generalCase)

        return listOf(toInvoiceRow(productProvider.dailyRefund, amount, unitPrice))
    }

    private fun getRefundedDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
        absences: List<AbsenceStub>,
        operationalDays: OperationalDays
    ): List<LocalDate> {
        val relevantOperationalDays = operationalDays.forUnit(placement.unit).filter { period.includes(it) }

        val forceMajeureAbsences = absences.filter { it.absenceType == AbsenceType.FORCE_MAJEURE }
        val forceMajeureDays =
            relevantOperationalDays.filter { day -> forceMajeureAbsences.find { day == it.date } != null }

        val parentLeaveAbsences = absences.filter { it.absenceType == AbsenceType.PARENTLEAVE }
        val parentLeaveDays = relevantOperationalDays
            .filter { ChronoUnit.YEARS.between(child.dateOfBirth, it) < 2 }
            .filter { day -> parentLeaveAbsences.find { day == it.date } != null }

        return (forceMajeureDays + parentLeaveDays).distinct().take(operationalDays.generalCase.size)
    }

    private fun getDailyDiscount(period: DateRange, totalSoFar: Int, operationalDays: List<LocalDate>): Int {
        val dayAmount = operationalDays.filter { period.includes(it) }.size
        return BigDecimal(totalSoFar).divide(BigDecimal(dayAmount), 0, RoundingMode.HALF_UP).toInt()
    }

    private fun monthlyAbsenceDicount(
        rows: List<InvoiceRow>,
        placement: PlacementStub,
        absences: List<AbsenceStub>,
        operationalDays: OperationalDays,
        toInvoiceRow: (ProductKey, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val relevantAbsences = absences.filter { operationalDays.forUnit(placement.unit).contains(it.date) }
        val sickAbsences = relevantAbsences.filter { it.absenceType == AbsenceType.SICKLEAVE }.map { it.date }
        val allAbsences = relevantAbsences.map { it.date }

        val halfPrice = { price: Int ->
            BigDecimal(price).divide(BigDecimal(2), 0, RoundingMode.HALF_UP).toInt()
        }
        val (product, totalDiscount) = when {
            sickAbsences.size >= operationalDays.generalCase.size -> productProvider.fullMonthSickLeave to -total
            sickAbsences.size >= 11 -> productProvider.partMonthSickLeave to -halfPrice(total)
            allAbsences.size >= operationalDays.generalCase.size -> productProvider.fullMonthAbsence to -halfPrice(total)
            else -> return listOf()
        }

        return listOf(toInvoiceRow(product, totalDiscount))
    }

    /*
     An extra invoice row is added for a child in case their invoice row sum is within 0.5€ of the monthly fee.
     These are typically used only when the child changes placement units and has for accounting reasons their monthly fee
     split into two invoice rows with daily prices. Daily prices are always rounded to whole cents so rounding mismatch
     is inevitable.

     A difference of 0.2€ is chosen because it's a bit over the maximum rounding error, which is 0.005€ * 31 (max amount of days in a month)
     */
    private fun applyRoundingRows(
        invoiceRows: List<InvoiceRow>,
        feeDecisions: List<FeeDecision>,
        invoicePeriod: DateRange
    ): List<InvoiceRow> {
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
}
