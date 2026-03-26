// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.shared.db.DatabaseEnum

enum class DecisionReasoningCollectionType : DatabaseEnum {
    DAYCARE,
    PRESCHOOL;

    override val sqlType = "decision_reasoning_collection_type"
}
