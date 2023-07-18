// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class DecisionType : DatabaseEnum {
    CLUB,
    DAYCARE,
    DAYCARE_PART_TIME,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    PRESCHOOL_CLUB,
    PREPARATORY_EDUCATION;

    override val sqlType = "decision_type"
}
