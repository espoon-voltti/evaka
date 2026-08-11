// SPDX-FileCopyrightText: 2026 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.template

import evaka.core.decision.DecisionType

fun decisionFilename(type: DecisionType): String =
    when (type) {
        DecisionType.CLUB -> "Kerhopäätös"

        DecisionType.DAYCARE,
        DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"

        DecisionType.PRESCHOOL -> "Esiopetuspäätös"

        DecisionType.PRESCHOOL_DAYCARE,
        DecisionType.PRESCHOOL_CLUB -> "Esiopetusta_täydentävän_toiminnan_päätös"

        DecisionType.PREPARATORY_EDUCATION -> "Valmistavan_opetuksen_päätös"
    }
