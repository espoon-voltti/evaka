// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.UploadToKoski
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

internal data class KoskiStudyRightRaw(
    val id: UUID,
    val childId: UUID,
    val unitId: DaycareId,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val version: Int,
    val studyRightOid: String?,
    val personOid: String?,
    val payload: String
)

internal fun Database.Read.getStoredResults() = createQuery("select * from koski_study_right")
    .mapTo<KoskiStudyRightRaw>()
    .toList()

internal fun Database.Transaction.setUnitOids() {
    createUpdate("UPDATE daycare SET oph_unit_oid = :unitOid WHERE daycare.id = :unitId")
        .bind("unitId", testDaycare.id)
        .bind("unitOid", "1.2.246.562.10.1111111111")
        .execute()

    createUpdate("UPDATE daycare SET oph_unit_oid = :unitOid WHERE daycare.id = :unitId")
        .bind("unitId", testDaycare2.id)
        .bind("unitOid", "1.2.246.562.10.2222222222")
        .execute()
}

internal class KoskiTester(private val jdbi: Jdbi, private val client: KoskiClient) {
    fun triggerUploads(today: LocalDate, params: KoskiSearchParams = KoskiSearchParams()) {
        val db = Database(jdbi)
        db.read { it.getPendingStudyRights(today, params) }.forEach { request ->
            client.uploadToKoski(db, UploadToKoski(request), today)
        }
    }
}
