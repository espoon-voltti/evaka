// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.invoicing.data.isElementaryFamily
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
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.orMax
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.ceil

@Component
class NewDraftInvoiceGenerator(private val productProvider: InvoiceProductProvider, private val featureConfig: FeatureConfig) : DraftInvoiceGenerator {
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
        val feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
        val contractDaysPerMonth: Int?,
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
                                    listOf(),
                                    null,
                                )
                            )
                        }
                        else ->
                            periodDecisions
                                .mapNotNull { decision ->
                                    decision.children.find { part -> part.child == child }
                                        ?.let { DateRange(decision.validFrom, decision.validTo) to it }
                                }
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
                                        },
                                        part.serviceNeed.contractDaysPerMonth,
                                    )
                                }
                    }
                }
                .flatten()
        }

        val rows = rowStubs
            .groupBy { (_, stub) -> stub.child }
            .flatMap { (_, childStubs) ->
                val separatePeriods = mergePeriods(childStubs)
                val contractDaysPerMonth = separatePeriods.first().second.contractDaysPerMonth

                val dailyFeeDivisor = contractDaysPerMonth
                    ?: featureConfig.dailyFeeDivisorOperationalDaysOverride
                    ?: operationalDays.generalCase.size

                val attendanceDates = operationalDays.fullMonth
                    .fold<LocalDate, List<List<LocalDate>>>(listOf()) { weeks, date ->
                        if (weeks.isEmpty() || date.dayOfWeek == DayOfWeek.MONDAY) weeks.plusElement(listOf(date))
                        else weeks.dropLast(1).plusElement(weeks.last() + date)
                    }
                    .flatMap { week ->
                        val weeklyMaximumOfOperationalDates = if (contractDaysPerMonth != null) {
                            5 * ceil(operationalDays.generalCase.size / contractDaysPerMonth.toDouble()).toInt()
                        } else 5

                        week
                            .filter { date ->
                                separatePeriods.any { (period, feeData) ->
                                    period.includes(date) &&
                                        operationalDays.forUnit(feeData.placement.unit).contains(date)
                                }
                            }
                            .take(weeklyMaximumOfOperationalDates)
                    }
                    .take(dailyFeeDivisor)

                separatePeriods.fold(listOf<InvoiceRow>()) { existingRows, (period, rowStub) ->
                    val placementUnit = rowStub.placement.unit
                    val codes = daycareCodes[placementUnit]
                        ?: error("Couldn't find invoice codes for daycare ($placementUnit)")
                    existingRows + toInvoiceRows(
                        period,
                        rowStub,
                        codes,
                        dailyFeeDivisor,
                        attendanceDates,
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
        invoiceRowStub: InvoiceRowStub,
        codes: DaycareCodes,
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
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
                attendanceDates,
                absences
            )
            else -> toPermanentPlacementInvoiceRows(
                period,
                child,
                placement,
                price,
                codes,
                dailyFeeDivisor,
                attendanceDates,
                feeAlterations,
                absences
            )
        }
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

    private fun calculateDailyPriceForInvoiceRow(price: Int, dailyFeeDivisor: Int): Int =
        BigDecimal(price).divide(BigDecimal(dailyFeeDivisor), 0, RoundingMode.HALF_UP).toInt()

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
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
        price: Int,
        codes: DaycareCodes,
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
        feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
        absences: List<AbsenceStub>
    ): List<InvoiceRow> {
        val periodOperationalDates = attendanceDates.filter { period.includes(it) }
        if (periodOperationalDates.isEmpty()) return listOf()

        val product = productProvider.mapToProduct(placement.type)
        val (amount, unitPrice) = if (periodOperationalDates.size == dailyFeeDivisor) Pair(
            1,
            { p: Int -> p }
        ) else Pair(
            periodOperationalDates.size,
            { p: Int -> calculateDailyPriceForInvoiceRow(p, dailyFeeDivisor) }
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
            absences,
            periodOperationalDates,
            attendanceDates
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
        return withDailyModifiers + monthlyAbsenceDiscount(
            withDailyModifiers,
            absences,
            attendanceDates
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
        absences: List<AbsenceStub>,
        periodOperationalDates: List<LocalDate>,
        attendanceDates: List<LocalDate>,
        toInvoiceRow: (ProductKey, Int, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val invoicePeriodTotalDateCount = attendanceDates.size

        val refundedDayCount = getRefundedDays(period, child, absences, periodOperationalDates)
        if (refundedDayCount == 0) return listOf()

        val (amount, unitPrice) =
            if (refundedDayCount >= invoicePeriodTotalDateCount) 1 to -total
            else refundedDayCount to -calculateDailyPriceForInvoiceRow(total, periodOperationalDates.size)

        return listOf(toInvoiceRow(productProvider.dailyRefund, amount, unitPrice))
    }

    private fun getRefundedDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        absences: List<AbsenceStub>,
        periodOperationalDates: List<LocalDate>
    ): Int {
        val forceMajeureAbsences = absences.filter { it.absenceType == AbsenceType.FORCE_MAJEURE }
        val forceMajeureDays = forceMajeureAbsences.filter { period.includes(it.date) }

        val parentLeaveAbsences = absences.filter { it.absenceType == AbsenceType.PARENTLEAVE }
        val parentLeaveDays = parentLeaveAbsences
            .filter { ChronoUnit.YEARS.between(child.dateOfBirth, it.date) < 2 }
            .filter { period.includes(it.date) }

        return minOf(forceMajeureDays.size + parentLeaveDays.size, periodOperationalDates.size)
    }

    private fun monthlyAbsenceDiscount(
        rows: List<InvoiceRow>,
        absences: List<AbsenceStub>,
        attendanceDates: List<LocalDate>,
        toInvoiceRow: (ProductKey, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val sickAbsences = absences.filter { it.absenceType == AbsenceType.SICKLEAVE }.map { it.date }
        val allAbsences = absences.map { it.date }

        val halfPrice = { price: Int ->
            BigDecimal(price).divide(BigDecimal(2), 0, RoundingMode.HALF_UP).toInt()
        }
        val (product, totalDiscount) = when {
            sickAbsences.size >= attendanceDates.size -> productProvider.fullMonthSickLeave to -total
            sickAbsences.size >= 11 -> productProvider.partMonthSickLeave to -halfPrice(total)
            allAbsences.size >= attendanceDates.size -> productProvider.fullMonthAbsence to -halfPrice(total)
            else -> return listOf()
        }

        return listOf(toInvoiceRow(product, totalDiscount))
    }

    private fun getPreviousMonthRange(): DateRange {
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
