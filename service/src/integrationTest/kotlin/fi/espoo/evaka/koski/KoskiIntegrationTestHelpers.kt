// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.util.UUID

internal data class KoskiStudyRightRaw(
    val id: UUID,
    val childId: ChildId,
    val unitId: DaycareId,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val version: Int,
    val studyRightOid: String?,
    val personOid: String?,
    val payload: String,
)

internal fun Database.Read.getStoredResults() =
    createQuery { sql("SELECT * FROM koski_study_right") }.toList<KoskiStudyRightRaw>()

internal fun Database.Transaction.setUnitOid(unit: DaycareId, oid: String) = execute {
    sql("UPDATE daycare SET oph_unit_oid = ${bind(oid)} WHERE daycare.id = ${bind(unit)}")
}

internal fun Database.Transaction.setUnitOids() {
    setUnitOid(testDaycare.id, "1.2.246.562.10.1111111111")
    setUnitOid(testDaycare2.id, "1.2.246.562.10.2222222222")
}

internal class KoskiTester(private val db: Database.Connection, private val client: KoskiClient) {
    fun triggerUploads(today: LocalDate) {
        db.read { it.getPendingStudyRights(today) }
            .forEach { request -> client.uploadToKoski(db, AsyncJob.UploadToKoski(request), today) }
    }
}
