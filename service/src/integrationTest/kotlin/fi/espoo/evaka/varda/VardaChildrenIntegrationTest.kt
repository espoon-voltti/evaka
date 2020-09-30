// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestVardaOrganizer
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class VardaChildrenIntegrationTest : FullApplicationTest() {
    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            insertGeneralTestFixtures(h)
            insertTestVardaOrganizer(h)
            insertTestVardaUnit(h, testDaycare.id)
            uploadChildren(h)
            assertEquals(0, getUploadedChildren(h).size)
        }
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `child is uploaded to Varda and is correctly stored into database after upload`() {
        val child = testChild_1
        val daycare = testDaycare
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = child.id,
                unitId = daycare.id,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )

            uploadChildren(h)

            val uploads = getUploadedChildren(h)
            assertEquals(1, uploads.size)

            val childRow = uploads[0]
            assertNotNull(childRow.id)
            assertEquals(child.id, childRow.personId)
            assertNotNull(childRow.vardaPersonId)
            assertNotNull(childRow.vardaPersonOid)
            assertNotNull(childRow.vardaChildId)
            assertNotNull(childRow.createdAt)
            assertNotNull(childRow.modifiedAt)
            assertNotNull(childRow.uploadedAt)
        }
    }

    @Test
    fun `PAOS child is uploaded to Varda`() {
        jdbi.handle { h ->
            insertTestVardaUnit(h, testPurchasedDaycare.id)
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testPurchasedDaycare.id,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )
            uploadChildren(h)
            assertEquals(1, getUploadedChildren(h).size)
        }
    }

    @Test
    fun `child before varda begin date is not uploaded to Varda`() {
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = getVardaMinDate().minusDays(5),
                endDate = getVardaMinDate().minusDays(1)
            )

            uploadChildren(h)
            assertEquals(0, getUploadedChildren(h).size)
        }
    }

    @Test
    fun `child in a non Varda unit is not uploaded`() {
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
            uploadChildren(h)
            assertEquals(0, getUploadedChildren(h).size)
        }
    }

    @Test
    fun `child already in database is not sent to Varda`() {
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )

            uploadChildren(h)
            val uploadedAt = getUploadedChildren(h)[0].uploadedAt

            uploadChildren(h)
            assertEquals(uploadedAt, getUploadedChildren(h)[0].uploadedAt)
        }
    }

    private fun uploadChildren(h: Handle) {
        updateChildren(h, vardaClient, unitFilter = false)
    }
}

private fun insertTestVardaUnit(h: Handle, id: UUID) {
    // language=sql
    val sql =
        """
        INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, uploaded_at)
        VALUES (:id, 123222, now())
        """.trimIndent()
    h.createUpdate(sql)
        .bind("id", id)
        .execute()

    val sql2 = "UPDATE daycare SET oph_unit_oid = :ophUnitOid WHERE daycare.id = :id;"

    h.createUpdate(sql2)
        .bind("id", id)
        .bind("ophUnitOid", "1.2.3332211")
        .execute()
}

private fun getUploadedChildren(h: Handle): List<VardaChildRow> =
    h.createQuery("SELECT * FROM varda_child")
        .mapTo<VardaChildRow>()
        .list()

data class VardaChildRow(
    val id: UUID,
    val personId: UUID,
    val vardaPersonId: Int,
    val vardaPersonOid: String,
    val vardaChildId: Int?,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val uploadedAt: Instant?
)
