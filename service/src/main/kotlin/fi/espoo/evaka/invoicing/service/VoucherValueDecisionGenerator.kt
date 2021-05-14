// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementWithServiceNeed
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.calculateVoucherValue
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.getAgeCoefficient
import fi.espoo.evaka.invoicing.domain.getSiblingDiscountPercent
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

internal fun Database.Transaction.handleValueDecisionChanges(
    objectMapper: ObjectMapper,
    from: LocalDate,
    child: PersonData.WithDateOfBirth,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    allChildren: List<PersonData.WithDateOfBirth>
) {
    val familySize = 1 + (partner?.let { 1 } ?: 0) + allChildren.size
    val prices = getPricing(from)
    val voucherValues = getVoucherValues(from)
    val incomes = getIncomesFrom(objectMapper, listOfNotNull(headOfFamily.id, partner?.id), from)
    val feeAlterations =
        getFeeAlterationsFrom(listOf(child.id), from) + addECHAFeeAlterations(listOf(child), incomes)

    val placements = getPaidPlacements2(from, allChildren)
    val serviceVoucherUnits = getServiceVoucherUnits()

    val newDrafts =
        generateNewValueDecisions(
            from,
            child,
            headOfFamily,
            partner,
            familySize,
            placements,
            prices,
            voucherValues,
            incomes,
            feeAlterations,
            serviceVoucherUnits
        )

    lockValueDecisionsForChild(child.id)

    val existingDrafts =
        findValueDecisionsForChild(child.id, null, listOf(VoucherValueDecisionStatus.DRAFT))
    val activeDecisions = findValueDecisionsForChild(
        child.id,
        null,
        listOf(
            VoucherValueDecisionStatus.WAITING_FOR_SENDING,
            VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            VoucherValueDecisionStatus.SENT
        )
    )

    val updatedDecisions = updateExistingDecisions(from, newDrafts, existingDrafts, activeDecisions)
    deleteValueDecisions(existingDrafts.map { it.id })
    upsertValueDecisions(updatedDecisions)
}

private fun generateNewValueDecisions(
    from: LocalDate,
    voucherChild: PersonData.WithDateOfBirth,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    familySize: Int,
    allPlacements: List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PlacementWithServiceNeed>>>>,
    prices: List<Pair<DateRange, Pricing>>,
    voucherValues: List<Pair<DateRange, Int>>,
    incomes: List<Income>,
    feeAlterations: List<FeeAlteration>,
    serviceVoucherUnits: List<UUID>
): List<VoucherValueDecision> {
    val periods = incomes.map { DateRange(it.validFrom, it.validTo) } +
        prices.map { it.first } +
        allPlacements.flatMap { (child, placements) ->
            placements.map { it.first } + listOf(
                DateRange(child.dateOfBirth, child.dateOfBirth.plusYears(3).minusDays(1)),
                DateRange(child.dateOfBirth.plusYears(3), null)
            )
        } +
        feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .mapNotNull { period ->
            val price = prices.find { it.first.contains(period) }?.second
                ?: error("Missing price for period ${period.start} - ${period.end}, cannot generate voucher value decision")

            val baseValue = voucherValues.find { it.first.contains(period) }?.second
                ?: error("Missing voucher value for period ${period.start} - ${period.end}, cannot generate voucher value decision")

            val income = incomes
                .find { headOfFamily.id == it.personId && DateRange(it.validFrom, it.validTo).contains(period) }
                ?.toDecisionIncome()

            val partnerIncome =
                partner?.let {
                    incomes
                        .find {
                            partner.id == it.personId && DateRange(
                                it.validFrom,
                                it.validTo
                            ).contains(period)
                        }
                        ?.toDecisionIncome()
                }

            val periodPlacements = allPlacements
                .map { (child, placements) -> child to placements.find { it.first.contains(period) }?.second }
                .filter { (_, placement) -> placement != null }
                .map { (child, placement) -> child to placement!! }

            val voucherChildPlacement = periodPlacements.find { (child, _) -> child == voucherChild }?.second

            voucherChildPlacement
                // Skip the placement if it's not into a service voucher unit
                ?.takeIf { placement -> serviceVoucherUnits.any { unit -> unit == placement.unitId } }
                ?.let { placement ->
                    val familyIncomes = partner?.let { listOf(income, partnerIncome) } ?: listOf(income)
                    val baseCoPayment = calculateBaseFee(price, familySize, familyIncomes)

                    val siblingIndex = periodPlacements
                        .sortedByDescending { (child, _) -> child.dateOfBirth }
                        .indexOfFirst { (child, _) -> child == voucherChild }

                    val siblingDiscount = getSiblingDiscountPercent(siblingIndex + 1)
                    val coPaymentBeforeAlterations =
                        calculateFeeBeforeFeeAlterations(baseCoPayment, placement.serviceNeed.feeCoefficient, siblingDiscount)
                    val relevantFeeAlterations = feeAlterations.filter {
                        DateRange(it.validFrom, it.validTo).contains(period)
                    }
                    val feeAlterationsWithEffects =
                        toFeeAlterationsWithEffects(coPaymentBeforeAlterations, relevantFeeAlterations)
                    val finalCoPayment = coPaymentBeforeAlterations + feeAlterationsWithEffects.sumBy { it.effect }

                    val ageCoefficient = getAgeCoefficient(period, voucherChild.dateOfBirth)
                    val value = calculateVoucherValue(baseValue, ageCoefficient, placement.serviceNeed.voucherValueCoefficient)

                    period to VoucherValueDecision(
                        id = UUID.randomUUID(),
                        status = VoucherValueDecisionStatus.DRAFT,
                        headOfFamily = headOfFamily,
                        partner = partner,
                        headOfFamilyIncome = income,
                        partnerIncome = partnerIncome,
                        familySize = familySize,
                        pricing = price,
                        validFrom = period.start,
                        validTo = period.end,
                        child = voucherChild,
                        placement = VoucherValueDecisionPlacement(UnitData.JustId(placement.unitId), placement.type),
                        serviceNeed = VoucherValueDecisionServiceNeed(
                            placement.serviceNeed.feeCoefficient,
                            placement.serviceNeed.voucherValueCoefficient,
                            placement.serviceNeed.feeDescriptionFi,
                            placement.serviceNeed.feeDescriptionSv,
                            placement.serviceNeed.voucherValueDescriptionFi,
                            placement.serviceNeed.voucherValueDescriptionSv
                        ),
                        baseCoPayment = baseCoPayment,
                        siblingDiscount = siblingDiscount,
                        coPayment = coPaymentBeforeAlterations,
                        feeAlterations = toFeeAlterationsWithEffects(coPaymentBeforeAlterations, relevantFeeAlterations),
                        finalCoPayment = finalCoPayment,
                        baseValue = baseValue,
                        ageCoefficient = ageCoefficient,
                        voucherValue = value
                    )
                }
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

private fun Database.Read.getServiceVoucherUnits(): List<UUID> {
    // language=sql
    return createQuery("SELECT id FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER' AND NOT invoiced_by_municipality")
        .map { rs, _ -> rs.getUUID("id") }
        .toList()
}

private fun Database.Read.getVoucherValues(from: LocalDate): List<Pair<DateRange, Int>> {
    // language=sql
    val sql = "SELECT * FROM voucher_value WHERE validity && daterange(:from, null, '[]')"

    return createQuery(sql)
        .bind("from", from)
        .mapTo<VoucherValue>()
        .map { it.validity to it.voucherValue }
        .toList()
}
