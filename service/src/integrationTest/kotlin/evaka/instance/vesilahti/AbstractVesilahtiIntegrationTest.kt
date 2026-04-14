// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti

import evaka.VesilahtiInstance
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.placement.PlacementType
import evaka.trevaka.AbstractIntegrationTest
import java.util.stream.Stream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["integration-test", "vesilahti_evaka"])
@Import(VesilahtiInstance::class)
abstract class AbstractVesilahtiIntegrationTest : AbstractIntegrationTest("vesilahti") {
    @Autowired protected lateinit var properties: VesilahtiProperties

    protected fun supportedDecisionTypes(): Stream<DecisionType> =
        Stream.of(
            DecisionType.DAYCARE,
            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PREPARATORY_EDUCATION,
        )

    protected fun supportedProviderTypes(): Stream<ProviderType> =
        Stream.of(ProviderType.MUNICIPAL, ProviderType.PURCHASED, ProviderType.PRIVATE)

    protected fun supportedPlacementTypes(): Stream<PlacementType> =
        Stream.of(
            PlacementType.DAYCARE,
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
}
