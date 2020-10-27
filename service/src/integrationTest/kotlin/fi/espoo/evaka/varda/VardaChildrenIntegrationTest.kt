// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class VardaChildrenIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            insertTestVardaUnit(h, testDaycare.id)
            uploadChildren(h)
            assertEquals(0, getUploadedChildren(h).size)
            mockEndpoint.children.clear()
        }
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
    fun `non-PAOS child is uploaded to Varda with correct payload`() {
        jdbi.handle { h ->
            val testOrganizationOid = defaultMunicipalOrganizerOid
            val daycareId = testDaycare.id
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = daycareId,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )

            h.createUpdate("update daycare set oph_organizer_oid = :oid where id = :id")
                .bind("id", daycareId)
                .bind("oid", testOrganizationOid)
                .execute()

            uploadChildren(h)
            val payload = mockEndpoint.children[0]
            assertEquals(1, getUploadedChildren(h).size)
            assertEquals(defaultMunicipalOrganizerOid, payload.organizerOid)
            assertNull(payload.ownOrganizationOid)
            assertNull(payload.paosOrganizationOid)
        }
    }

    @Test
    fun `PAOS child is uploaded to Varda with correct payload`() {
        jdbi.handle { h ->
            val testOrganizationOid = "1.22.333.4444.55555"
            val daycareId = testPurchasedDaycare.id
            insertTestVardaUnit(h, daycareId)
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = daycareId,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )

            h.createUpdate("update daycare set oph_organizer_oid = :oid where id = :id")
                .bind("id", daycareId)
                .bind("oid", testOrganizationOid)
                .execute()

            uploadChildren(h)
            val payload = mockEndpoint.children[0]
            assertEquals(1, getUploadedChildren(h).size)
            assertNull(payload.organizerOid)
            assertEquals(defaultMunicipalOrganizerOid, payload.ownOrganizationOid)
            assertEquals(testOrganizationOid, payload.paosOrganizationOid)
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
    fun `child is uploaded despite daycare itself is not sent to varda`() {
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
            uploadChildren(h)
            assertEquals(1, getUploadedChildren(h).size)
        }
    }

    @Test
    fun `child is not uploaded if upload_to_varda is false`() {
        jdbi.handle { h ->
            val daycareId = testDaycare2.id
            h.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id")
                .bind("id", daycareId)
                .execute()

            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = daycareId,
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

    @Test
    fun `varda child gets organizer oid from daycare`() {
        jdbi.handle { h ->
            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()

            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )

            uploadChildren(h)
            val organizerOid = getUploadedChildren(h)[0].ophOrganizerOid

            assertEquals("1.22.333.4444.1", organizerOid)
        }
    }

    @Test
    fun `updating daycare organizer oid yields new varda_child`() {
        jdbi.handle { h ->
            insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
            uploadChildren(h)

            assertEquals(1, getUploadedChildren(h).size)

            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()

            uploadChildren(h)

            assertEquals(2, getUploadedChildren(h).size)
        }
    }

    private fun uploadChildren(h: Handle) {
        updateChildren(h, vardaClient, vardaOrganizerName)
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

internal fun getUploadedChildren(h: Handle): List<VardaChildRow> =
    h.createQuery("SELECT * FROM varda_child WHERE uploaded_at IS NOT NULL")
        .mapTo<VardaChildRow>()
        .list()

data class VardaChildRow(
    val id: UUID,
    val personId: UUID,
    val vardaPersonId: Int,
    val vardaPersonOid: String,
    val vardaChildId: Int?,
    val ophOrganizerOid: String?,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val uploadedAt: Instant?
)
