package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FinanceDecision
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.periodsCanMerge
import java.math.BigDecimal
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

fun <T : FinanceDecision<T>> existsActiveDuplicateThatWillRemainEffective(
    draft: T,
    activeDecisions: List<T>,
    drafts: List<T>,
): Boolean {
    val activeDuplicate =
        activeDecisions.find {
            it.validDuring.contains(draft.validDuring) && it.contentEquals(draft)
        }
            ?: return false

    val nonIdenticalDraftsOverlappingActive =
        drafts.filter {
            it.validDuring.overlaps(activeDuplicate.validDuring) &&
                !it.contentEquals(activeDuplicate)
        }

    val activeValidUntil =
        nonIdenticalDraftsOverlappingActive.minOfOrNull { it.validFrom.minusDays(1) }
            ?: activeDuplicate.validTo

    if (activeValidUntil != null && activeValidUntil < activeDuplicate.validFrom) {
        return false // active decision will be annulled
    }

    val newActiveRange = DateRange(activeDuplicate.validFrom, activeValidUntil)

    return newActiveRange.contains(draft.validDuring)
}

fun <T : FinanceDecision<T>> mergeAdjacentIdenticalDrafts(decisions: List<T>): List<T> {
    return decisions
        .sortedBy { it.validFrom }
        .fold(emptyList()) { acc, next ->
            val prev = acc.lastOrNull()
            if (
                prev != null &&
                    periodsCanMerge(prev.validDuring, next.validDuring) &&
                    prev.contentEquals(next)
            ) {
                acc.dropLast(1) + prev.withValidity(DateRange(prev.validFrom, next.validTo))
            } else {
                acc + next
            }
        }
}

fun <Decision : FinanceDecision<Decision>, Difference> Decision.getDifferencesToPrevious(
    newDrafts: List<Decision>,
    existingActiveDecisions: List<Decision>,
    getDifferences: (decision1: Decision, decision2: Decision) -> Set<Difference>
): Set<Difference> {
    if (this.isEmpty()) {
        return emptySet()
    }

    val draftDifferences =
        newDrafts
            .filter { other ->
                !other.isEmpty() && periodsCanMerge(other.validDuring, this.validDuring)
            }
            .flatMap { other -> getDifferences(other, this) }

    if (draftDifferences.isNotEmpty()) {
        return draftDifferences.toSet()
    }

    val activeDifferences =
        existingActiveDecisions
            .filter { active -> periodsCanMerge(active.validDuring, this.validDuring) }
            .flatMap { active -> getDifferences(active, this) }

    return activeDifferences.toSet()
}

data class PlacementDetails(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val placementType: PlacementType,
    val unitId: DaycareId,
    val providerType: ProviderType,
    val invoicedUnit: Boolean,
    val hasServiceNeed: Boolean,
    val serviceNeedOption: ServiceNeedOption,
    val serviceNeedVoucherValues: ServiceNeedOptionVoucherValueRange?
) : WithFiniteRange {
    val financeDecisionType: FinanceDecisionType?
        get() =
            when {
                providerType == ProviderType.PRIVATE_SERVICE_VOUCHER ->
                    FinanceDecisionType.VOUCHER_VALUE_DECISION
                invoicedUnit && serviceNeedOption.feeCoefficient > BigDecimal.ZERO ->
                    FinanceDecisionType.FEE_DECISION
                else -> null
            }
}

data class PlacementRange(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val type: PlacementType,
    val unitId: DaycareId,
    val unitProviderType: ProviderType,
    val invoicedUnit: Boolean
) : WithFiniteRange

data class ServiceNeedRange(
    val childId: PersonId,
    override val finiteRange: FiniteDateRange,
    val optionId: ServiceNeedOptionId
) : WithFiniteRange

// these types will not provide sibling discount and will not be shown on any decision
private val ignoredPlacementTypes =
    setOf(PlacementType.CLUB, PlacementType.SCHOOL_SHIFT_CARE) + PlacementType.temporary

fun getPlacementDetails(
    tx: Database.Read,
    childIds: Set<PersonId>
): Map<PersonId, List<PlacementDetails>> {
    if (childIds.isEmpty()) return emptyMap()

    val serviceNeedOptions = tx.getServiceNeedOptions()
    val serviceNeedOptionVoucherValues =
        tx.getServiceNeedOptionVoucherValues().groupBy { it.serviceNeedOptionId }

    val placements =
        tx.createQuery<Any> {
                sql(
                    """
                SELECT 
                    child_id, 
                    daterange(pl.start_date, pl.end_date, '[]') as finite_range, 
                    pl.type,
                    pl.unit_id,
                    d.provider_type as unit_provider_type,
                    invoiced_by_municipality as invoiced_unit
                FROM placement pl
                JOIN daycare d ON pl.unit_id = d.id
                WHERE child_id = ANY(${bind(childIds)}) AND NOT pl.type = ANY(${bind(ignoredPlacementTypes)})
            """
                )
            }
            .mapTo<PlacementRange>()
            .groupBy { it.childId }

    val serviceNeeds =
        tx.createQuery(
                """
        SELECT child_id, daterange(sn.start_date, sn.end_date, '[]') as finite_range, sn.option_id
        FROM service_need sn
        JOIN placement p ON sn.placement_id = p.id
        WHERE child_id = ANY(:ids)
    """
            )
            .bind("ids", childIds.toTypedArray())
            .mapTo<ServiceNeedRange>()
            .groupBy { it.childId }

    val dateRanges =
        buildFiniteDateRanges(
            *placements.flatMap { it.value }.toTypedArray(),
            *serviceNeeds.flatMap { it.value }.toTypedArray(),
            *serviceNeedOptionVoucherValues.flatMap { it.value }.toTypedArray()
        )

    return childIds.associateWith { childId ->
        dateRanges.mapNotNull { range ->
            val placement =
                placements[childId]?.firstOrNull { it.range.contains(range) }
                    ?: return@mapNotNull null
            val serviceNeed = serviceNeeds[childId]?.firstOrNull { it.range.contains(range) }
            val serviceNeedOption =
                if (serviceNeed != null) {
                    serviceNeedOptions.find { it.id == serviceNeed.optionId }
                        ?: throw Error("Missing service need option ${serviceNeed.optionId}")
                } else {
                    serviceNeedOptions.find {
                        it.defaultOption && it.validPlacementType == placement.type
                    }
                        ?: throw Error("Missing default service need option for ${placement.type}")
                }

            val serviceNeedVoucherValues =
                serviceNeedOptionVoucherValues[serviceNeedOption.id]?.firstOrNull {
                    it.range.contains(range)
                }

            PlacementDetails(
                childId = childId,
                finiteRange = range,
                placementType = placement.type,
                unitId = placement.unitId,
                providerType = placement.unitProviderType,
                invoicedUnit = placement.invoicedUnit,
                hasServiceNeed = serviceNeed != null,
                serviceNeedOption = serviceNeedOption,
                serviceNeedVoucherValues = serviceNeedVoucherValues
            )
        }
    }
}

data class ServiceNeedOptionVoucherValueRange(
    val id: ServiceNeedOptionVoucherValueId,
    val serviceNeedOptionId: ServiceNeedOptionId,
    override val range: DateRange,
    val baseValue: Int,
    val coefficient: BigDecimal,
    val value: Int,
    val baseValueUnder3y: Int,
    val coefficientUnder3y: BigDecimal,
    val valueUnder3y: Int
) : WithRange

private fun Database.Read.getServiceNeedOptionVoucherValues():
    List<ServiceNeedOptionVoucherValueRange> {
    return createQuery(
            """
SELECT
    id,
    service_need_option_id,
    validity as range,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y
FROM service_need_option_voucher_value
        """
                .trimIndent()
        )
        .mapTo<ServiceNeedOptionVoucherValueRange>()
        .toList()
}

data class IncomeRange(override val range: DateRange, val income: DecisionIncome) : WithRange

data class FeeThresholdsRange(override val range: DateRange, val thresholds: FeeThresholds) :
    WithRange

data class ServiceNeedOptionFeeRange(
    override val range: DateRange,
    val serviceNeedOptionFee: ServiceNeedOptionFee
) : WithRange

data class FeeAlterationRange(override val range: DateRange, val feeAlteration: FeeAlteration) :
    WithRange

data class Child(val id: PersonId, override val dateOfBirth: LocalDate, val ssn: String?) :
    HasDateOfBirth

data class ChildRelation(
    val headOfChild: PersonId,
    override val finiteRange: FiniteDateRange,
    @Nested("child") val child: Child
) : WithFiniteRange

fun Database.Read.getChildRelations(parentIds: Set<PersonId>): Map<PersonId, List<ChildRelation>> {
    if (parentIds.isEmpty()) return emptyMap()

    return createQuery(
            """
            SELECT 
                fc.head_of_child, 
                daterange(fc.start_date, fc.end_date, '[]') as finite_range,
                p.id as child_id,
                p.date_of_birth as child_date_of_birth,
                p.social_security_number as child_ssn
            FROM fridge_child fc
            JOIN person p on fc.child_id = p.id
            WHERE head_of_child = ANY(:ids) AND NOT conflict
        """
        )
        .bind("ids", parentIds.toTypedArray())
        .mapTo<ChildRelation>()
        .mapNotNull {
            val under18 =
                FiniteDateRange(
                    it.child.dateOfBirth,
                    it.child.dateOfBirth.plusYears(18).minusDays(1)
                )
            it.range.intersection(under18)?.let { range -> it.copy(finiteRange = range) }
        }
        .groupBy { it.headOfChild }
}

data class PartnerRelation(val partnerId: PersonId, override val range: DateRange) : WithRange

fun Database.Read.getPartnerRelations(id: PersonId): List<PartnerRelation> {
    return createQuery(
            """
            SELECT 
                fp2.person_id as partnerId,
                daterange(fp2.start_date, fp2.end_date, '[]') as range
            FROM fridge_partner fp1
            JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx <> fp2.indx
            WHERE fp1.person_id = :id AND NOT fp1.conflict AND NOT fp2.conflict
        """
        )
        .bind("id", id)
        .mapTo<PartnerRelation>()
        .list()
}
