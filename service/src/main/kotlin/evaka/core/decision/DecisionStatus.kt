// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.shared.db.DatabaseEnum

enum class DecisionStatus : DatabaseEnum {
    PENDING,
    ACCEPTED,
    REJECTED;

    override val sqlType: String = "decision_status"
}
