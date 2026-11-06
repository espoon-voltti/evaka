// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.dataremoval.DataRetentionOverrides
import evaka.core.dataremoval.TableOverride
import evaka.core.dataremoval.decisionRetentionBeforeArchival
import evaka.core.dataremoval.feeDecisionRetentionBeforeArchival
import evaka.core.dataremoval.voucherValueDecisionRetentionBeforeArchival

/** Espoo archives only child documents, so its decisions do not wait for archival */
val espooDataRetentionOverrides =
    DataRetentionOverrides(
        mapOf(
            "decision" to
                TableOverride(
                    expirationRule = decisionRetentionBeforeArchival,
                    deletedWithParent = true,
                ),
            "fee_decision" to
                TableOverride(
                    expirationRule = feeDecisionRetentionBeforeArchival,
                    deletedWithParent = false,
                ),
            "voucher_value_decision" to
                TableOverride(
                    expirationRule = voucherValueDecisionRetentionBeforeArchival,
                    deletedWithParent = false,
                ),
        )
    )
