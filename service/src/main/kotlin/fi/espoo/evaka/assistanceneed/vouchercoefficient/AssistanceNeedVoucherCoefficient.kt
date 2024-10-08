// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.vouchercoefficient

import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.user.EvakaUser
import java.math.BigDecimal
import org.jdbi.v3.core.mapper.Nested

data class AssistanceNeedVoucherCoefficient(
    val id: AssistanceNeedVoucherCoefficientId,
    val childId: ChildId,
    val coefficient: BigDecimal,
    val validityPeriod: FiniteDateRange,
    val modifiedAt: HelsinkiDateTime,
    @Nested("modified_by") val modifiedBy: EvakaUser?,
)

data class AssistanceNeedVoucherCoefficientRequest(
    val coefficient: Double,
    val validityPeriod: FiniteDateRange,
)

data class AssistanceNeedVoucherCoefficientResponse(
    val voucherCoefficient: AssistanceNeedVoucherCoefficient,
    val permittedActions: Set<Action.AssistanceNeedVoucherCoefficient>,
)
