// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.pis.HasDateOfBirth
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.math.BigDecimal
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

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

data class ServiceNeedOptionFeeRange(
    override val range: DateRange,
    val serviceNeedOptionFee: ServiceNeedOptionFee
) : WithRange

data class ServiceNeedOptionVoucherValueRange(
    val serviceNeedOptionId: ServiceNeedOptionId,
    override val range: DateRange,
    val baseValue: Int,
    val coefficient: BigDecimal,
    val value: Int,
    val baseValueUnder3y: Int,
    val coefficientUnder3y: BigDecimal,
    val valueUnder3y: Int
) : WithRange

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

data class Child(val id: PersonId, override val dateOfBirth: LocalDate, val ssn: String?) :
    HasDateOfBirth

data class ChildRelation(
    val headOfChild: PersonId,
    override val finiteRange: FiniteDateRange,
    @Nested("child") val child: Child
) : WithFiniteRange

data class PartnerRelation(val partnerId: PersonId, override val range: DateRange) : WithRange

data class IncomeRange(override val range: DateRange, val income: DecisionIncome) : WithRange

data class FeeAlterationRange(override val range: DateRange, val feeAlteration: FeeAlteration) :
    WithRange

data class FeeThresholdsRange(override val range: DateRange, val thresholds: FeeThresholds) :
    WithRange
