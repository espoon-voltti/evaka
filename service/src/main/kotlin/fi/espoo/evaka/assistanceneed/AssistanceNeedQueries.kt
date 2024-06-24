// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getCapacityFactorsByChild(childId: ChildId): List<AssistanceNeedCapacityFactor> =
    createQuery {
        sql(
            """
SELECT valid_during AS date_range, capacity_factor
FROM assistance_factor
WHERE child_id = ${bind(childId)}
"""
        )
    }.toList<AssistanceNeedCapacityFactor>()
