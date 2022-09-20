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
import fi.espoo.evaka.invoicing.domain.calculateMaxFee
import fi.espoo.evaka.invoicing.domain.feeAlterationEffect
import fi.espoo.evaka.invoicing.domain.invoiceRowTotal
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.springframework.stereotype.Component

enum class InvoiceGenerationLogic {
    Default,
    Free,
}

interface InvoiceGenerationLogicChooser {
    fun logicForMonth(
        tx: Database.Read,
        year: Int,
        month: Month,
        childId: ChildId
    ): InvoiceGenerationLogic
}

object DefaultInvoiceGenerationLogic : InvoiceGenerationLogicChooser {
    override fun logicForMonth(
        tx: Database.Read,
        year: Int,
        month: Month,
        childId: ChildId
    ): InvoiceGenerationLogic = InvoiceGenerationLogic.Default
}

@Component
class DraftInvoiceGenerator(
    private val productProvider: InvoiceProductProvider,
    private val featureConfig: FeatureConfig,
    private val invoiceGenerationLogicChooser: InvoiceGenerationLogicChooser
) {
    fun generateDraftInvoices(
        tx: Database.Read,
        decisions: Map<PersonId, List<FeeDecision>>,
        placements: Map<PersonId, List<Placements>>,
        period: DateRange,
        daycareCodes: Map<DaycareId, AreaId>,
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
                    tx,
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
        val maxPrice: Int,
        val contractDaysPerMonth: Int?,
    )

    private enum class FullMonthAbsenceType {
        SICK_LEAVE_FULL_MONTH,
        ABSENCE_FULL_MONTH,
        SICK_LEAVE_11,
        NOTHING
    }

    private fun generateDraftInvoice(
        tx: Database.Read,
        decisions: List<FeeDecision>,
        placements: List<Placements>,
        invoicePeriod: DateRange,
        areaIds: Map<DaycareId, AreaId>,
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

        val rowStubs =
            placements.flatMap { (placementsPeriod, _, childPlacementPairs) ->
                val relevantPeriod =
                    DateRange(
                        maxOf(invoicePeriod.start, placementsPeriod.start),
                        minOf(orMax(invoicePeriod.end), orMax(placementsPeriod.end))
                    )
                val periodDecisions =
                    decisions.filter {
                        placementsPeriod.overlaps(DateRange(it.validFrom, it.validTo))
                    }

                childPlacementPairs
                    .sortedByDescending { (child) -> child.dateOfBirth }
                    .mapIndexed { index, (child, placement) ->
                        when (placement.type) {
                            PlacementType.TEMPORARY_DAYCARE,
                            PlacementType.TEMPORARY_DAYCARE_PART_DAY -> {
                                val partDay =
                                    placement.type == PlacementType.TEMPORARY_DAYCARE_PART_DAY
                                val fee =
                                    feeThresholds.calculatePriceForTemporary(partDay, index + 1)
                                listOf(
                                    relevantPeriod to
                                        InvoiceRowStub(
                                            ChildWithDateOfBirth(child.id, child.dateOfBirth),
                                            placement,
                                            fee,
                                            listOf(),
                                            fee,
                                            fee,
                                            null,
                                        )
                                )
                            }
                            else ->
                                periodDecisions
                                    .mapNotNull { decision ->
                                        decision.children
                                            .find { part -> part.child == child }
                                            ?.let {
                                                DateRange(decision.validFrom, decision.validTo) to
                                                    it
                                            }
                                    }
                                    .filterNot { (_, part) -> freeChildren.contains(part.child.id) }
                                    .map { (decisionPeriod, part) ->
                                        val maxFeeBeforeFeeAlterations =
                                            calculateMaxFee(part.baseFee, part.siblingDiscount)
                                        val maxFee =
                                            part.feeAlterations.fold(maxFeeBeforeFeeAlterations) {
                                                currentFee,
                                                feeAlteration ->
                                                currentFee +
                                                    feeAlterationEffect(
                                                        currentFee,
                                                        feeAlteration.type,
                                                        feeAlteration.amount,
                                                        feeAlteration.isAbsolute
                                                    )
                                            }
                                        DateRange(
                                            maxOf(relevantPeriod.start, decisionPeriod.start),
                                            minOf(
                                                orMax(relevantPeriod.end),
                                                orMax(decisionPeriod.end)
                                            )
                                        ) to
                                            InvoiceRowStub(
                                                ChildWithDateOfBirth(
                                                    part.child.id,
                                                    part.child.dateOfBirth
                                                ),
                                                PlacementStub(
                                                    part.placement.unitId,
                                                    part.placement.type
                                                ),
                                                part.fee,
                                                part.feeAlterations.map { feeAlteration ->
                                                    Pair(feeAlteration.type, feeAlteration.effect)
                                                },
                                                part.finalFee,
                                                maxFee,
                                                part.serviceNeed.contractDaysPerMonth,
                                            )
                                    }
                        }
                    }
                    .flatten()
            }

        val rows =
            rowStubs
                .groupBy { (_, stub) -> stub.child }
                .flatMap { (child, childStubs) ->
                    val separatePeriods = mergePeriods(childStubs)

                    val logic =
                        invoiceGenerationLogicChooser.logicForMonth(
                            tx,
                            invoicePeriod.start.year,
                            invoicePeriod.start.month,
                            child.id
                        )
                    if (logic == InvoiceGenerationLogic.Free) return@flatMap listOf()

                    val contractDaysPerMonth =
                        separatePeriods.first().second.contractDaysPerMonth.takeIf {
                            featureConfig.useContractDaysAsDailyFeeDivisor
                        }
                    val childPlannedAbsences = plannedAbsences[child.id] ?: setOf()

                    val dailyFeeDivisor =
                        contractDaysPerMonth
                            ?: featureConfig.dailyFeeDivisorOperationalDaysOverride
                                ?: operationalDays.generalCase.size

                    val attendanceDates =
                        getAttendanceDates(
                            child,
                            operationalDays,
                            separatePeriods,
                            contractDaysPerMonth,
                            childPlannedAbsences,
                            childrenPartialMonth
                        )

                    val relevantAbsences =
                        (absences[child.id] ?: listOf()).filter { absence ->
                            isUnitOperationalDay(operationalDays, separatePeriods, absence.date)
                        }

                    val fullMonthAbsenceType =
                        childrenFullMonthAbsences[child.id] ?: FullMonthAbsenceType.NOTHING

                    separatePeriods
                        .filter { (_, rowStub) -> rowStub.finalPrice != 0 }
                        .flatMap { (period, rowStub) ->
                            toInvoiceRows(
                                period,
                                rowStub,
                                rowStub.placement.unit,
                                dailyFeeDivisor,
                                minOf(
                                    contractDaysPerMonth ?: operationalDays.generalCase.size,
                                    dailyFeeDivisor
                                ),
                                attendanceDates,
                                relevantAbsences,
                                fullMonthAbsenceType,
                            )
                        }
                }
                .let { rows -> applyRoundingRows(rows, decisions, invoicePeriod) }
                .filter { row -> row.price != 0 }

        if (rows.isEmpty()) return null

        val areaId =
            rowStubs
                .maxByOrNull { (_, stub) -> stub.child.dateOfBirth }!!
                .let { (_, stub) ->
                    areaIds[stub.placement.unit]
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
            return absences[childId]?.any { absence ->
                absence.date == date && absence.absenceType == AbsenceType.SICKLEAVE
            }
                ?: false
        }

        return getOperationalDaysForChildsUnits(placementsList, operationalDays)
            .mapValues { (childId, allOperationalDays) ->
                allOperationalDays.filter { date -> childHasFeeDecision(decisions, childId, date) }
            }
            .mapValues { (childId, childOperationalDays) ->
                val allSickLeaves = childOperationalDays.all { date -> hasSickleave(childId, date) }
                val atLeastOneSickLeave =
                    childOperationalDays.any { date -> hasSickleave(childId, date) }
                val allSickLeavesOrPlannedAbsences =
                    childOperationalDays.all { date ->
                        hasSickleave(childId, date) || hasPlannedAbsence(childId, date)
                    }
                val atLeast11SickLeaves =
                    childOperationalDays.filter { date -> hasSickleave(childId, date) }.size >= 11
                val allAbsences = childOperationalDays.all { date -> hasAbsence(childId, date) }

                if (allSickLeaves) {
                    FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH
                } else if (
                    featureConfig.freeSickLeaveOnContractDays &&
                        atLeastOneSickLeave &&
                        allSickLeavesOrPlannedAbsences
                ) {
                    // freeSickLeaveOnContractDays: The month becomes free if it has at least one
                    // sick
                    // leave, and a sick leave or planned absence on all days
                    FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH
                } else if (atLeast11SickLeaves) {
                    FullMonthAbsenceType.SICK_LEAVE_11
                } else if (allAbsences) {
                    FullMonthAbsenceType.ABSENCE_FULL_MONTH
                } else {
                    FullMonthAbsenceType.NOTHING
                }
            }
    }

    private fun childHasFeeDecision(
        decisions: List<FeeDecision>,
        childId: ChildId,
        date: LocalDate
    ): Boolean {
        return decisions.any { decision ->
            decision.validDuring.includes(date) &&
                decision.children.any { part -> part.child.id == childId }
        }
    }

    private fun getOperationalDaysForChildsUnits(
        placementsList: List<Placements>,
        operationalDays: OperationalDays,
    ): Map<ChildId, Set<LocalDate>> {
        return placementsList
            .flatMap { p ->
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
        val attendanceDates =
            operationalDatesByWeek(operationalDays, separatePeriods).flatMap { weekOperationalDates
                ->
                if (contractDaysPerMonth != null) {
                    // Use real attendance dates (with no planned absences) for contract day
                    // children
                    weekOperationalDates.filterNot { date -> childPlannedAbsences.contains(date) }
                } else {
                    // Take at most 5 days per week (for round-the-clock units)
                    weekOperationalDates.take(5)
                }
            }

        // If this is a full month for a contract day child (not in partialMonthChildren), make sure
        // that there's
        // no less than `contractDaysPerMonth` days even if they have more planned absences than
        // they
        // should
        return if (
            contractDaysPerMonth != null &&
                !partialMonthChildren.contains(child.id) &&
                attendanceDates.size < contractDaysPerMonth
        ) {
            (attendanceDates +
                    operationalDays.fullMonth
                        .filter { date ->
                            isUnitOperationalDay(operationalDays, separatePeriods, date) &&
                                !attendanceDates.contains(date)
                        }
                        .take(contractDaysPerMonth - attendanceDates.size))
                .sorted()
        } else {
            attendanceDates
        }
    }

    private fun operationalDatesByWeek(
        operationalDays: OperationalDays,
        separatePeriods: List<Pair<DateRange, InvoiceRowStub>>
    ): List<List<LocalDate>> {
        return operationalDays.fullMonth
            .fold(listOf<List<LocalDate>>()) { weeks, date ->
                if (weeks.isEmpty() || date.dayOfWeek == DayOfWeek.MONDAY)
                    weeks.plusElement(listOf(date))
                else weeks.dropLast(1).plusElement(weeks.last() + date)
            }
            .map { week ->
                week.filter { date -> isUnitOperationalDay(operationalDays, separatePeriods, date) }
            }
    }

    private fun isUnitOperationalDay(
        operationalDays: OperationalDays,
        separatePeriods: List<Pair<DateRange, InvoiceRowStub>>,
        date: LocalDate
    ): Boolean {
        return separatePeriods.any { (period, feeData) ->
            period.includes(date) && operationalDays.forUnit(feeData.placement.unit).contains(date)
        }
    }

    private fun toInvoiceRows(
        period: DateRange,
        invoiceRowStub: InvoiceRowStub,
        unitId: DaycareId,
        dailyFeeDivisor: Int,
        numRelevantOperationalDays: Int,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>,
        fullMonthAbsenceType: FullMonthAbsenceType,
    ): List<InvoiceRow> {
        return when (invoiceRowStub.placement.type) {
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
                toTemporaryPlacementInvoiceRows(
                    period,
                    invoiceRowStub.child,
                    invoiceRowStub.placement,
                    invoiceRowStub.priceBeforeFeeAlterations,
                    unitId,
                    dailyFeeDivisor,
                    attendanceDates,
                    absences
                )
            else ->
                toPermanentPlacementInvoiceRows(
                    period,
                    invoiceRowStub.child,
                    invoiceRowStub.placement,
                    invoiceRowStub.priceBeforeFeeAlterations,
                    invoiceRowStub.finalPrice,
                    invoiceRowStub.maxPrice,
                    unitId,
                    dailyFeeDivisor,
                    invoiceRowStub.contractDaysPerMonth,
                    numRelevantOperationalDays,
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
        unitId: DaycareId,
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>
    ): List<InvoiceRow> {
        val amount =
            attendanceDates
                .take(dailyFeeDivisor)
                .filter { day -> period.includes(day) }
                .filter { day -> absences.none { absence -> absence.date == day } }
                .size

        return if (amount == 0) listOf()
        else
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    periodStart = period.start,
                    periodEnd = period.end!!,
                    child = child.id,
                    amount = amount,
                    unitPrice = price,
                    unitId = unitId,
                    product = productProvider.mapToProduct(placement.type),
                    correctionId = null
                )
            )
    }

    private fun toPermanentPlacementInvoiceRows(
        period: DateRange,
        child: ChildWithDateOfBirth,
        placement: PlacementStub,
        price: Int,
        finalPrice: Int,
        maxPrice: Int,
        unitId: DaycareId,
        dailyFeeDivisor: Int,
        contractDaysPerMonth: Int?,
        numRelevantOperationalDays: Int,
        attendanceDates: List<LocalDate>,
        feeAlterations: List<Pair<FeeAlteration.Type, Int>>,
        absences: List<AbsenceStub>,
        fullMonthAbsenceType: FullMonthAbsenceType,
    ): List<InvoiceRow> {
        // Make sure the number of operational days in a month doesn't exceed `dailyFeeDivisor`.
        //
        // Example: A child has a placement to a round-the-clock unit for the first half and to a
        // normal unit for the second half of the month. The round-the-clock unit has more
        // operational
        // days, so we have to make sure that we don't invoice more than `dailyFeeDivisor` days.
        //
        val periodAttendanceDates =
            attendanceDates.take(dailyFeeDivisor).filter { period.includes(it) }
        if (periodAttendanceDates.isEmpty()) return listOf()

        val isFullMonth = periodAttendanceDates.size == numRelevantOperationalDays

        val product = productProvider.mapToProduct(placement.type)
        val (amount, unitPrice) =
            if (isFullMonth) Pair(1, { p: Int -> p })
            else
                Pair(
                    periodAttendanceDates.size,
                    { p: Int -> calculateDailyPriceForInvoiceRow(p, dailyFeeDivisor) }
                )

        val initialRows =
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    child = child.id,
                    periodStart = period.start,
                    periodEnd = period.end!!,
                    amount = amount,
                    unitPrice = unitPrice(price),
                    unitId = unitId,
                    product = product,
                    correctionId = null
                )
            ) +
                feeAlterations.map { (feeAlterationType, feeAlterationEffect) ->
                    InvoiceRow(
                        id = InvoiceRowId(UUID.randomUUID()),
                        periodStart = period.start,
                        periodEnd = period.end,
                        child = child.id,
                        product =
                            productProvider.mapToFeeAlterationProduct(product, feeAlterationType),
                        unitId = unitId,
                        amount = amount,
                        unitPrice = unitPrice(feeAlterationEffect),
                        correctionId = null
                    )
                }

        val withDailyModifiers =
            initialRows +
                surplusContractDays(
                    period,
                    child,
                    finalPrice,
                    initialRows.sumOf { it.price },
                    maxPrice,
                    unitId,
                    contractDaysPerMonth,
                    attendanceDates,
                    absences,
                    fullMonthAbsenceType in
                        listOf(
                            FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH,
                            FullMonthAbsenceType.ABSENCE_FULL_MONTH
                        )
                ) +
                dailyAbsenceRefund(
                    period,
                    initialRows,
                    child,
                    absences,
                    periodAttendanceDates,
                    isFullMonth,
                    dailyFeeDivisor,
                ) { refundProduct, refundAmount, refundUnitPrice ->
                    InvoiceRow(
                        id = InvoiceRowId(UUID.randomUUID()),
                        child = child.id,
                        periodStart = period.start,
                        periodEnd = period.end,
                        amount = refundAmount,
                        unitPrice = refundUnitPrice,
                        unitId = unitId,
                        product = refundProduct,
                        correctionId = null
                    )
                }
        return withDailyModifiers +
            monthlyAbsenceDiscount(withDailyModifiers, fullMonthAbsenceType) {
                absenceProduct,
                absenceDiscount ->
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    child = child.id,
                    periodStart = period.start,
                    periodEnd = period.end,
                    product = absenceProduct,
                    unitId = unitId,
                    amount = amount,
                    unitPrice =
                        BigDecimal(absenceDiscount)
                            .divide(BigDecimal(amount), 0, RoundingMode.HALF_UP)
                            .toInt(),
                    correctionId = null
                )
            }
    }

    private val plannedAbsenceTypes = setOf(AbsenceType.PLANNED_ABSENCE, AbsenceType.FREE_ABSENCE)

    private fun surplusContractDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        monthlyPrice: Int,
        invoiceRowSum: Int,
        maxPrice: Int,
        unitId: DaycareId,
        contractDaysPerMonth: Int?,
        attendanceDates: List<LocalDate>,
        absences: List<AbsenceStub>,
        isAbsentFullMonth: Boolean
    ): List<InvoiceRow> {
        if (contractDaysPerMonth == null || isAbsentFullMonth) return listOf()

        fun hasAbsence(date: LocalDate) = absences.any { it.date == date }

        val attendancesBeforePeriod =
            attendanceDates.filter { it < period.start && !hasAbsence(it) }.size
        val attendancesInPeriod =
            attendanceDates.filter { period.includes(it) && !hasAbsence(it) }.size
        val unplannedAbsenceSurplusDays =
            if (featureConfig.unplannedAbsencesAreContractSurplusDays)
                absences.filter { !plannedAbsenceTypes.contains(it.absenceType) }
            else listOf()
        val (unplannedAbsencesInPeriod, unplannedAbsencesBeforePeriod) =
            unplannedAbsenceSurplusDays
                .filter { it.date < period.start || period.includes(it.date) }
                .partition { period.includes(it.date) }
        val attendanceDays =
            attendancesBeforePeriod +
                attendancesInPeriod +
                unplannedAbsencesBeforePeriod.size +
                unplannedAbsencesInPeriod.size

        return if (contractDaysPerMonth < attendanceDays) {
            val surplusAttendanceDays = attendanceDays - contractDaysPerMonth
            val surplusDailyPrice =
                calculateDailyPriceForInvoiceRow(monthlyPrice, contractDaysPerMonth)
            val totalAddition = surplusAttendanceDays * surplusDailyPrice
            val (amount, unitPrice) =
                when {
                    // surplus days increase takes invoice row sum above max price threshold
                    invoiceRowSum + totalAddition > maxPrice -> 1 to (maxPrice - invoiceRowSum)
                    // total attendances days is over the max contract day surplus threshold
                    featureConfig.maxContractDaySurplusThreshold
                        ?: Int.MAX_VALUE < attendanceDays -> 1 to (maxPrice - invoiceRowSum)
                    else -> surplusAttendanceDays to surplusDailyPrice
                }
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    periodStart = period.start,
                    periodEnd = period.end!!,
                    child = child.id,
                    product = productProvider.contractSurplusDay,
                    unitId = unitId,
                    amount = amount,
                    unitPrice = unitPrice,
                    correctionId = null
                )
            )
        } else listOf()
    }

    private fun dailyAbsenceRefund(
        period: DateRange,
        rows: List<InvoiceRow>,
        child: ChildWithDateOfBirth,
        absences: List<AbsenceStub>,
        periodAttendanceDates: List<LocalDate>,
        isFullMonth: Boolean,
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
            else
                refundedDayCount to
                    -calculateDailyPriceForInvoiceRow(
                        total,
                        if (isFullMonth) dailyFeeDivisor else periodAttendanceDates.size
                    )

        return listOf(toInvoiceRow(productProvider.dailyRefund, amount, unitPrice))
    }

    private fun getRefundedDays(
        period: DateRange,
        child: ChildWithDateOfBirth,
        absences: List<AbsenceStub>,
        dailyFeeDivisor: Int,
    ): Int {
        val forceMajeureAndFreeAbsences =
            absences
                .filter {
                    it.absenceType == AbsenceType.FORCE_MAJEURE ||
                        (featureConfig.freeAbsenceGivesADailyRefund &&
                            it.absenceType == AbsenceType.FREE_ABSENCE)
                }
                .map { it.date }
        val forceMajeureDays = forceMajeureAndFreeAbsences.filter { period.includes(it) }

        val parentLeaveAbsences =
            absences.filter { it.absenceType == AbsenceType.PARENTLEAVE }.map { it.date }
        val parentLeaveDays =
            if (parentLeaveAbsences.isNotEmpty())
                parentLeaveAbsences
                    .filter { ChronoUnit.YEARS.between(child.dateOfBirth, it) < 2 }
                    .filter { period.includes(it) }
            else listOf()

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

        val (product, totalDiscount) =
            when (fullMonthAbsenceType) {
                FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH ->
                    productProvider.fullMonthSickLeave to -total
                FullMonthAbsenceType.SICK_LEAVE_11 ->
                    productProvider.partMonthSickLeave to -halfPrice(total)
                FullMonthAbsenceType.ABSENCE_FULL_MONTH ->
                    productProvider.fullMonthAbsence to -halfPrice(total)
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
                val uniqueChildFees =
                    feeDecisions
                        .flatMap { it.children }
                        .filter { it.child.id == child }
                        .map { it.finalFee }
                        .distinct()

                val invoiceRowSum = rows.sumOf { it.price }

                val roundingRow =
                    if (uniqueChildFees.size == 1) {
                        val difference = uniqueChildFees.first() - invoiceRowSum

                        if (difference != 0 && -20 < difference && difference < 20) {
                            rows
                                .first()
                                .copy(
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
