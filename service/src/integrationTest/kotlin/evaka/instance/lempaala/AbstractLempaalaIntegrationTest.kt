// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala

import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.placement.PlacementType
import evaka.trevaka.AbstractIntegrationTest
import java.util.stream.Stream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "lempaala_evaka"])
abstract class AbstractLempaalaIntegrationTest : AbstractIntegrationTest() {
    @Autowired protected lateinit var properties: LempaalaProperties

    protected fun supportedDecisionTypes(): Stream<DecisionType> =
        Stream.of(DecisionType.DAYCARE, DecisionType.PRESCHOOL_DAYCARE)

    protected fun supportedProviderTypes(): Stream<ProviderType> =
        Stream.of(
            ProviderType.MUNICIPAL,
            ProviderType.PURCHASED,
            ProviderType.PRIVATE_SERVICE_VOUCHER,
        )

    protected fun supportedPlacementTypes(): Stream<PlacementType> =
        Stream.of(
            PlacementType.DAYCARE,
            PlacementType.PRESCHOOL_DAYCARE_ONLY,
            PlacementType.TEMPORARY_DAYCARE,
        )

    protected fun supportedFeeDecisionTypes(): Stream<FeeDecisionType> =
        Stream.of(
            FeeDecisionType.NORMAL,
            FeeDecisionType.RELIEF_REJECTED,
            FeeDecisionType.RELIEF_ACCEPTED,
            FeeDecisionType.RELIEF_PARTLY_ACCEPTED,
        )

    protected fun supportedVoucherValueDecisionTypes(): Stream<VoucherValueDecisionType> =
        Stream.of(
            VoucherValueDecisionType.NORMAL,
            VoucherValueDecisionType.RELIEF_REJECTED,
            VoucherValueDecisionType.RELIEF_ACCEPTED,
            VoucherValueDecisionType.RELIEF_PARTLY_ACCEPTED,
        )
}
