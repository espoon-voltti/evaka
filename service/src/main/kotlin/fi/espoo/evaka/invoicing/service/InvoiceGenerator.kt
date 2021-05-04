// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.invoicing.data.deleteDraftInvoicesByPeriod
import fi.espoo.evaka.invoicing.data.feeDecisionQueryBase
import fi.espoo.evaka.invoicing.data.toFeeDecision
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceRow
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.PermanentPlacement
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Placement
import fi.espoo.evaka.invoicing.domain.Product
import fi.espoo.evaka.invoicing.domain.TemporaryPlacement
import fi.espoo.evaka.invoicing.domain.calculatePriceForTemporary
import fi.espoo.evaka.invoicing.domain.getFeeAlterationProduct
import fi.espoo.evaka.invoicing.domain.getProductFromActivity
import fi.espoo.evaka.invoicing.domain.invoiceRowTotal
import fi.espoo.evaka.invoicing.domain.merge
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PGConstants
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.OperationalDays
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.operationalDays
import fi.espoo.evaka.shared.domain.orMax
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

fun Database.Transaction.createAllDraftInvoices(objectMapper: ObjectMapper, period: DateRange = getPreviousMonthRange()) {
    val effectiveDecisions = getInvoiceableFeeDecisions(objectMapper, period).groupBy { it.headOfFamily.id }
    val placements = getInvoiceablePlacements(period).groupBy { it.headOfFamily.id }
    val invoicedHeadsOfFamily = getInvoicedHeadsOfFamily(period)

    val unhandledDecisions = effectiveDecisions.filterNot { invoicedHeadsOfFamily.contains(it.key) }
    val unhandledPlacements = placements.filterNot { invoicedHeadsOfFamily.contains(it.key) }
    val daycareCodes = getDaycareCodes()
    val operationalDays = operationalDays(handle, period.start.year, period.start.month)

    val absences: List<AbsenceStub> = getAbsenceStubs(period, listOf(CareType.DAYCARE, CareType.PRESCHOOL_DAYCARE))

    val freeChildren: List<UUID> =
        if (period.start.month == Month.JULY && (period.end != null && period.end.month == Month.JULY && period.start.year == period.end.year)) {
            getFreeJulyChildren(period.start.year)
        } else emptyList()

    val invoices =
        generateDraftInvoices(
            unhandledDecisions,
            unhandledPlacements,
            period,
            daycareCodes,
            operationalDays,
            absences,
            freeChildren
        )

    deleteDraftInvoicesByPeriod(period)
    upsertInvoices(invoices)
}

internal fun generateDraftInvoices(
    decisions: Map<UUID, List<FeeDecision>>,
    placements: Map<UUID, List<Placements>>,
    period: DateRange,
    daycareCodes: Map<UUID, DaycareCodes>,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub> = listOf(),
    freeChildren: List<UUID> = listOf()
): List<Invoice> {
    return placements.keys.mapNotNull { headOfFamilyId ->
        generateDraftInvoice(
            decisions[headOfFamilyId] ?: listOf(),
            placements[headOfFamilyId] ?: listOf(),
            period,
            daycareCodes,
            operationalDays,
            absences,
            freeChildren
        )
    }
}

data class InvoiceRowStub(
    val child: PersonData.WithDateOfBirth,
    val placement: Placement,
    val priceBeforeFeeAlterations: Int,
    val feeAlterations: List<Pair<FeeAlteration.Type, Int>>
)

internal fun generateDraftInvoice(
    decisions: List<FeeDecision>,
    placements: List<Placements>,
    invoicePeriod: DateRange,
    daycareCodes: Map<UUID, DaycareCodes>,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub>,
    freeChildren: List<UUID>
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
                            PersonData.WithDateOfBirth(child.id, child.dateOfBirth),
                            TemporaryPlacement(placement.unit, placement.partDay),
                            calculatePriceForTemporary(placement.partDay, index + 1),
                            listOf()
                        )
                    )
                    is PlacementStub.Permanent ->
                        periodDecisions
                            .mapNotNull { decision ->
                                decision.parts.find { part -> part.child == child }
                                    ?.let { DateRange(decision.validFrom, decision.validTo) to it }
                            }
                            .filterNot { (_, part) -> part.finalFee() == 0 }
                            .filterNot { (_, part) -> freeChildren.contains(part.child.id) }
                            .map { (decisionPeriod, part) ->
                                DateRange(
                                    maxOf(relevantPeriod.start, decisionPeriod.start),
                                    minOf(orMax(relevantPeriod.end), orMax(decisionPeriod.end))
                                ) to InvoiceRowStub(
                                    PersonData.WithDateOfBirth(part.child.id, part.child.dateOfBirth),
                                    part.placement,
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
        .values.flatMap { mergePeriods(it) }
        .flatMap { (period, rowStub) ->
            val placementUnit = rowStub.placement.unit
            val codes = daycareCodes[placementUnit]
                ?: error("Couldn't find invoice codes for daycare ($placementUnit)")
            toInvoiceRows(
                period,
                rowStub,
                codes,
                operationalDays,
                absences.filter { it.childId == rowStub.child.id }
            )
        }
        .let { rows -> applyRoundingRows(rows, decisions, invoicePeriod) }
        .filter { row -> row.price() != 0 }

    if (rows.isEmpty()) return null

    val agreementType = rowStubs
        .maxByOrNull { (_, stub) -> stub.child.dateOfBirth }!!
        .let { (_, stub) ->
            daycareCodes[stub.placement.unit]?.areaCode
                ?: error("Couldn't find areaCode for daycare (${stub.placement.unit})")
        }

    return Invoice(
        UUID.randomUUID(),
        status = InvoiceStatus.DRAFT,
        periodStart = invoicePeriod.start,
        periodEnd = invoicePeriod.end!!,
        agreementType = agreementType,
        headOfFamily = PersonData.JustId(headOfFamily.id),
        rows = rows
    )
}

internal fun calculateDailyPriceForInvoiceRow(price: Int, operationalDays: Int): Int {
    return BigDecimal(price).divide(BigDecimal(operationalDays), 0, RoundingMode.HALF_UP).toInt()
}

internal fun toInvoiceRows(
    period: DateRange,
    invoiceRowStub: InvoiceRowStub,
    codes: DaycareCodes,
    operationalDays: OperationalDays,
    absences: List<AbsenceStub>
): List<InvoiceRow> {
    val (child, placement, price, feeAlterations) = invoiceRowStub

    return when (placement) {
        is PermanentPlacement -> toPermanentPlacementInvoiceRows(
            period,
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
    child: PersonData.WithDateOfBirth,
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
            id = UUID.randomUUID(),
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
    child: PersonData.WithDateOfBirth,
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

        val unitDays = unitOperationalDays.filter { day -> period.includes(day) }
        val weeks = operationalDays.fullMonth.fold(listOf<List<LocalDate>>()) { weeks, date ->
            if (weeks.isEmpty()) listOf(listOf(date))
            else {
                if (date.dayOfWeek == DayOfWeek.MONDAY) weeks.plusElement(listOf(date))
                else weeks.subList(0, weeks.size - 1).plusElement(weeks.last() + date)
            }
        }

        weeks.flatMap { week ->
            week.filter { unitDays.contains(it) }.take(operationalDays.generalCase.intersect(week).size)
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
                id = UUID.randomUUID(),
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
                id = UUID.randomUUID(),
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
                    id = UUID.randomUUID(),
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
            id = UUID.randomUUID(),
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
            id = UUID.randomUUID(),
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
                .flatMap { it.parts }
                .filter { it.child.id == child.id }
                .map { it.finalFee() }
                .distinct()

            val invoiceRowSum = rows.map { it.price() }.sum()

            val roundingRow = if (uniqueChildFees.size == 1) {
                val difference = uniqueChildFees.first() - invoiceRowSum

                if (difference != 0 && -20 < difference && difference < 20) {
                    rows.first().copy(
                        id = UUID.randomUUID(),
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

fun Database.Read.getInvoiceableFeeDecisions(objectMapper: ObjectMapper, period: DateRange): List<FeeDecision> {
    val sql =
        """
            $feeDecisionQueryBase
            WHERE
                decision.valid_from <= :period_end
                AND (decision.valid_to IS NULL OR decision.valid_to >= :period_start)
                AND decision.status = ANY(:effective::fee_decision_status[])
                ${
        /* delete this when these kinds of fee decisions stop existing */
        "AND (decision.valid_from <= decision.valid_to OR decision.valid_to IS NULL)"
        }
        """

    return createQuery(sql)
        .bind("period_start", period.start)
        .bind("period_end", period.end)
        .bind("effective", FeeDecisionStatus.effective)
        .map(toFeeDecision(objectMapper))
        .let { it.merge() }
}

fun Database.Read.getInvoicedHeadsOfFamily(period: DateRange): List<UUID> {
    val sql =
        "SELECT DISTINCT head_of_family FROM invoice WHERE period_start = :period_start AND period_end = :period_end AND status = :sent::invoice_status"

    return createQuery(sql)
        .bind("period_start", period.start)
        .bind("period_end", period.end)
        .bind("sent", InvoiceStatus.SENT)
        .map { rs, _ -> rs.getUUID("head_of_family") }
        .list()
}

data class AbsenceStub(
    val childId: UUID,
    val date: LocalDate,
    var careType: CareType,
    val absenceType: AbsenceType
)

// PRESENCE is used to "remove" an absence leaving a mark when the absence was removed in the database
// PLANNED_ABSENCE is used to indicate when a child is not even supposed to be present, it's not an actual absence
private val absenceTypesWithNoEffectOnInvoices = arrayOf(AbsenceType.PRESENCE, AbsenceType.PLANNED_ABSENCE)

fun Database.Read.getAbsenceStubs(spanningPeriod: DateRange, careTypes: List<CareType>): List<AbsenceStub> {
    val sql =
        """
        SELECT child_id, date, care_type, absence_type
        FROM absence
        WHERE :period @> date
        AND NOT absence_type = ANY(:absenceTypes)
        AND care_type = ANY(:careTypes)
        """

    return createQuery(sql)
        .bind("period", spanningPeriod)
        .bind("absenceTypes", absenceTypesWithNoEffectOnInvoices)
        .bind("careTypes", careTypes.toTypedArray())
        .map { rs, _ ->
            AbsenceStub(
                childId = rs.getUUID("child_id"),
                date = rs.getDate("date").toLocalDate(),
                careType = rs.getEnum("care_type"),
                absenceType = rs.getEnum("absence_type")
            )
        }
        .toList()
}

sealed class PlacementStub(open val unit: UUID) {
    data class Temporary(override val unit: UUID, val partDay: Boolean) : PlacementStub(unit)
    data class Permanent(override val unit: UUID) : PlacementStub(unit)
}

data class Placements(
    val period: DateRange,
    val headOfFamily: PersonData.JustId,
    val placements: List<Pair<PersonData.WithDateOfBirth, PlacementStub>>
)

internal fun Database.Read.getInvoiceablePlacements(
    spanningPeriod: DateRange
): List<Placements> {
    val placements = createQuery(
        // language=sql
        """
            SELECT p.child_id, p.start_date, p.end_date, p.unit_id, p.type FROM placement p
            JOIN daycare u ON p.unit_id = u.id AND u.invoiced_by_municipality
            WHERE daterange(start_date, end_date, '[]') && :period
        """.trimIndent()
    )
        .bind("period", spanningPeriod)
        .map { rs, _ ->
            Pair(
                rs.getUUID("child_id"),
                Triple(
                    DateRange(
                        rs.getObject("start_date", LocalDate::class.java),
                        rs.getObject("end_date", LocalDate::class.java)
                    ),
                    rs.getUUID("unit_id"),
                    rs.getEnum<PlacementType>("type")
                )
            )
        }
        .groupBy { (childId) -> childId }
        .mapValues { it.value.map { (_, triple) -> triple } }

    val familyCompositions = toFamilyCompositions(
        getChildrenWithHeadOfFamilies(placements.keys.toList(), spanningPeriod),
        spanningPeriod
    )

    return familyCompositions
        .map { (headOfFamily, families) ->
            val relevantPlacements = families.flatMap { (period, children) ->
                children.flatMap { child ->
                    (placements[child.id] ?: listOf())
                        .filter { it.first.overlaps(period) }
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
    relationships: List<Triple<DateRange, PersonData.JustId, PersonData.WithDateOfBirth>>,
    spanningPeriod: DateRange
): Map<PersonData.JustId, List<Pair<DateRange, List<PersonData.WithDateOfBirth>>>> {
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
    childIds: List<UUID>,
    period: DateRange
): List<Triple<DateRange, PersonData.JustId, PersonData.WithDateOfBirth>> {
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
        WHERE fridge_child.child_id IN (<childIds>)
            AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(:start, :end, '[]')
            AND conflict = false
    """

    return createQuery(sql)
        .bind("start", period.start)
        .bind("end", period.end)
        .bindList("childIds", childIds)
        .map { rs, _ ->
            Triple(
                DateRange(
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate().let { if (it < PGConstants.infinity) it else null }
                ),
                PersonData.JustId(id = UUID.fromString(rs.getString("head_of_child"))),
                PersonData.WithDateOfBirth(
                    id = UUID.fromString(rs.getString("child_id")),
                    dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate()
                )
            )
        }
        .list()
}

fun Database.Read.getDaycareCodes(): Map<UUID, DaycareCodes> {
    val sql =
        """
        SELECT daycare.id, daycare.cost_center, area.area_code, area.sub_cost_center
        FROM daycare INNER JOIN care_area AS area ON daycare.care_area_id = area.id
    """
    return createQuery(sql)
        .map { rs, _ ->
            UUID.fromString(rs.getString("id")) to DaycareCodes(
                areaCode = rs.getObject("area_code") as Int?,
                costCenter = rs.getString("cost_center"),
                subCostCenter = rs.getString("sub_cost_center")
            )
        }
        .toMap()
}

fun Database.Read.getFreeJulyChildren(year: Int): List<UUID> {
    val sql =
        //language=sql
        """
SELECT
  distinct(p09.child_id)
FROM
  (SELECT child_id FROM placement WHERE ${placementOn(year - 1, 9)}) p09,
  (SELECT child_id FROM placement WHERE ${placementOn(year - 1, 10)}) p10,
  (SELECT child_id FROM placement WHERE ${placementOn(year - 1, 11)}) p11,
  (SELECT child_id FROM placement WHERE ${placementOn(year - 1, 12)}) p12,
  (SELECT child_id FROM placement WHERE ${placementOn(year, 1)}) p01,
  (SELECT child_id FROM placement WHERE ${placementOn(year, 2)}) p02,
  (SELECT child_id FROM placement WHERE ${placementOn(year, 3)}) p03,
   ${if (year != 2020) {
            """
            (SELECT child_id FROM placement WHERE ${placementOn(year, 4)}) p04,
            (SELECT child_id FROM placement WHERE ${placementOn(year, 5)}) p05,
        """
        } else ""}
  (SELECT child_id FROM placement WHERE ${placementOn(year, 6)}) p06
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
        .mapTo(UUID::class.java)
        .list()
}

private fun placementOn(year: Int, month: Int): String {
    val firstOfMonth = "'$year-$month-01'"
    return "daterange(start_date, end_date, '[]') && daterange($firstOfMonth::date, ($firstOfMonth::date + INTERVAL '1 month -1 day')::date, '[]')"
}
