// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
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
import java.util.UUID

@Component
class DraftInvoiceGenerator(
    private val productProvider: InvoiceProductProvider,
    private val featureConfig: FeatureConfig
) {
    fun generateDraftInvoices(
        decisions: Map<PersonId, List<FeeDecision>>,
        placements: Map<PersonId, List<Placements>>,
        period: DateRange,
        daycareCodes: Map<DaycareId, DaycareCodes>,
        operationalDays: OperationalDays,
        feeThresholds: FeeThresholds,
        absences: List<AbsenceStub>,
        plannedAbsences: Map<ChildId, Set<LocalDate>>,
        freeChildren: List<ChildId>,
        codebtors: Map<PersonId, PersonId?>
    ): List<Invoice> {
        val absencesByChild = absences.groupBy { absence -> absence.childId }
        return placements.keys.mapNotNull { headOfFamilyId ->
            try {
                generateDraftInvoice(
                    decisions[headOfFamilyId] ?: listOf(),
                    placements[headOfFamilyId] ?: listOf(),
                    period,
                    daycareCodes,
                    operationalDays,
                    feeThresholds,
                    absencesByChild,
                    plannedAbsences,
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
        val finalPrice: Int,
        val contractDaysPerMonth: Int?,
    )

    private enum class FullMonthAbsenceType {
        SICK_LEAVE_FULL_MONTH,
        ABSENCE_FULL_MONTH,
        SICK_LEAVE_11,
        NOTHING
    }

    private fun generateDraftInvoice(
        decisions: List<FeeDecision>,
        placements: List<Placements>,
        invoicePeriod: DateRange,
        daycareCodes: Map<DaycareId, DaycareCodes>,
        operationalDays: OperationalDays,
        feeThresholds: FeeThresholds,
        absences: Map<ChildId, List<AbsenceStub>>,
        plannedAbsences: Map<ChildId, Set<LocalDate>>,
        freeChildren: List<ChildId>,
        codebtors: Map<PersonId, PersonId?>
    ): Invoice? {
        val headOfFamily = placements.first().headOfFamily
        val childrenPartialMonth = getPartialMonthChildren(placements, decisions, operationalDays)
        val childrenFullMonthAbsences =
            getFullMonthAbsences(placements, decisions, operationalDays, absences, plannedAbsences)

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
                            val fee = feeThresholds.calculatePriceForTemporary(partDay, index + 1)
                            listOf(
                                relevantPeriod to InvoiceRowStub(
                                    ChildWithDateOfBirth(child.id, child.dateOfBirth),
                                    placement,
                                    fee,
                                    listOf(),
                                    fee,
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
                                        part.finalFee,
                                        part.serviceNeed.contractDaysPerMonth,
                                    )
                                }
                    }
                }
                .flatten()
        }

        val rows = rowStubs
            .groupBy { (_, stub) -> stub.child }
            .flatMap { (child, childStubs) ->
                val separatePeriods = mergePeriods(childStubs)
                val contractDaysPerMonth = separatePeriods.first().second.contractDaysPerMonth
                val childPlannedAbsences = plannedAbsences[child.id] ?: setOf()

                val dailyFeeDivisor = contractDaysPerMonth
                    ?: featureConfig.dailyFeeDivisorOperationalDaysOverride
                    ?: operationalDays.generalCase.size

                val attendanceDates = getAttendanceDates(
                    child,
                    operationalDays,
                    separatePeriods,
                    contractDaysPerMonth,
                    childPlannedAbsences,
                    childrenPartialMonth
                )

                val relevantAbsences = (absences[child.id] ?: listOf()).filter { absence ->
                    isUnitOperationalDay(
                        operationalDays,
                        separatePeriods,
                        absence.date
                    )
                }

                separatePeriods
                    .filter { (_, rowStub) -> rowStub.finalPrice != 0 }
                    .flatMap { (period, rowStub) ->
                        val childId = rowStub.child.id
                        val placementUnit = rowStub.placement.unit
                        val codes = daycareCodes[placementUnit]
                            ?: error("Couldn't find invoice codes for daycare ($placementUnit)")
                        toInvoiceRows(
                            period,
                            rowStub,
                            codes,
                            dailyFeeDivisor,
                            attendanceDates,
                            relevantAbsences,
                            childrenFullMonthAbsences[childId] ?: FullMonthAbsenceType.NOTHING,
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

    private fun getPartialMonthChildren(
        placementsList: List<Placements>,
        decisions: List<FeeDecision>,
        operationalDays: OperationalDays,
    ): Set<ChildId> {
        return getOperationalDaysForChildsUnits(placementsList, operationalDays)
            .mapValues { (childId, childOperationalDays) ->
                childOperationalDays.any { date -> !childHasFeeDecision(decisions, childId, date) }
            }
            .filter { (_, partialMonth) -> partialMonth }
            .map { (childId, _) -> childId }
            .toSet()
    }

    private fun getFullMonthAbsences(
        placementsList: List<Placements>,
        decisions: List<FeeDecision>,
        operationalDays: OperationalDays,
        absences: Map<ChildId, List<AbsenceStub>>,
        plannedAbsences: Map<ChildId, Set<LocalDate>>
    ): Map<ChildId, FullMonthAbsenceType> {
        fun hasPlannedAbsence(childId: ChildId, date: LocalDate): Boolean {
            return plannedAbsences[childId]?.contains(date) ?: false
        }

        fun hasAbsence(childId: ChildId, date: LocalDate): Boolean {
            return absences[childId]?.any { absence -> absence.date == date } ?: false
        }

        fun hasSickleave(childId: ChildId, date: LocalDate): Boolean {
            return absences[childId]?.any { absence -> absence.date == date && absence.absenceType == AbsenceType.SICKLEAVE }
                ?: false
        }

        fun hasAnyAbsence(childId: ChildId, date: LocalDate): Boolean {
            return hasPlannedAbsence(childId, date) || hasAbsence(childId, date)
        }

        return getOperationalDaysForChildsUnits(placementsList, operationalDays)
            .mapValues { (childId, allOperationalDays) ->
                allOperationalDays.filter { date -> childHasFeeDecision(decisions, childId, date) }
            }
            .mapValues { (childId, childOperationalDays) ->
                if (childOperationalDays.all { date -> hasSickleave(childId, date) }) {
                    FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH
                } else if (childOperationalDays.filter { date -> hasSickleave(childId, date) }.size >= 11) {
                    FullMonthAbsenceType.SICK_LEAVE_11
                } else if (childOperationalDays.all { date -> hasAnyAbsence(childId, date) }) {
                    FullMonthAbsenceType.ABSENCE_FULL_MONTH
                } else {
                    FullMonthAbsenceType.NOTHING
                }
            }
    }

    private fun childHasFeeDecision(decisions: List<FeeDecision>, childId: ChildId, date: LocalDate): Boolean {
        return decisions.any { decision -> decision.validDuring.includes(date) && decision.children.any { part -> part.child.id == childId } }
    }

    private fun getOperationalDaysForChildsUnits(
        placementsList: List<Placements>,
        operationalDays: OperationalDays,
    ): Map<ChildId, Set<LocalDate>> {
        return placementsList.flatMap { p ->
            p.placements.map { (child, placement) ->
                val unitOperationalDays = operationalDays.forUnit(placement.unit)
                child.id to unitOperationalDays
            }
        }
            .groupBy { (childId, _) -> childId }
            .mapValues { (_, operationalDaysList) ->
                operationalDaysList.flatMap { (_, operationalDays) -> operationalDays }.toSet()
            }
    }

    private fun getAttendanceDates(
        child: ChildWithDateOfBirth,
        operationalDays: OperationalDays,
        separatePeriods: List<Pair<DateRange, InvoiceRowStub>>,
        contractDaysPerMonth: Int?,
        childPlannedAbsences: Set<LocalDate>,
        partialMonthChildren: Set<ChildId>,
    ): List<LocalDate> {
        val attendanceDates = operationalDatesByWeek(operationalDays, separatePeriods)
            .flatMap { weekOperationalDates ->
                if (contractDaysPerMonth != null) {
                    // Use real attendance dates (with no planned absences) for contract day children
                    weekOperationalDates.filterNot { date -> childPlannedAbsences.contains(date) }
                } else {
                    // Take at most 5 days per week (for round-the-clock units)
                    weekOperationalDates.take(5)
                }
            }

        // If this is a full month for a contract day child (not int partialMonthChildren), make sure that there's
        // no less than `contractDaysPerMonth` days even if they have more planned absences than they should
        return if (contractDaysPerMonth != null && !partialMonthChildren.contains(child.id) && attendanceDates.size < contractDaysPerMonth) {
            (
                attendanceDates + operationalDays.fullMonth.filter { date ->
                    isUnitOperationalDay(
                        operationalDays,
                        separatePeriods,
                        date
                    ) && !attendanceDates.contains(date)
                }.take(contractDaysPerMonth - attendanceDates.size)
                ).sorted()
        } else {
            attendanceDates
        }
    }

    private fun operationalDatesByWeek(
        operationalDays: OperationalDays,
        separatePeriods: List<Pair<DateRange, InvoiceRowStub>>
    ): List<List<LocalDate>> {
        return operationalDays.fullMonth.fold(listOf<List<LocalDate>>()) { weeks, date ->
            if (weeks.isEmpty() || date.dayOfWeek == DayOfWeek.MONDAY) weeks.plusElement(listOf(date))
            else weeks.dropLast(1).plusElement(weeks.last() + date)
        }.map { week -> week.filter { date -> isUnitOperationalDay(operationalDays, separatePeriods, date) } }
    }

    private fun isUnitOperationalDay(
        operationalDays: OperationalDays,
        separatePeriods: List<Pair<DateRange, InvoiceRowStub>>,
        date: LocalDate
    ): Boolean {
        return separatePeriods.any { (period, feeData) ->
            period.includes(date) &&
                operationalDays.forUnit(feeData.placement.unit).contains(date)
        }
    }

    private fun toInvoiceRows(
        period: DateRange,
        invoiceRowStub: InvoiceRowStub,
        codes: DaycareCodes,
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>,
        fullMonthAbsenceType: FullMonthAbsenceType,
    ): List<InvoiceRow> {
        return when (invoiceRowStub.placement.type) {
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY -> toTemporaryPlacementInvoiceRows(
                period,
                invoiceRowStub.child,
                invoiceRowStub.placement,
                invoiceRowStub.priceBeforeFeeAlterations,
                codes,
                dailyFeeDivisor,
                attendanceDates,
                absences
            )
            else -> toPermanentPlacementInvoiceRows(
                period,
                invoiceRowStub.child,
                invoiceRowStub.placement,
                invoiceRowStub.priceBeforeFeeAlterations,
                codes,
                dailyFeeDivisor,
                invoiceRowStub.contractDaysPerMonth,
                attendanceDates,
                invoiceRowStub.feeAlterations,
                absences,
                fullMonthAbsenceType
            )
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
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>
    ): List<InvoiceRow> {
        val amount = attendanceDates
            .take(dailyFeeDivisor)
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
        contractDaysPerMonth: Int?,
        attendanceDates: List<LocalDate>,
        feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
        absences: List<AbsenceStub>,
        fullMonthAbsenceType: FullMonthAbsenceType,
    ): List<InvoiceRow> {
        // Make sure the number of operational days in a month doesn't exceed `dailyFeeDivisor`.
        //
        // Example: A child has a placement to a round-the-clock unit for the first half and to a
        // normal unit for the second half of the month. The round-the-clock unit has more operational
        // days, so we have to make sure that we don't invoice more than `dailyFeeDivisor` days.
        //
        val periodAttendanceDates = attendanceDates.take(dailyFeeDivisor).filter { period.includes(it) }
        if (periodAttendanceDates.isEmpty()) return listOf()

        val product = productProvider.mapToProduct(placement.type)
        val (amount, unitPrice) = if (periodAttendanceDates.size == dailyFeeDivisor) Pair(
            1,
            { p: Int -> p }
        ) else Pair(
            periodAttendanceDates.size,
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
        } + surplusContractDays(
            period,
            child,
            product,
            price,
            codes,
            feeAlterations,
            dailyFeeDivisor,
            contractDaysPerMonth,
            attendanceDates,
            absences
        )

        val withDailyModifiers = initialRows + dailyAbsenceRefund(
            period,
            initialRows,
            child,
            absences,
            periodAttendanceDates,
            dailyFeeDivisor,
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
            fullMonthAbsenceType
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

    private fun surplusContractDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        product: ProductKey,
        price: Int,
        codes: DaycareCodes,
        feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
        dailyFeeDivisor: Int,
        contractDaysPerMonth: Int?,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>
    ): List<InvoiceRow> {
        if (contractDaysPerMonth == null) return listOf()

        fun hasAbsence(date: LocalDate) = absences.any { it.date == date }

        val attendancesBeforePeriod = attendanceDates.filter { it < period.start && !hasAbsence(it) }.size
        val attendancesInPeriod = attendanceDates.filter { period.includes(it) && !hasAbsence(it) }.size
        val surplusAttendanceDays = attendancesBeforePeriod + attendancesInPeriod - contractDaysPerMonth

        return if (surplusAttendanceDays > 0) {
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    periodStart = period.start,
                    periodEnd = period.end!!,
                    child = child,
                    product = product,
                    unitId = codes.unitId,
                    amount = surplusAttendanceDays,
                    unitPrice = calculateDailyPriceForInvoiceRow(price, dailyFeeDivisor),
                )
            ) + feeAlterations.map { (feeAlterationType, feeAlterationEffect) ->
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    periodStart = period.start,
                    periodEnd = period.end,
                    child = child,
                    product = productProvider.mapToFeeAlterationProduct(product, feeAlterationType),
                    unitId = codes.unitId,
                    amount = surplusAttendanceDays,
                    unitPrice = calculateDailyPriceForInvoiceRow(feeAlterationEffect, dailyFeeDivisor)
                )
            }
        } else listOf()
    }

    private fun dailyAbsenceRefund(
        period: DateRange,
        rows: List<InvoiceRow>,
        child: ChildWithDateOfBirth,
        absences: List<AbsenceStub>,
        periodAttendanceDates: List<LocalDate>,
        dailyFeeDivisor: Int,
        toInvoiceRow: (ProductKey, Int, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        assert(periodAttendanceDates.size <= dailyFeeDivisor)

        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val refundedDayCount = getRefundedDays(period, child, absences, dailyFeeDivisor)
        if (refundedDayCount == 0) return listOf()

        val (amount, unitPrice) =
            if (refundedDayCount >= dailyFeeDivisor) 1 to -total
            else refundedDayCount to -calculateDailyPriceForInvoiceRow(total, periodAttendanceDates.size)

        return listOf(toInvoiceRow(productProvider.dailyRefund, amount, unitPrice))
    }

    private fun getRefundedDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        absences: List<AbsenceStub>,
        dailyFeeDivisor: Int,
    ): Int {
        val forceMajeureAbsences = absences.filter { it.absenceType == AbsenceType.FORCE_MAJEURE }
        val forceMajeureDays = forceMajeureAbsences.filter { period.includes(it.date) }

        val parentLeaveAbsences = absences.filter { it.absenceType == AbsenceType.PARENTLEAVE }
        val parentLeaveDays = parentLeaveAbsences
            .filter { ChronoUnit.YEARS.between(child.dateOfBirth, it.date) < 2 }
            .filter { period.includes(it.date) }

        return minOf(forceMajeureDays.size + parentLeaveDays.size, dailyFeeDivisor)
    }

    private fun monthlyAbsenceDiscount(
        rows: List<InvoiceRow>,
        fullMonthAbsenceType: FullMonthAbsenceType,
        toInvoiceRow: (ProductKey, Int) -> InvoiceRow
    ): List<InvoiceRow> {
        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val halfPrice = { price: Int ->
            BigDecimal(price).divide(BigDecimal(2), 0, RoundingMode.HALF_UP).toInt()
        }

        val (product, totalDiscount) = when (fullMonthAbsenceType) {
            FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH -> productProvider.fullMonthSickLeave to -total
            FullMonthAbsenceType.SICK_LEAVE_11 -> productProvider.partMonthSickLeave to -halfPrice(total)
            FullMonthAbsenceType.ABSENCE_FULL_MONTH -> productProvider.fullMonthAbsence to -halfPrice(total)
            FullMonthAbsenceType.NOTHING -> return listOf()
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
