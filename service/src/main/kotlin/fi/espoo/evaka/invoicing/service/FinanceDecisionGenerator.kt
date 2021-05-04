// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.deleteValueDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.getFeeAlterationsFrom
import fi.espoo.evaka.invoicing.data.getIncomesFrom
import fi.espoo.evaka.invoicing.data.getPricing
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.lockValueDecisionsForChild
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionPart
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.invoicing.domain.FridgeFamily
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.PermanentPlacementWithHours
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.invoicing.domain.Pricing
import fi.espoo.evaka.invoicing.domain.ServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.calculateBaseFee
import fi.espoo.evaka.invoicing.domain.calculateFeeBeforeFeeAlterations
import fi.espoo.evaka.invoicing.domain.calculateServiceNeed
import fi.espoo.evaka.invoicing.domain.calculateVoucherValue
import fi.espoo.evaka.invoicing.domain.decisionContentsAreEqual
import fi.espoo.evaka.invoicing.domain.getAgeCoefficient
import fi.espoo.evaka.invoicing.domain.getECHAIncrease
import fi.espoo.evaka.invoicing.domain.getServiceCoefficient
import fi.espoo.evaka.invoicing.domain.getSiblingDiscountPercent
import fi.espoo.evaka.invoicing.domain.toFeeAlterationsWithEffects
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.serviceneed.getServiceNeedsByChildDuringPeriod
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.asDistinctPeriods
import fi.espoo.evaka.shared.domain.mergePeriods
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.shared.domain.orMax
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class FinanceDecisionGenerator(private val objectMapper: ObjectMapper, env: Environment) {
    private val feeDecisionMinDate: LocalDate = LocalDate.parse(env.getRequiredProperty("fee_decision_min_date"))

    fun createRetroactive(tx: Database.Transaction, headOfFamily: UUID, from: LocalDate) {
        val period = DateRange(from, null)
        findFamiliesByHeadOfFamily(tx.handle, headOfFamily, period)
            .filter { it.period.overlaps(period) }
            .forEach {
                // intentionally does not care about feeDecisionMinDate
                tx.handleFeeDecisionChanges(
                    objectMapper,
                    maxOf(period.start, it.period.start),
                    it.headOfFamily,
                    it.partner,
                    it.children
                )
                tx.handleFeeDecisionChanges2(
                    objectMapper,
                    maxOf(period.start, it.period.start),
                    it.headOfFamily,
                    it.partner,
                    it.children
                )
            }
    }

    fun handlePlacement(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from new placement (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(tx.handle, childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleServiceNeed(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed service need (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(tx.handle, childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleFamilyUpdate(tx: Database.Transaction, adultId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed family (adultId: $adultId, period: $period)" }

        val families = findFamiliesByHeadOfFamily(tx.handle, adultId, period) + findFamiliesByPartner(tx.handle, adultId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleIncomeChange(tx: Database.Transaction, personId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed income (personId: $personId, period: $period)" }

        val families = findFamiliesByHeadOfFamily(tx.handle, personId, period) + findFamiliesByPartner(tx.handle, personId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    fun handleFeeAlterationChange(tx: Database.Transaction, childId: UUID, period: DateRange) {
        logger.debug { "Generating fee decisions from changed fee alteration (childId: $childId, period: $period)" }

        val families = findFamiliesByChild(tx.handle, childId, period)
        handleDecisionChangesForFamilies(tx, period, families)
    }

    private fun handleDecisionChangesForFamilies(tx: Database.Transaction, period: DateRange, families: List<FridgeFamily>) {
        families.filter { it.period.overlaps(period) }
            .forEach { family ->
                tx.handleFeeDecisionChanges(
                    objectMapper,
                    maxOf(feeDecisionMinDate, period.start, family.period.start),
                    family.headOfFamily,
                    family.partner,
                    family.children
                )
                tx.handleFeeDecisionChanges2(
                    objectMapper,
                    maxOf(feeDecisionMinDate, period.start, family.period.start),
                    family.headOfFamily,
                    family.partner,
                    family.children
                )
                family.children.forEach { child ->
                    tx.handleValueDecisionChanges(
                        objectMapper,
                        maxOf(period.start, family.period.start),
                        child,
                        family.headOfFamily,
                        family.partner,
                        family.children
                    )
                }
            }
    }
}

private fun Database.Transaction.handleFeeDecisionChanges(
    objectMapper: ObjectMapper,
    from: LocalDate,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    children: List<PersonData.WithDateOfBirth>
) {
    val familySize = 1 + (partner?.let { 1 } ?: 0) + children.size
    val prices = getPricing(handle, from)
    val incomes = getIncomesFrom(handle, objectMapper, listOfNotNull(headOfFamily.id, partner?.id), from)
    val feeAlterations =
        getFeeAlterationsFrom(children.map { it.id }, from) + addECHAFeeAlterations(children, incomes)

    val placements = getPaidPlacements(handle, from, children)
    val invoicedUnits = getUnitsThatAreInvoiced(handle)

    val newDrafts =
        generateNewFeeDecisions(
            from,
            headOfFamily,
            partner,
            familySize,
            placements,
            prices,
            incomes,
            feeAlterations,
            invoicedUnits
        )

    handle.lockFeeDecisionsForHeadOfFamily(headOfFamily.id)

    val existingDrafts =
        findFeeDecisionsForHeadOfFamily(handle, objectMapper, headOfFamily.id, null, listOf(FeeDecisionStatus.DRAFT))
    val activeDecisions = findFeeDecisionsForHeadOfFamily(
        handle,
        objectMapper,
        headOfFamily.id,
        null,
        listOf(
            FeeDecisionStatus.WAITING_FOR_SENDING,
            FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            FeeDecisionStatus.SENT
        )
    )

    val updatedDecisions = updateExistingDecisions(from, newDrafts, existingDrafts, activeDecisions)
    deleteFeeDecisions(handle, existingDrafts.map { it.id })
    upsertFeeDecisions(handle, objectMapper, updatedDecisions)
}

private fun Database.Transaction.handleValueDecisionChanges(
    objectMapper: ObjectMapper,
    from: LocalDate,
    child: PersonData.WithDateOfBirth,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    allChildren: List<PersonData.WithDateOfBirth>
) {
    val familySize = 1 + (partner?.let { 1 } ?: 0) + allChildren.size
    val prices = getPricing(handle, from)
    val voucherValues = handle.getVoucherValues(from)
    val incomes = getIncomesFrom(handle, objectMapper, listOfNotNull(headOfFamily.id, partner?.id), from)
    val feeAlterations =
        getFeeAlterationsFrom(listOf(child.id), from) + addECHAFeeAlterations(listOf(child), incomes)

    val placements = getPaidPlacements(handle, from, allChildren)
    val serviceVoucherUnits = getServiceVoucherUnits(handle)

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

    handle.lockValueDecisionsForChild(child.id)

    val existingDrafts =
        handle.findValueDecisionsForChild(objectMapper, child.id, null, listOf(VoucherValueDecisionStatus.DRAFT))
    val activeDecisions = handle.findValueDecisionsForChild(
        objectMapper,
        child.id,
        null,
        listOf(
            VoucherValueDecisionStatus.WAITING_FOR_SENDING,
            VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            VoucherValueDecisionStatus.SENT
        )
    )

    val updatedDecisions = updateExistingDecisions(from, newDrafts, existingDrafts, activeDecisions)
    handle.deleteValueDecisions(existingDrafts.map { it.id })
    handle.upsertValueDecisions(objectMapper, updatedDecisions)
}

internal fun addECHAFeeAlterations(
    children: List<PersonData.WithDateOfBirth>,
    incomes: List<Income>
): List<FeeAlteration> {
    return incomes.filter { it.worksAtECHA }.flatMap { income ->
        children.map { child -> getECHAIncrease(child.id, DateRange(income.validFrom, income.validTo)) }
    }
}

private fun generateNewFeeDecisions(
    from: LocalDate,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    familySize: Int,
    allPlacements: List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PermanentPlacementWithHours>>>>,
    prices: List<Pair<DateRange, Pricing>>,
    incomes: List<Income>,
    feeAlterations: List<FeeAlteration>,
    invoicedUnits: List<UUID>
): List<FeeDecision> {
    val periods = incomes.map { DateRange(it.validFrom, it.validTo) } +
        prices.map { it.first } +
        allPlacements.flatMap { (_, placements) ->
            placements.map { it.first }
        } +
        feeAlterations.map { DateRange(it.validFrom, it.validTo) }

    return asDistinctPeriods(periods, DateRange(from, null))
        .map { period ->
            val price = prices.find { it.first.contains(period) }?.second
                ?: error("Missing price for period ${period.start} - ${period.end}, cannot generate fee decision")

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

            val validPlacements = allPlacements
                .map { (child, placements) -> child to placements.find { it.first.contains(period) }?.second }
                .filter { (_, placement) -> placement != null }
                .map { (child, placement) -> child to placement!! }

            val parts = if (validPlacements.isNotEmpty()) {
                val familyIncomes = partner?.let { listOf(income, partnerIncome) } ?: listOf(income)

                val baseFee = calculateBaseFee(price, familySize, familyIncomes)
                validPlacements
                    .sortedByDescending { (child, _) -> child.dateOfBirth }
                    .mapIndexed { index, (child, placement) ->
                        val placedIntoInvoicedUnit = invoicedUnits.any { unit -> unit == placement.unit }
                        if (!placedIntoInvoicedUnit) {
                            return@mapIndexed null
                        }

                        val serviceNeedIsFree = placement.serviceNeed == ServiceNeed.LTE_0
                        if (serviceNeedIsFree) {
                            return@mapIndexed null
                        }

                        val siblingDiscount = getSiblingDiscountPercent(index + 1)
                        val feeBeforeAlterations = calculateFeeBeforeFeeAlterations(baseFee, placement.withoutHours(), siblingDiscount)
                        val relevantFeeAlterations = feeAlterations.filter {
                            it.personId == child.id && DateRange(it.validFrom, it.validTo).contains(period)
                        }

                        FeeDecisionPart(
                            child,
                            placement.withoutHours(),
                            baseFee,
                            siblingDiscount,
                            feeBeforeAlterations,
                            toFeeAlterationsWithEffects(feeBeforeAlterations, relevantFeeAlterations)
                        )
                    }.filterNotNull()
            } else {
                listOf()
            }

            period to FeeDecision(
                id = UUID.randomUUID(),
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamily = headOfFamily,
                partner = partner,
                headOfFamilyIncome = income,
                partnerIncome = partnerIncome,
                familySize = familySize,
                pricing = price,
                parts = parts.sortedBy { it.siblingDiscount },
                validFrom = period.start,
                validTo = period.end
            )
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

private fun generateNewValueDecisions(
    from: LocalDate,
    voucherChild: PersonData.WithDateOfBirth,
    headOfFamily: PersonData.JustId,
    partner: PersonData.JustId?,
    familySize: Int,
    allPlacements: List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PermanentPlacementWithHours>>>>,
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
                ?.takeIf { placement -> serviceVoucherUnits.any { unit -> unit == placement.unit } }
                ?.let { placement ->
                    val familyIncomes = partner?.let { listOf(income, partnerIncome) } ?: listOf(income)
                    val baseCoPayment = calculateBaseFee(price, familySize, familyIncomes)

                    val siblingIndex = periodPlacements
                        .sortedByDescending { (child, _) -> child.dateOfBirth }
                        .indexOfFirst { (child, _) -> child == voucherChild }

                    val siblingDiscount = getSiblingDiscountPercent(siblingIndex + 1)
                    val coPaymentBeforeAlterations =
                        calculateFeeBeforeFeeAlterations(baseCoPayment, placement.withoutHours(), siblingDiscount)
                    val relevantFeeAlterations = feeAlterations.filter {
                        DateRange(it.validFrom, it.validTo).contains(period)
                    }

                    val ageCoefficient = getAgeCoefficient(period, voucherChild.dateOfBirth)
                    val serviceCoefficient = getServiceCoefficient(placement)
                    val value = calculateVoucherValue(baseValue, ageCoefficient, serviceCoefficient)

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
                        placement = placement,
                        baseCoPayment = baseCoPayment,
                        siblingDiscount = siblingDiscount,
                        coPayment = coPaymentBeforeAlterations,
                        feeAlterations = toFeeAlterationsWithEffects(coPaymentBeforeAlterations, relevantFeeAlterations),
                        baseValue = baseValue,
                        ageCoefficient = ageCoefficient,
                        serviceCoefficient = serviceCoefficient,
                        value = value
                    )
                }
        }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
}

internal fun getUnitsThatAreInvoiced(h: Handle): List<UUID> {
    // language=sql
    return h.createQuery("SELECT id FROM daycare WHERE invoiced_by_municipality")
        .map { rs, _ -> rs.getUUID("id") }
        .toList()
}

private fun getServiceVoucherUnits(h: Handle): List<UUID> {
    // language=sql
    return h.createQuery("SELECT id FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER' AND NOT invoiced_by_municipality")
        .map { rs, _ -> rs.getUUID("id") }
        .toList()
}

private fun findFamiliesByChild(h: Handle, childId: UUID, period: DateRange): List<FridgeFamily> {
    val parentRelations = h.getParentships(null, childId, includeConflicts = false, period = period)

    return parentRelations.flatMap {
        val fridgePartners = h.getPartnersForPerson(it.headOfChildId, includeConflicts = false, period = period)
        val fridgeChildren = h.getParentships(it.headOfChildId, null, includeConflicts = false, period = period)
        generateFamilyCompositions(
            it.headOfChild.id,
            fridgePartners,
            fridgeChildren,
            DateRange(maxOf(period.start, it.startDate), minEndDate(period.end, it.endDate))
        )
    }
}

private fun findFamiliesByHeadOfFamily(h: Handle, headOfFamilyId: UUID, period: DateRange): List<FridgeFamily> {
    val childRelations = h.getParentships(headOfFamilyId, null, includeConflicts = false, period = period)
    val partners = h.getPartnersForPerson(headOfFamilyId, includeConflicts = false, period = period)
    return generateFamilyCompositions(headOfFamilyId, partners, childRelations, period)
}

private fun findFamiliesByPartner(h: Handle, personId: UUID, period: DateRange): List<FridgeFamily> {
    val possibleHeadsOfFamily = h.getPartnersForPerson(personId, includeConflicts = false, period = period)
    return possibleHeadsOfFamily.flatMap { findFamiliesByHeadOfFamily(h, it.person.id, period) }
}

private fun generateFamilyCompositions(
    headOfFamily: UUID,
    partners: Iterable<Partner>,
    parentships: Iterable<Parentship>,
    wholePeriod: DateRange
): List<FridgeFamily> {
    val periodsWhenChildrenAreNotAdults = parentships.map {
        val birthday = it.child.dateOfBirth
        DateRange(birthday, birthday.plusYears(18))
    }

    val allPeriods = partners.map { DateRange(it.startDate, it.endDate) } +
        parentships.map { DateRange(it.startDate, it.endDate) } + periodsWhenChildrenAreNotAdults

    val familyPeriods = asDistinctPeriods(allPeriods, wholePeriod)
        .map { period ->
            val partner = partners.find { DateRange(it.startDate, it.endDate).contains(period) }?.person
            val children = parentships
                .filter { DateRange(it.startDate, it.endDate).contains(period) }
                // Do not include children that are over 18 years old during the period
                .filter { it.child.dateOfBirth.plusYears(18) >= period.start }
                .map { it.child }
            period to Triple(
                PersonData.JustId(headOfFamily),
                partner?.let { PersonData.JustId(it.id) },
                children.map { PersonData.WithDateOfBirth(it.id, it.dateOfBirth) }
            )
        }

    return mergePeriods(familyPeriods).map { (period, familyData) ->
        FridgeFamily(
            headOfFamily = familyData.first,
            partner = familyData.second,
            children = familyData.third,
            period = period
        )
    }
}

private fun getPaidPlacements(
    h: Handle,
    from: LocalDate,
    children: List<PersonData.WithDateOfBirth>
): List<Pair<PersonData.WithDateOfBirth, List<Pair<DateRange, PermanentPlacementWithHours>>>> {
    return children.map { child ->
        val placements = getActivePaidPlacements(h, child.id, from)
        if (placements.isEmpty()) return@map child to listOf<Pair<DateRange, PermanentPlacementWithHours>>()

        val serviceNeeds = getServiceNeedsByChildDuringPeriod(h, child.id, from, null)
        val placementsWithServiceNeeds = addServiceNeedsToPlacements(placements, serviceNeeds)

        child to placementsWithServiceNeeds
    }
}

private val excludedPlacementTypes = arrayOf(
    fi.espoo.evaka.placement.PlacementType.CLUB,
    fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE,
    fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE_PART_DAY
)
/**
 * Leaves out club and temporary placements since they shouldn't have an effect on fee or value decisions
 */
private fun getActivePaidPlacements(h: Handle, childId: UUID, from: LocalDate): List<Placement> {
    // language=sql
    val sql =
        "SELECT * FROM placement WHERE child_id = :childId AND end_date >= :from AND NOT type = ANY(:excludedTypes::placement_type[])"

    return h.createQuery(sql)
        .bind("childId", childId)
        .bind("from", from)
        .bind("excludedTypes", excludedPlacementTypes)
        .mapTo<Placement>()
        .toList()
}

private fun addServiceNeedsToPlacements(
    placements: List<Placement>,
    serviceNeeds: List<fi.espoo.evaka.serviceneed.ServiceNeed>
): List<Pair<DateRange, PermanentPlacementWithHours>> {
    return placements.flatMap { placement ->
        val placementPeriod = DateRange(placement.startDate, placement.endDate)
        asDistinctPeriods(serviceNeeds.map { DateRange(it.startDate, it.endDate) }, placementPeriod)
            .map { period ->
                val serviceNeed = serviceNeeds.find { (DateRange(it.startDate, it.endDate)).contains(period) }
                val placementType = getPlacementType(placement)
                period to PermanentPlacementWithHours(
                    unit = placement.unitId,
                    type = placementType,
                    serviceNeed = calculateServiceNeed(placementType, serviceNeed?.hoursPerWeek),
                    hours = serviceNeed?.hoursPerWeek
                )
            }
            .let { mergePeriods(it) }
    }
}

private fun getPlacementType(placement: Placement): PlacementType =
    when (placement.type) {
        fi.espoo.evaka.placement.PlacementType.DAYCARE,
        fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME ->
            PlacementType.DAYCARE
        fi.espoo.evaka.placement.PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
            PlacementType.FIVE_YEARS_OLD_DAYCARE
        fi.espoo.evaka.placement.PlacementType.PRESCHOOL -> PlacementType.PRESCHOOL
        fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL_WITH_DAYCARE
        fi.espoo.evaka.placement.PlacementType.PREPARATORY -> PlacementType.PREPARATORY
        fi.espoo.evaka.placement.PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY_WITH_DAYCARE
        fi.espoo.evaka.placement.PlacementType.CLUB,
        fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE,
        fi.espoo.evaka.placement.PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
            error("Placements of type '${placement.type}' should not be included in finance decisions")
    }

internal fun <Decision : FinanceDecision<Decision>> mergeAndFilterUnnecessaryDrafts(
    drafts: List<Decision>,
    active: List<Decision>
): List<Decision> {
    if (drafts.isEmpty()) return drafts

    val minDate = drafts.map { it.validFrom }.minOrNull()!! // min always exists when list is non-empty
    val maxDate = drafts.map { it.validTo }.maxByOrNull { orMax(it) }

    return asDistinctPeriods((drafts + active).map { DateRange(it.validFrom, it.validTo) }, DateRange(minDate, maxDate))
        .fold(listOf<Decision>()) { decisions, period ->
            val keptDraft = drafts.find { DateRange(it.validFrom, it.validTo).contains(period) }?.let { draft ->
                val decision = active.find { DateRange(it.validFrom, it.validTo).contains(period) }
                if (draftIsUnnecessary(draft, decision, alreadyGeneratedDrafts = decisions.isNotEmpty())) {
                    null
                } else {
                    draft.withValidity(DateRange(period.start, period.end))
                }
            }
            if (keptDraft != null) decisions + keptDraft else decisions
        }
        .let { mergeDecisions(it) }
}

/*
 * a draft is unnecessary when:
 *   - the draft is "empty" and there is no existing sent decision that should be overridden
 *   - the draft is practically identical to an existing sent decision and no drafts have been generated before this draft
 */
internal fun <Decision : FinanceDecision<Decision>> draftIsUnnecessary(
    draft: Decision,
    sent: Decision?,
    alreadyGeneratedDrafts: Boolean
): Boolean {
    return (draft.isEmpty() && sent == null) ||
        (!alreadyGeneratedDrafts && sent != null && draft.contentEquals(sent))
}

internal fun <Decision : FinanceDecision<Decision>> mergeDecisions(
    decisions: List<Decision>
): List<Decision> {
    return decisions
        .map { DateRange(it.validFrom, it.validTo) to it }
        .let { mergePeriods(it, ::decisionContentsAreEqual) }
        .map { (period, decision) -> decision.withValidity(period) }
        .map { it.withRandomId() }
}

internal fun <Decision : FinanceDecision<Decision>> updateExistingDecisions(
    from: LocalDate,
    newDrafts: List<Decision>,
    existingDrafts: List<Decision>,
    activeDecisions: List<Decision>
): List<Decision> {
    val draftsWithUpdatedDates = filterOrUpdateStaleDrafts(existingDrafts, DateRange(from, null))
        .map { it.withRandomId() }

    val (withUpdatedEndDates, mergedDrafts) = updateDecisionEndDatesAndMergeDrafts(
        activeDecisions,
        newDrafts + draftsWithUpdatedDates
    )

    return mergedDrafts + withUpdatedEndDates
}

internal fun <Decision : FinanceDecision<Decision>> filterOrUpdateStaleDrafts(
    drafts: List<Decision>,
    period: DateRange
): List<Decision> {
    val (overlappingDrafts, nonOverlappingDrafts) = drafts.partition {
        DateRange(it.validFrom, it.validTo).overlaps(period)
    }

    val updatedOverlappingDrafts = when (period.end) {
        null -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start -> listOf(it.withValidity(DateRange(it.validFrom, period.start.minusDays(1))))
                else -> emptyList()
            }
        }
        else -> overlappingDrafts.flatMap {
            when {
                it.validFrom < period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.withValidity(DateRange(it.validFrom, period.start.minusDays(1))),
                    it.withValidity(DateRange(period.end.plusDays(1), it.validTo))
                )
                it.validFrom < period.start && orMax(it.validTo) <= orMax(period.end) -> listOf(
                    it.withValidity(DateRange(it.validFrom, period.start.minusDays(1)))
                )
                it.validFrom >= period.start && orMax(it.validTo) > orMax(period.end) -> listOf(
                    it.withValidity(DateRange(period.end.plusDays(1), it.validTo))
                )
                else -> emptyList()
            }
        }
    }

    return nonOverlappingDrafts + updatedOverlappingDrafts
}

internal fun <Decision : FinanceDecision<Decision>> updateDecisionEndDatesAndMergeDrafts(
    actives: List<Decision>,
    drafts: List<Decision>
): Pair<List<Decision>, List<Decision>> {
    val mergedDrafts = mergeDecisions(drafts)

    /*
     * Immediately update the validity end dates for active decisions if a new draft has the same contents and they
     * both are valid to the future
     */
    val (updatedActives, keptDrafts) = actives.fold(
        Pair(listOf<Decision>(), mergedDrafts)
    ) { (updatedActives, keptDrafts), decision ->
        val firstOverlappingSimilarDraft = keptDrafts.filter { draft ->
            decision.validFrom == draft.validFrom
        }.firstOrNull { draft -> decision.contentEquals(draft) }

        firstOverlappingSimilarDraft?.let { similarDraft ->
            val now = LocalDate.now()
            if (orMax(decision.validTo) >= now && orMax(similarDraft.validTo) >= now) {
                Pair(
                    updatedActives + decision.withValidity(DateRange(decision.validFrom, similarDraft.validTo)),
                    keptDrafts.filterNot { it.id == similarDraft.id }
                )
            } else null
        } ?: Pair(updatedActives, keptDrafts)
    }

    val allUpdatedActives = actives.map { decision -> updatedActives.find { it.id == decision.id } ?: decision }
    val filteredDrafts = mergeAndFilterUnnecessaryDrafts(keptDrafts, allUpdatedActives)

    return Pair(updatedActives, filteredDrafts)
}

private fun Handle.getVoucherValues(from: LocalDate): List<Pair<DateRange, Int>> {
    // language=sql
    val sql = "SELECT * FROM voucher_value WHERE validity && daterange(:from, null, '[]')"

    return createQuery(sql)
        .bind("from", from)
        .mapTo<VoucherValue>()
        .map { it.validity to it.voucherValue }
        .toList()
}
