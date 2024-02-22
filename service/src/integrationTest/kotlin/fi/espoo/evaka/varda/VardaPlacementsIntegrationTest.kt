// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.testDaycare
import java.time.Instant

internal fun insertVardaUnit(
    db: Database.Connection,
    unitId: DaycareId = testDaycare.id,
    unitOid: String? = "1.2.3"
) {
    db.transaction {
        @Suppress("DEPRECATION")
        it.createUpdate(
                """
INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, created_at, uploaded_at)
VALUES (:evakaDaycareId, :vardaUnitId,  :createdAt, :uploadedAt)
                """
                    .trimIndent()
            )
            .bind("evakaDaycareId", unitId)
            .bind("vardaUnitId", 1L)
            .bind("ophUnitOid", unitOid)
            .bind("createdAt", Instant.now())
            .bind("uploadedAt", Instant.now())
            .execute()

        @Suppress("DEPRECATION")
        it.createUpdate("UPDATE daycare SET oph_unit_oid = :unitOid WHERE daycare.id = :unitId")
            .bind("unitId", unitId)
            .bind("unitOid", unitOid)
            .execute()
    }
}
