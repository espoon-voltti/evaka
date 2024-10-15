// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.calculateMaxFee
import fi.espoo.evaka.invoicing.domain.feeAlterationEffect
import fi.espoo.evaka.invoicing.domain.invoiceRowTotal
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.InvoiceRowId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.Tracing
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.utils.memoize
import fi.espoo.evaka.shared.withSpan
import fi.espoo.evaka.shared.withValue
import io.opentelemetry.api.trace.Tracer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
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
        childId: ChildId,
    ): InvoiceGenerationLogic
}

object DefaultInvoiceGenerationLogic : InvoiceGenerationLogicChooser {
    override fun logicForMonth(
        tx: Database.Read,
        year: Int,
        month: Month,
        childId: ChildId,
    ): InvoiceGenerationLogic = InvoiceGenerationLogic.Default
}

@Component
class DraftInvoiceGenerator(
    private val productProvider: InvoiceProductProvider,
    private val featureConfig: FeatureConfig,
    private val invoiceGenerationLogicChooser: InvoiceGenerationLogicChooser,
    private val tracer: Tracer = noopTracer(),
) {
    fun generateDraftInvoices(tx: Database.Read, data: InvoiceCalculationData): List<Invoice> {
        val headsOfFamily = data.decisions.keys + data.temporaryPlacements.keys
        return headsOfFamily.mapNotNull { headOfFamilyId ->
            try {
                val headOfFamilyDecisions = data.decisions[headOfFamilyId] ?: listOf()
                val feeDecisionPlacements =
                    headOfFamilyDecisions.flatMap { decision ->
                        decision.children.flatMap { child ->
                            data.permanentPlacements[child.child.id] ?: listOf()
                        }
                    }

                tracer.withSpan(
                    "generateDraftInvoice",
                    Tracing.headOfFamilyId withValue headOfFamilyId,
                ) {
                    generateDraftInvoice(
                        tx,
                        HeadOfFamilyInvoiceCalculationData(
                            headOfFamilyId,
                            data.codebtors[headOfFamilyId],
                            headOfFamilyDecisions,
                            feeDecisionPlacements +
                                (data.temporaryPlacements[headOfFamilyId] ?: listOf()),
                            data.invoicePeriod,
                            data.areaIds,
                            data.operationalDaysByChild,
                            data.businessDays,
                            data.feeThresholds,
                            data.absences,
                            data.freeChildren,
                            data.defaultServiceNeedOptions,
                        ),
                    )
                }
            } catch (e: Exception) {
                error("Failed to generate invoice for head of family $headOfFamilyId: $e")
            }
        }
    }

    private data class InvoiceRowStub(
        val child: ChildWithDateOfBirth,
        val placement: PlacementStub,
        val priceBeforeFeeAlterations: Int,
        val feeAlterations: List<Pair<FeeAlterationType, Int>>,
        val finalPrice: Int,
        val contractDaysPerMonth: Int?,
    )

    private enum class FullMonthAbsenceType {
        SICK_LEAVE_FULL_MONTH,
        ABSENCE_FULL_MONTH,
        SICK_LEAVE_11,
        NOTHING,
    }

    private fun generateDraftInvoice(
        tx: Database.Read,
        data: HeadOfFamilyInvoiceCalculationData,
    ): Invoice? {
        val isFreeJulyChild = data.freeChildren::contains
        val getDefaultServiceNeedOption = data.defaultServiceNeedOptions::get
        val businessDayCount = data.businessDays.ranges().map { it.durationInDays() }.sum().toInt()

        val feeDecisionRangesByChild =
            data.decisions
                .asSequence()
                .flatMap { decision ->
                    decision.children.asSequence().map { it.child.id to decision.validDuring }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { DateSet.of(it.value) }

        val getChildFullMonthAbsence = memoize { child: ChildId ->
            getFullMonthAbsence(
                ChildAbsences(
                    data.absences,
                    child,
                    data.operationalDaysByChild[child] ?: DateSet.empty(),
                )
            )
        }

        val getInvoiceMaxFee: (ChildId, Boolean) -> Int = { childId, capMaxFeeAtDefault ->
            val childDecisions =
                data.decisions.mapNotNull { decision ->
                    val childDecisionPart = decision.children.find { it.child.id == childId }
                    val dateRange = data.invoicePeriod.intersection(decision.validDuring)
                    if (dateRange != null && childDecisionPart != null) {
                        dateRange to childDecisionPart
                    } else {
                        null
                    }
                }

            val getDecisionPartMaxFee: (FeeDecisionChild) -> Int = { part ->
                val maxFeeBeforeFeeAlterations = calculateMaxFee(part.baseFee, part.siblingDiscount)
                part.feeAlterations.fold(maxFeeBeforeFeeAlterations) { currentFee, feeAlteration ->
                    currentFee +
                        feeAlterationEffect(
                            currentFee,
                            feeAlteration.type,
                            feeAlteration.amount,
                            feeAlteration.isAbsolute,
                        )
                }
            }
            val getDefaultMaxFee: (PlacementType, Int) -> Int = { placementType, discountedFee ->
                val feeCoefficient =
                    getDefaultServiceNeedOption(placementType)?.feeCoefficient
                        ?: throw Exception(
                            "No default service need option found for placement type $placementType"
                        )
                (feeCoefficient * BigDecimal(discountedFee)).toInt()
            }

            val childDecisionMaxFees =
                childDecisions.map { (dateRange, decisionPart) ->
                    val decisionPartMaxFee = getDecisionPartMaxFee(decisionPart)
                    dateRange to
                        minOf(
                            decisionPartMaxFee,
                            if (capMaxFeeAtDefault) {
                                getDefaultMaxFee(decisionPart.placement.type, decisionPartMaxFee)
                            } else Int.MAX_VALUE,
                        )
                }

            if (featureConfig.useContractDaysAsDailyFeeDivisor) {
                childDecisionMaxFees.maxOf { (_, maxFee) -> maxFee }
            } else {
                childDecisionMaxFees
                    .map { (dateRange, maxFee) ->
                        val daysInRange =
                            data.businessDays
                                .intersectRanges(dateRange)
                                .map { it.durationInDays() }
                                .sum()
                        (BigDecimal(maxFee) * BigDecimal(daysInRange)).divide(
                            BigDecimal(businessDayCount),
                            2,
                            RoundingMode.HALF_UP,
                        )
                    }
                    .fold(BigDecimal.ZERO) { sum, maxFee -> sum + maxFee }
                    .toInt()
            }
        }

        val rowStubs =
            data.placements
                .groupBy { it.second.child }
                .asSequence()
                .sortedByDescending { (child) -> child.dateOfBirth }
                .flatMapIndexed { index, (child, placements) ->
                    placements.flatMap { (placementDateRange, placement) ->
                        val relevantPeriod =
                            FiniteDateRange(
                                maxOf(data.invoicePeriod.start, placementDateRange.start),
                                minOf(data.invoicePeriod.end, placementDateRange.end),
                            )
                        val periodDecisions =
                            data.decisions.filter { placementDateRange.overlaps(it.validDuring) }

                        when (placement.type) {
                            PlacementType.TEMPORARY_DAYCARE,
                            PlacementType.TEMPORARY_DAYCARE_PART_DAY -> {
                                val partDay =
                                    placement.type == PlacementType.TEMPORARY_DAYCARE_PART_DAY
                                val fee =
                                    data.feeThresholds.calculatePriceForTemporary(
                                        partDay,
                                        index + 1,
                                    )
                                listOf(
                                    relevantPeriod to
                                        InvoiceRowStub(child, placement, fee, listOf(), fee, null)
                                )
                            }
                            else ->
                                periodDecisions
                                    .mapNotNull { decision ->
                                        decision.children
                                            .find { part -> part.child == child }
                                            ?.let { decision.validDuring to it }
                                    }
                                    .filterNot { (_, part) -> isFreeJulyChild(part.child.id) }
                                    .map { (decisionPeriod, part) ->
                                        FiniteDateRange(
                                            maxOf(relevantPeriod.start, decisionPeriod.start),
                                            minOf(relevantPeriod.end, decisionPeriod.end),
                                        ) to
                                            InvoiceRowStub(
                                                ChildWithDateOfBirth(
                                                    part.child.id,
                                                    part.child.dateOfBirth,
                                                ),
                                                PlacementStub(
                                                    part.child,
                                                    part.placement.unitId,
                                                    part.placement.type,
                                                ),
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
                }

        val rows =
            rowStubs
                .groupBy { (_, stub) -> stub.child }
                .flatMap { (child, childStubs) ->
                    val separatePeriods =
                        mergePeriods(childStubs.map { it.first to it.second }).map {
                            it.first to it.second
                        }

                    val logic =
                        invoiceGenerationLogicChooser.logicForMonth(
                            tx,
                            data.invoicePeriod.start.year,
                            data.invoicePeriod.start.month,
                            child.id,
                        )
                    if (logic == InvoiceGenerationLogic.Free) return@flatMap listOf()

                    // In cities that use contract days it is not allowed to change service needs
                    // during middle of the month, so picking contractDaysPerMonth from the first
                    // one is safe
                    val contractDaysPerMonth =
                        separatePeriods.first().second.contractDaysPerMonth.takeIf {
                            featureConfig.useContractDaysAsDailyFeeDivisor
                        }

                    val dailyFeeDivisor =
                        contractDaysPerMonth
                            ?: featureConfig.dailyFeeDivisorOperationalDaysOverride
                            ?: businessDayCount

                    val childOperationalDays =
                        data.operationalDaysByChild[child.id] ?: DateSet.empty()

                    val businessDaysWithoutDecision =
                        data.businessDays - (feeDecisionRangesByChild[child.id] ?: DateSet.empty())

                    val attendanceDates =
                        getAttendanceDates(
                            data.invoicePeriod,
                            childOperationalDays,
                            contractDaysPerMonth,
                            isPartialMonthChild = businessDaysWithoutDecision.isNotEmpty(),
                            absences = ChildAbsences(data.absences, child.id, childOperationalDays),
                        )

                    val invoiceRows = mutableListOf<InvoiceRow>()
                    var invoiceRowSum = 0
                    separatePeriods
                        .filter { (_, rowStub) -> rowStub.finalPrice != 0 }
                        .forEach { (period, rowStub) ->
                            val rows =
                                toInvoiceRows(
                                    invoiceRowSum,
                                    period,
                                    rowStub,
                                    dailyFeeDivisor,
                                    numRelevantOperationalDays =
                                        minOf(
                                            contractDaysPerMonth ?: businessDayCount,
                                            dailyFeeDivisor,
                                        ),
                                    attendanceDates,
                                    ChildAbsences(
                                        data.absences,
                                        rowStub.child.id,
                                        childOperationalDays,
                                    ),
                                    getChildFullMonthAbsence(child.id),
                                    getInvoiceMaxFee,
                                )
                            invoiceRowSum += rows.sumOf { it.price }
                            invoiceRows += rows
                        }
                    invoiceRows
                }
                .let { rows -> applyRoundingRows(rows, data.decisions, data.invoicePeriod) }
                .filter { row -> row.price != 0 }

        if (rows.isEmpty()) return null

        val areaId =
            rowStubs
                .maxByOrNull { (_, stub) -> stub.child.dateOfBirth }!!
                .let { (_, stub) ->
                    data.areaIds[stub.placement.unit]
                        ?: error("Couldn't find areaId for daycare (${stub.placement.unit})")
                }

        return Invoice(
            id = InvoiceId(UUID.randomUUID()),
            status = InvoiceStatus.DRAFT,
            periodStart = data.invoicePeriod.start,
            periodEnd = data.invoicePeriod.end,
            areaId = areaId,
            headOfFamily = data.headOfFamily,
            codebtor = data.codebtor,
            rows = rows,
        )
    }

    private fun getFullMonthAbsence(absences: ChildAbsences): FullMonthAbsenceType {
        val operationalDays = absences.operationalDays()

        val allSickLeaves =
            operationalDays.all { date -> absences.hasAbsenceOnDate(date, AbsenceType.SICKLEAVE) }
        val atLeastOneSickLeave =
            operationalDays.any { date -> absences.hasAbsenceOnDate(date, AbsenceType.SICKLEAVE) }
        val allSickLeavesOrPlannedAbsences =
            operationalDays.all { date ->
                absences.hasAbsenceOnDate(date, AbsenceType.SICKLEAVE) ||
                    absences.hasAbsenceOnDate(date, AbsenceType.PLANNED_ABSENCE)
            }
        val atLeast11SickLeaves =
            operationalDays.count { date ->
                absences.hasAbsenceOnDate(date, AbsenceType.SICKLEAVE)
            } >= 11
        val allAbsences = operationalDays.all { date -> absences.hasAbsenceOnDate(date) }

        return if (allSickLeaves) {
            FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH
        } else if (
            featureConfig.freeSickLeaveOnContractDays &&
                atLeastOneSickLeave &&
                allSickLeavesOrPlannedAbsences
        ) {
            // freeSickLeaveOnContractDays: The month becomes free if it has at least one
            // sick leave, and a sick leave or planned absence on all days
            FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH
        } else if (atLeast11SickLeaves) {
            FullMonthAbsenceType.SICK_LEAVE_11
        } else if (allAbsences) {
            FullMonthAbsenceType.ABSENCE_FULL_MONTH
        } else {
            FullMonthAbsenceType.NOTHING
        }
    }

    private fun getAttendanceDates(
        period: FiniteDateRange,
        childOperationalDays: DateSet,
        contractDaysPerMonth: Int?,
        isPartialMonthChild: Boolean,
        absences: ChildAbsences,
    ): List<LocalDate> {
        val hasPlannedAbsence = { date: LocalDate ->
            absences.hasAbsenceOnDate(date, AbsenceType.PLANNED_ABSENCE)
        }

        val attendanceDates =
            operationalDatesByWeek(period, childOperationalDays).flatMap { weekOperationalDates ->
                if (contractDaysPerMonth != null) {
                    // Use real attendance dates (with no planned absences) for contract day
                    // children
                    weekOperationalDates.filterNot(hasPlannedAbsence)
                } else {
                    // Take at most 5 days per week (for round-the-clock units)
                    weekOperationalDates.take(5)
                }
            }

        // If this is a full month for a contract day child (not in partialMonthChildren), make sure
        // that there's no less than `contractDaysPerMonth` days even if they have more
        // planned absences than they should
        return if (
            contractDaysPerMonth != null &&
                !isPartialMonthChild &&
                attendanceDates.size < contractDaysPerMonth
        ) {
            val extraDatesToAdd = contractDaysPerMonth - attendanceDates.size
            val operationalDaysWithoutAttendance =
                childOperationalDays
                    .ranges()
                    .flatMap { it.dates() }
                    .filter { date -> !attendanceDates.contains(date) }
                    .sorted()

            (attendanceDates + operationalDaysWithoutAttendance.take(extraDatesToAdd)).sorted()
        } else {
            attendanceDates
        }
    }

    private fun operationalDatesByWeek(
        period: FiniteDateRange,
        operationalDays: DateSet,
    ): List<List<LocalDate>> {
        return period
            .dates()
            .fold<LocalDate, List<List<LocalDate>>>(listOf()) { weeks, date ->
                if (weeks.isEmpty() || date.dayOfWeek == DayOfWeek.MONDAY) {
                    weeks.plusElement(listOf(date))
                } else {
                    weeks.dropLast(1).plusElement(weeks.last() + date)
                }
            }
            .map { week -> week.filter { date -> operationalDays.includes(date) } }
    }

    private fun toInvoiceRows(
        accumulatedSum: Int,
        period: FiniteDateRange,
        invoiceRowStub: InvoiceRowStub,
        dailyFeeDivisor: Int,
        numRelevantOperationalDays: Int,
        attendanceDates: List<LocalDate>,
        absenceQueries: ChildAbsences,
        fullMonthAbsenceType: FullMonthAbsenceType,
        getInvoiceMaxFee: (ChildId, Boolean) -> Int,
    ): List<InvoiceRow> {
        val refundAbsenceDates =
            invoiceRowStub.placement.type != PlacementType.TEMPORARY_DAYCARE_PART_DAY ||
                featureConfig.temporaryDaycarePartDayAbsenceGivesADailyRefund
        return when (invoiceRowStub.placement.type) {
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
                toTemporaryPlacementInvoiceRows(
                    period,
                    invoiceRowStub.child,
                    invoiceRowStub.placement.type,
                    invoiceRowStub.priceBeforeFeeAlterations,
                    invoiceRowStub.placement.unit,
                    dailyFeeDivisor,
                    attendanceDates,
                    isDateRefunded =
                        if (refundAbsenceDates) { date -> absenceQueries.hasAbsenceOnDate(date) }
                        else { _ -> false },
                )
            else ->
                toPermanentPlacementInvoiceRows(
                    accumulatedSum,
                    period,
                    invoiceRowStub.child,
                    invoiceRowStub.placement.type,
                    invoiceRowStub.priceBeforeFeeAlterations,
                    invoiceRowStub.finalPrice,
                    invoiceRowStub.placement.unit,
                    dailyFeeDivisor,
                    invoiceRowStub.contractDaysPerMonth,
                    numRelevantOperationalDays,
                    attendanceDates,
                    invoiceRowStub.feeAlterations,
                    absenceQueries,
                    fullMonthAbsenceType,
                    getInvoiceMaxFee,
                )
        }
    }

    private fun calculateDailyPriceForInvoiceRow(price: Int, dailyFeeDivisor: Int): Int =
        BigDecimal(price).divide(BigDecimal(dailyFeeDivisor), 0, RoundingMode.HALF_UP).toInt()

    private fun toTemporaryPlacementInvoiceRows(
        period: FiniteDateRange,
        child: ChildWithDateOfBirth,
        placementType: PlacementType,
        price: Int,
        unitId: DaycareId,
        dailyFeeDivisor: Int,
        attendanceDates: List<LocalDate>,
        isDateRefunded: (date: LocalDate) -> Boolean,
    ): List<InvoiceRow> {
        val amount =
            attendanceDates
                .take(dailyFeeDivisor)
                .filter { date -> period.includes(date) }
                .filterNot { date -> isDateRefunded(date) }
                .size

        return if (amount == 0) {
            listOf()
        } else {
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    periodStart = period.start,
                    periodEnd = period.end,
                    child = child.id,
                    amount = amount,
                    unitPrice = price,
                    unitId = unitId,
                    product = productProvider.mapToProduct(placementType),
                    correctionId = null,
                )
            )
        }
    }

    private fun toPermanentPlacementInvoiceRows(
        accumulatedSum: Int,
        period: FiniteDateRange,
        child: ChildWithDateOfBirth,
        placementType: PlacementType,
        price: Int,
        finalPrice: Int,
        unitId: DaycareId,
        dailyFeeDivisor: Int,
        contractDaysPerMonth: Int?,
        numRelevantOperationalDays: Int,
        attendanceDates: List<LocalDate>,
        feeAlterations: List<Pair<FeeAlterationType, Int>>,
        absenceQueries: ChildAbsences,
        fullMonthAbsenceType: FullMonthAbsenceType,
        getInvoiceMaxFee: (ChildId, Boolean) -> Int,
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

        val product = productProvider.mapToProduct(placementType)
        val (amount, unitPrice) =
            if (isFullMonth) {
                Pair(1, { p: Int -> p })
            } else {
                Pair(
                    periodAttendanceDates.size,
                    { p: Int -> calculateDailyPriceForInvoiceRow(p, dailyFeeDivisor) },
                )
            }

        val initialRows =
            listOf(
                InvoiceRow(
                    id = InvoiceRowId(UUID.randomUUID()),
                    child = child.id,
                    periodStart = period.start,
                    periodEnd = period.end,
                    amount = amount,
                    unitPrice = unitPrice(price),
                    unitId = unitId,
                    product = product,
                    correctionId = null,
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
                        correctionId = null,
                    )
                }

        val withDailyModifiers =
            initialRows +
                surplusContractDays(
                    accumulatedSum + initialRows.sumOf { it.price },
                    period,
                    child,
                    finalPrice,
                    unitId,
                    contractDaysPerMonth,
                    attendanceDates,
                    absenceQueries,
                    fullMonthAbsenceType in
                        listOf(
                            FullMonthAbsenceType.SICK_LEAVE_FULL_MONTH,
                            FullMonthAbsenceType.ABSENCE_FULL_MONTH,
                        ),
                    getInvoiceMaxFee,
                    placementType,
                ) +
                dailyAbsenceRefund(
                    period,
                    initialRows,
                    child,
                    absenceQueries,
                    periodAttendanceDates,
                    numRelevantOperationalDays,
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
                        correctionId = null,
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
                    correctionId = null,
                )
            }
    }

    private val plannedAbsenceTypes = setOf(AbsenceType.PLANNED_ABSENCE, AbsenceType.FREE_ABSENCE)

    private fun surplusContractDays(
        accumulatedSum: Int,
        period: FiniteDateRange,
        child: ChildWithDateOfBirth,
        monthlyPrice: Int,
        unitId: DaycareId,
        contractDaysPerMonth: Int?,
        attendanceDates: List<LocalDate>,
        absences: ChildAbsences,
        isAbsentFullMonth: Boolean,
        getInvoiceMaxFee: (ChildId, Boolean) -> Int,
        placementType: PlacementType,
    ): List<InvoiceRow> {
        if (contractDaysPerMonth == null || isAbsentFullMonth) return listOf()

        val untilEndOfPeriod = FiniteDateRange(LocalDate.MIN, period.end)
        val attendances =
            attendanceDates
                .filter { untilEndOfPeriod.includes(it) && !absences.hasAbsenceOnDate(it) }
                .size
        val unplannedAbsenceSurplusDays =
            if (featureConfig.unplannedAbsencesAreContractSurplusDays) {
                absences.absenceCountInPeriod(
                    untilEndOfPeriod,
                    AbsenceType.entries - plannedAbsenceTypes,
                )
            } else {
                0
            }
        val attendanceDays = attendances + unplannedAbsenceSurplusDays

        return if (contractDaysPerMonth < attendanceDays) {
            val surplusAttendanceDays = attendanceDays - contractDaysPerMonth
            val surplusDailyPrice =
                calculateDailyPriceForInvoiceRow(monthlyPrice, contractDaysPerMonth)
            val totalAddition = surplusAttendanceDays * surplusDailyPrice
            val maxPrice =
                getInvoiceMaxFee(
                    child.id,
                    listOf(PlacementType.PREPARATORY_DAYCARE, PlacementType.PRESCHOOL_DAYCARE)
                        .contains(placementType),
                )

            val (amount, unitPrice) =
                when {
                    // surplus days increase takes invoice row sum above max price threshold
                    accumulatedSum + totalAddition > maxPrice -> 1 to (maxPrice - accumulatedSum)
                    // total attendances days is over the max contract day surplus threshold
                    (featureConfig.maxContractDaySurplusThreshold ?: Int.MAX_VALUE) <
                        attendanceDays -> 1 to (maxPrice - accumulatedSum)
                    else -> surplusAttendanceDays to surplusDailyPrice
                }
            // it is possible that the max fee is not over the already accumulated invoice total so
            // this prevents the
            // surplus from being a 0€ row or a discount
            if (unitPrice > 0) {
                listOf(
                    InvoiceRow(
                        id = InvoiceRowId(UUID.randomUUID()),
                        periodStart = period.start,
                        periodEnd = period.end,
                        child = child.id,
                        product = productProvider.contractSurplusDay,
                        unitId = unitId,
                        amount = amount,
                        unitPrice = unitPrice,
                        correctionId = null,
                    )
                )
            } else {
                listOf()
            }
        } else {
            listOf()
        }
    }

    private fun dailyAbsenceRefund(
        period: FiniteDateRange,
        rows: List<InvoiceRow>,
        child: ChildWithDateOfBirth,
        absenceQueries: ChildAbsences,
        periodAttendanceDates: List<LocalDate>,
        numRelevantOperationalDays: Int,
        isFullMonth: Boolean,
        dailyFeeDivisor: Int,
        toInvoiceRow: (ProductKey, Int, Int) -> InvoiceRow,
    ): List<InvoiceRow> {
        assert(periodAttendanceDates.size <= dailyFeeDivisor)

        val total = invoiceRowTotal(rows)
        if (total == 0) return listOf()

        val refundedDayCount = getRefundedDays(period, child, absenceQueries)
        if (refundedDayCount == 0) return listOf()

        val (amount, unitPrice) =
            if (refundedDayCount >= minOf(dailyFeeDivisor, numRelevantOperationalDays)) {
                1 to -total
            } else {
                refundedDayCount to
                    -calculateDailyPriceForInvoiceRow(
                        total,
                        if (isFullMonth) dailyFeeDivisor else periodAttendanceDates.size,
                    )
            }

        return listOf(toInvoiceRow(productProvider.dailyRefund, amount, unitPrice))
    }

    private fun getRefundedDays(
        period: FiniteDateRange,
        child: ChildWithDateOfBirth,
        absenceQueries: ChildAbsences,
    ): Int {
        val forceMajeureDays =
            absenceQueries.absenceCountInPeriod(
                period,
                listOfNotNull(
                    AbsenceType.FORCE_MAJEURE,
                    if (featureConfig.freeAbsenceGivesADailyRefund) AbsenceType.FREE_ABSENCE
                    else null,
                ),
            )

        val under2YearOldPeriod =
            FiniteDateRange(child.dateOfBirth, child.dateOfBirth.plusYears(2).minusDays(1))
                .intersection(period)

        val parentLeaveDays =
            if (under2YearOldPeriod != null) {
                absenceQueries.absenceCountInPeriod(
                    under2YearOldPeriod,
                    listOf(AbsenceType.PARENTLEAVE),
                )
            } else {
                0
            }

        return forceMajeureDays + parentLeaveDays
    }

    private fun monthlyAbsenceDiscount(
        rows: List<InvoiceRow>,
        fullMonthAbsenceType: FullMonthAbsenceType,
        toInvoiceRow: (ProductKey, Int) -> InvoiceRow,
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
        invoicePeriod: FiniteDateRange,
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
                                    periodEnd = invoicePeriod.end,
                                    amount = 1,
                                    unitPrice = difference,
                                )
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                if (roundingRow != null) rows + roundingRow else rows
            }
    }

    class InvoiceCalculationData(
        val decisions: Map<PersonId, List<FeeDecision>>,
        val permanentPlacements: Map<ChildId, List<Pair<FiniteDateRange, PlacementStub>>>,
        val temporaryPlacements: Map<PersonId, List<Pair<FiniteDateRange, PlacementStub>>>,
        val invoicePeriod: FiniteDateRange,
        val areaIds: Map<DaycareId, AreaId>,
        val operationalDaysByChild: Map<ChildId, DateSet>,
        val businessDays: DateSet,
        val feeThresholds: FeeThresholds,
        val absences: Map<AbsenceType, Map<ChildId, DateSet>>,
        val freeChildren: Set<ChildId>,
        val codebtors: Map<PersonId, PersonId?>,
        val defaultServiceNeedOptions: Map<PlacementType, ServiceNeedOption>,
    )

    class HeadOfFamilyInvoiceCalculationData(
        val headOfFamily: PersonId,
        val codebtor: PersonId?,
        val decisions: List<FeeDecision>,
        val placements: List<Pair<FiniteDateRange, PlacementStub>>,
        val invoicePeriod: FiniteDateRange,
        val areaIds: Map<DaycareId, AreaId>,
        val operationalDaysByChild: Map<ChildId, DateSet>,
        val businessDays: DateSet,
        val feeThresholds: FeeThresholds,
        val absences: Map<AbsenceType, Map<ChildId, DateSet>>,
        val freeChildren: Set<ChildId>,
        val defaultServiceNeedOptions: Map<PlacementType, ServiceNeedOption>,
    )

    class ChildAbsences(
        private val absences: Map<AbsenceType, Map<ChildId, DateSet>>,
        private val childId: ChildId,
        private val operationalDaysSet: DateSet,
    ) {
        fun hasAbsenceOnDate(date: LocalDate): Boolean {
            return operationalDaysSet.includes(date) &&
                AbsenceType.entries.any { absences[it]?.get(childId)?.includes(date) ?: false }
        }

        fun hasAbsenceOnDate(date: LocalDate, type: AbsenceType): Boolean {
            return operationalDaysSet.includes(date) &&
                (absences[type]?.get(childId)?.includes(date) ?: false)
        }

        fun absenceCountInPeriod(
            period: FiniteDateRange,
            absenceTypes: Iterable<AbsenceType>,
        ): Int {
            return absenceTypes.sumOf { absenceType ->
                absences[absenceType]
                    ?.get(childId)
                    ?.intersection(operationalDaysSet)
                    ?.intersectRanges(period)
                    ?.sumOf { intersection -> intersection.durationInDays().toInt() } ?: 0
            }
        }

        fun operationalDays(): Sequence<LocalDate> =
            operationalDaysSet.ranges().flatMap { it.dates() }
    }
}
