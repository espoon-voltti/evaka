// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.dataremoval.DataRetentionOverrides
import evaka.core.dataremoval.DateSource.GraphTableColumn
import evaka.core.dataremoval.DateSource.OwnColumn
import evaka.core.dataremoval.ExpirationRule.After
import evaka.core.dataremoval.ExpirationRule.Always
import evaka.core.dataremoval.ExpirationRule.Coalesce
import evaka.core.dataremoval.TableOverride
import evaka.core.dataremoval.feeDecisionChildrenPlacementOrValidityEnd
import java.time.Period

private val tenYears = Period.ofYears(10)
private val lastPlacementEnd = GraphTableColumn("placement", "end_date")

/** Espoo archives only child documents, so its decisions do not wait for archival */
val espooDataRetentionOverrides =
    DataRetentionOverrides(
        mapOf(
            "decision" to
                TableOverride(
                    expirationRule =
                        Coalesce(
                            After(tenYears, lastPlacementEnd),
                            After(tenYears, OwnColumn("sent_date")),
                            Always,
                        ),
                    deletedWithParent = true,
                ),
            "fee_decision" to
                TableOverride(
                    expirationRule = After(tenYears, feeDecisionChildrenPlacementOrValidityEnd),
                    deletedWithParent = false,
                ),
            "voucher_value_decision" to
                TableOverride(
                    expirationRule =
                        Coalesce(
                            After(tenYears, lastPlacementEnd),
                            After(tenYears, OwnColumn("valid_to")),
                        ),
                    deletedWithParent = false,
                ),
        )
    )
