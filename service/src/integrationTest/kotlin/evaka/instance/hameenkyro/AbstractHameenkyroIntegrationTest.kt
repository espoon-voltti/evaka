// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.hameenkyro

import evaka.HameenkyroInstance
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.placement.PlacementType
import evaka.trevaka.AbstractIntegrationTest
import java.util.stream.Stream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "hameenkyro_evaka"])
@Import(HameenkyroInstance::class)
abstract class AbstractHameenkyroIntegrationTest : AbstractIntegrationTest("hameenkyro") {
    @Autowired protected lateinit var properties: HameenkyroProperties

    protected fun supportedDecisionTypes(): Stream<DecisionType> =
        Stream.of(
            DecisionType.DAYCARE,
            DecisionType.PRESCHOOL,
            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PREPARATORY_EDUCATION,
        )

    protected fun supportedProviderTypes(): Stream<ProviderType> =
        Stream.of(
            ProviderType.MUNICIPAL,
            ProviderType.PURCHASED,
            ProviderType.PRIVATE,
            ProviderType.PRIVATE_SERVICE_VOUCHER,
        )

    protected fun supportedPlacementTypes(): Stream<PlacementType> =
        Stream.of(
            PlacementType.DAYCARE,
            PlacementType.PRESCHOOL,
            PlacementType.PREPARATORY_DAYCARE,
            PlacementType.PRESCHOOL_DAYCARE_ONLY,
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.SCHOOL_SHIFT_CARE,
        )

    protected fun supportedFeeDecisionTypes(): Stream<FeeDecisionType> =
        Stream.of(
            FeeDecisionType.NORMAL,
            FeeDecisionType.RELIEF_REJECTED,
            FeeDecisionType.RELIEF_ACCEPTED,
        )

    protected fun supportedVoucherValueDecisionTypes(): Stream<VoucherValueDecisionType> =
        Stream.of(
            VoucherValueDecisionType.NORMAL,
            VoucherValueDecisionType.RELIEF_REJECTED,
            VoucherValueDecisionType.RELIEF_ACCEPTED,
        )
}
