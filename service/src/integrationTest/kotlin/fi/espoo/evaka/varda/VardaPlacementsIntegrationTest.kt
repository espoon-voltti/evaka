// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testDaycare

internal fun insertVardaUnit(
    db: Database.Connection,
    unitId: DaycareId = testDaycare.id,
    unitOid: String? = "1.2.3"
) {
    db.transaction {
        it.execute {
            sql(
                """
INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, created_at, last_success_at)
VALUES (${bind(unitId)}, 1, ${bind(HelsinkiDateTime.now())}, ${bind(HelsinkiDateTime.now())})
"""
            )
        }

        it.execute {
            sql(
                """
UPDATE daycare SET oph_unit_oid = ${bind(unitOid)} WHERE daycare.id = ${bind(unitId)}
"""
            )
        }
    }
}
