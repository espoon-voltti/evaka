// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
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
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
        insertTestVardaUnit(db, testDaycare.id)
        uploadChildren()
        assertEquals(0, getUploadedChildren(db).size)
        mockEndpoint.children.clear()
        mockEndpoint.cleanUp()
    }

    @Test
    fun `child is uploaded to Varda and is correctly stored into database after upload`() {
        val child = testChild_1
        val daycare = testDaycare
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )
        }

        uploadChildren()

        val uploads = getUploadedChildren(db)
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

    @Test
    fun `non-PAOS child is uploaded to Varda with correct payload`() {
        val testOrganizationOid = defaultMunicipalOrganizerOid
        val daycareId = testDaycare.id
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = daycareId,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )

            tx.createUpdate("update daycare set oph_organizer_oid = :oid where id = :id")
                .bind("id", daycareId)
                .bind("oid", testOrganizationOid)
                .execute()
        }

        uploadChildren()
        val payload = mockEndpoint.children.values.first()
        assertEquals(1, getUploadedChildren(db).size)
        assertEquals(defaultMunicipalOrganizerOid, payload.organizerOid)
        assertNull(payload.ownOrganizationOid)
        assertNull(payload.paosOrganizationOid)
    }

    @Test
    fun `PAOS child is uploaded to Varda with correct payload`() {
        val testOrganizationOid = "1.22.333.4444.55555"
        val daycareId = testPurchasedDaycare.id
        insertTestVardaUnit(db, daycareId)
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = daycareId,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )

            tx.createUpdate("update daycare set oph_organizer_oid = :oid where id = :id")
                .bind("id", daycareId)
                .bind("oid", testOrganizationOid)
                .execute()
        }

        uploadChildren()
        val payload = mockEndpoint.children.values.first()
        assertEquals(1, getUploadedChildren(db).size)
        assertNull(payload.organizerOid)
        assertEquals(defaultMunicipalOrganizerOid, payload.ownOrganizationOid)
        assertEquals(testOrganizationOid, payload.paosOrganizationOid)
    }

    @Test
    fun `child before varda begin date is not uploaded to Varda`() {
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = getVardaMinDate().minusDays(5),
                endDate = getVardaMinDate().minusDays(1)
            )
        }
        uploadChildren()
        assertEquals(0, getUploadedChildren(db).size)
    }

    @Test
    fun `child is uploaded despite daycare itself is not sent to varda`() {
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare2.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
        }
        uploadChildren()
        assertEquals(1, getUploadedChildren(db).size)
    }

    @Test
    fun `child is not uploaded if upload_to_varda is false`() {
        db.transaction { tx ->
            val daycareId = testDaycare2.id
            tx.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id")
                .bind("id", daycareId)
                .execute()

            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = daycareId,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
        }
        uploadChildren()
        assertEquals(0, getUploadedChildren(db).size)
    }

    @Test
    fun `child already in database is not sent to Varda`() {
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
        }
        uploadChildren()
        val uploadedAt = getUploadedChildren(db)[0].uploadedAt

        uploadChildren()
        assertEquals(uploadedAt, getUploadedChildren(db)[0].uploadedAt)
    }

    @Test
    fun `varda child gets organizer oid from daycare`() {
        db.transaction { tx ->
            tx.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()

            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
        }

        uploadChildren()
        val organizerOid = getUploadedChildren(db)[0].ophOrganizerOid

        assertEquals("1.22.333.4444.1", organizerOid)
    }

    @Test
    fun `updating daycare organizer oid yields new varda_child`() {
        db.transaction { tx ->
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusMonths(2),
                endDate = LocalDate.now().plusMonths(1)
            )
        }
        uploadChildren()

        assertEquals(1, getUploadedChildren(db).size)

        db.transaction { tx ->
            tx.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        uploadChildren()

        assertEquals(2, getUploadedChildren(db).size)
    }

    @Test
    fun `nameless child is not uploaded to Varda`() {
        val child = namelessChild
        val daycare = testDaycare
        db.transaction { tx ->
            tx.insertTestPerson(
                DevPerson(
                    id = child.id,
                    dateOfBirth = child.dateOfBirth,
                    ssn = child.ssn,
                    firstName = child.firstName,
                    lastName = child.lastName,
                    streetAddress = child.streetAddress ?: "",
                    postalCode = child.postalCode ?: "",
                    postOffice = child.postOffice ?: ""
                )
            )
            tx.insertTestChild(DevChild(id = child.id))

            tx.insertTestPlacement(
                childId = child.id,
                unitId = daycare.id,
                startDate = LocalDate.now().minusMonths(1),
                endDate = LocalDate.now().plusMonths(1)
            )
        }

        uploadChildren()

        val uploads = getUploadedChildren(db)
        assertEquals(0, uploads.size)
    }

    private fun uploadChildren() {
        updateChildren(db, vardaClient, vardaOrganizerName)
    }
}

private fun insertTestVardaUnit(db: Database.Connection, id: UUID) = db.transaction {
    // language=sql
    val sql =
        """
        INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, uploaded_at)
        VALUES (:id, 123222, now())
        """.trimIndent()
    it.createUpdate(sql)
        .bind("id", id)
        .execute()

    val sql2 = "UPDATE daycare SET oph_unit_oid = :ophUnitOid WHERE daycare.id = :id;"

    it.createUpdate(sql2)
        .bind("id", id)
        .bind("ophUnitOid", "1.2.3332211")
        .execute()
}

internal fun getUploadedChildren(db: Database.Connection): List<VardaChildRow> = db.read {
    it.createQuery("SELECT * FROM varda_child WHERE uploaded_at IS NOT NULL")
        .mapTo<VardaChildRow>()
        .list()
}

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

val namelessChild = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2017, 6, 1),
    ssn = "311219A999T",
    firstName = "",
    lastName = "",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)
