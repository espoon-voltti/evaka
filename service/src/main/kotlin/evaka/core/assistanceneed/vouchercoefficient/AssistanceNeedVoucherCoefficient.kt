// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.assistanceneed.vouchercoefficient

import evaka.core.shared.AssistanceNeedVoucherCoefficientId
import evaka.core.shared.ChildId
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.security.Action
import evaka.core.user.EvakaUser
import java.math.BigDecimal
import org.jdbi.v3.core.mapper.Nested

data class AssistanceNeedVoucherCoefficient(
    val id: AssistanceNeedVoucherCoefficientId,
    val childId: ChildId,
    val coefficient: BigDecimal,
    val validityPeriod: FiniteDateRange,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser,
)

data class AssistanceNeedVoucherCoefficientRequest(
    val coefficient: Double,
    val validityPeriod: FiniteDateRange,
)

data class AssistanceNeedVoucherCoefficientResponse(
    val voucherCoefficient: AssistanceNeedVoucherCoefficient,
    val permittedActions: Set<Action.AssistanceNeedVoucherCoefficient>,
)
