// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testGhostUnitDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class VardaPlacementsIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
        mockEndpoint.cleanUp()
    }

    @Test
    fun `a daycare placement is sent when the corresponding decision has been sent`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        val vardaDecisionId = insertTestVardaDecision(db, decisionId = decisionId)
        val placementId = insertPlacement(db, testChild_1.id, period)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
        assertEquals(placementId, result.first().evakaPlacementId)
        assertEquals(vardaDecisionId, result.first().decisionId)
    }

    @Test
    fun `a daycare placement is sent when the corresponding decision has been sent even if the placement is to a different unit and the decision is to a ghost unit`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period, unitId = testGhostUnitDaycare.id!!)
        val vardaDecisionId = insertTestVardaDecision(db, decisionId = decisionId)
        val placementId = insertPlacement(db, testChild_1.id, period, unitId = testDaycare.id)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
        assertEquals(placementId, result.first().evakaPlacementId)
        assertEquals(vardaDecisionId, result.first().decisionId)
    }

    @Test
    fun `a daycare placement is not sent when the corresponding varda decision is missing`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        insertDecisionWithApplication(db, testChild_1, period)
        insertPlacement(db, testChild_1.id, period)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(0, result.size)
    }

    @Test
    fun `a daycare placement is sent if upload_to_varda flag is true even if the corresponding varda unit is missing`() {
        val daycareId = testDaycare.id
        db.transaction {
            it.createUpdate("update daycare set upload_to_varda = true where id = :id")
                .bind("id", daycareId)
                .execute()
        }
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val decisionId = insertDecisionWithApplication(db, testChild_1, period, unitId = daycareId)

        val vardaUnits = getVardaUnits(db)
        assertNull(vardaUnits.find { it.evakaDaycareId == daycareId })

        insertTestVardaDecision(db, decisionId = decisionId)
        insertPlacement(db, testChild_1.id, period, unitId = daycareId)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
    }

    @Test
    fun `a daycare placement is not sent when starts in the future`() {
        val period = ClosedPeriod(LocalDate.now().plusMonths(1), LocalDate.now().plusYears(1))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        insertTestVardaDecision(db, decisionId = decisionId)
        insertPlacement(db, testChild_1.id, period)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(0, result.size)
    }

    @Test
    fun `a preschool placement is not sent`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId =
            insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        insertTestVardaDecision(db, decisionId = decisionId)
        insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(0, result.size)
    }

    @Test
    fun `a preschool daycare placement is not sent`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId =
            insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        val vardaDecisionId = insertTestVardaDecision(db, decisionId = decisionId)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
        assertEquals(placementId, result.first().evakaPlacementId)
        assertEquals(vardaDecisionId, result.first().decisionId)
    }

    @Test
    fun `multiple daycare placements are all sent`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        val vardaDecisionId = insertTestVardaDecision(db, decisionId = decisionId)
        val placementId1 = insertPlacement(db, testChild_1.id, period.copy(end = period.start.plusDays(5)))
        val placementId2 = insertPlacement(
            db,
            testChild_1.id,
            period.copy(start = period.start.plusDays(6), end = period.start.plusDays(10))
        )
        val placementId3 = insertPlacement(db, testChild_1.id, period.copy(start = period.start.plusDays(11)))

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(3, result.size)
        assertTrue(result.any { p -> p.evakaPlacementId == placementId1 })
        assertTrue(result.any { p -> p.evakaPlacementId == placementId2 })
        assertTrue(result.any { p -> p.evakaPlacementId == placementId3 })
        assertTrue(result.all { p -> p.decisionId == vardaDecisionId })
    }

    @Test
    fun `varda placement is deleted when the original placement is deleted`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        insertTestVardaDecision(db, decisionId = decisionId)
        insertPlacement(db, testChild_1.id, period)

        updatePlacements(db, vardaClient)

        val beforeDelete = getVardaPlacements(db)
        assertEquals(1, beforeDelete.size)

        db.transaction { it.createUpdate("DELETE FROM placement").execute() }
        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(0, result.size)
    }

    @Test
    fun `varda placement is deleted when the original placement is deleted with multiple placements`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        insertTestVardaDecision(db, decisionId = decisionId)
        val placementId = insertPlacement(db, testChild_1.id, period.copy(end = period.start.plusDays(5)))
        insertPlacement(
            db,
            testChild_1.id,
            period.copy(start = period.start.plusDays(6), end = period.start.plusDays(10))
        )
        insertPlacement(db, testChild_1.id, period.copy(start = period.start.plusDays(11)))

        updatePlacements(db, vardaClient)

        val beforeDelete = getVardaPlacements(db)
        assertEquals(3, beforeDelete.size)

        db.transaction { it.createUpdate("DELETE FROM placement WHERE id = :id").bind("id", placementId).execute() }
        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(2, result.size)
        assertTrue(result.none { it.evakaPlacementId == placementId })
    }

    @Test
    fun `varda placement is updated when a placement is updated`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        insertTestVardaDecision(db, decisionId = decisionId)
        val placementId = insertPlacement(db, testChild_1.id, period)

        updatePlacements(db, vardaClient)
        val originalUploadedAt = getVardaPlacements(db).first().uploadedAt

        updatePlacement(db, placementId, originalUploadedAt.plusSeconds(1))
        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
        assertTrue(originalUploadedAt < result.first().uploadedAt)
    }

    @Test
    fun `a daycare placement is sent when a varda decision has been derived from it`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertTestVardaDecision(db, placementId = placementId)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(1, result.size)
        assertEquals(placementId, result.first().evakaPlacementId)
    }

    @Test
    fun `a daycare placement is not sent when varda_unit has no OID`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db, unitOid = null)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertTestVardaDecision(db, placementId = placementId)

        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(0, result.size)
    }

    @Test
    fun `placement is soft deleted if it is flagged with should_be_deleted`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertTestVardaDecision(db, placementId = placementId)

        updatePlacements(db, vardaClient)

        assertEquals(1, getVardaPlacements(db).size)
        assertEquals(0, getSoftDeletedVardaPlacements(db).size)

        db.transaction { it.createUpdate("UPDATE varda_placement SET should_be_deleted = true").execute() }

        removeMarkedPlacementsFromVarda(db, vardaClient)
        updatePlacements(db, vardaClient)

        assertEquals(1, getSoftDeletedVardaPlacements(db).size)
    }

    @Test
    fun `placement is not updated if upload flag is turned off`() {
        val unitId = testDaycare.id
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertVardaUnit(db, unitId = unitId)
        val placementId = insertPlacement(db, testChild_1.id, unitId = unitId, period = period)
        insertTestVardaDecision(db, placementId = placementId)

        updatePlacements(db, vardaClient)

        db.transaction { it.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id").bind("id", testDaycare.id).execute() }

        val originalUploadedAt = getVardaPlacements(db).first().uploadedAt

        updatePlacement(db, placementId, originalUploadedAt.plusSeconds(1))
        updatePlacements(db, vardaClient)

        val result = getVardaPlacements(db)
        assertEquals(originalUploadedAt, result.first().uploadedAt)
    }

    @Test
    fun `updating daycare organizer oid yields new varda placement if old is soft deleted`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

        insertPlacementWithDecision(db, child = testChild_1, unitId = testDaycare.id, period = period)

        updateChildren(db, vardaClient, vardaOrganizerName)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)

        assertEquals(1, getVardaPlacements(db).size)

        db.transaction {
            it.createUpdate("update varda_decision set deleted_at = NOW()").execute()
            it.createUpdate("update varda_placement set deleted_at = NOW()").execute()
            it.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        updateChildren(db, vardaClient, vardaOrganizerName)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)

        assertEquals(2, getVardaPlacements(db).size)
    }
}

private fun getSoftDeletedVardaPlacements(db: Database.Connection) = db.read {
    it.createQuery("SELECT * FROM varda_placement WHERE deleted_at IS NOT NULL")
        .map(toVardaPlacementRow)
        .toList()
}

internal fun getVardaPlacements(db: Database.Connection) = db.read {
    it.createQuery("SELECT * FROM varda_placement")
        .map(toVardaPlacementRow)
        .toList()
}

internal fun insertVardaUnit(db: Database.Connection, unitId: UUID = testDaycare.id, unitOid: String? = "1.2.3") {
    db.transaction {
        it
            .createUpdate(
                """
INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, created_at, uploaded_at)
VALUES (:evakaDaycareId, :vardaUnitId,  :createdAt, :uploadedAt)
                """.trimIndent()
            )
            .bind("evakaDaycareId", unitId)
            .bind("vardaUnitId", 1L)
            .bind("ophUnitOid", unitOid)
            .bind("createdAt", Instant.now())
            .bind("uploadedAt", Instant.now())
            .execute()

        it.createUpdate("UPDATE daycare SET oph_unit_oid = :unitOid WHERE daycare.id = :unitId")
            .bind("unitId", unitId)
            .bind("unitOid", unitOid)
            .execute()
    }
}

internal fun insertTestVardaDecision(db: Database.Connection, decisionId: UUID? = null, placementId: UUID? = null): UUID {
    val id = UUID.randomUUID()
    db.transaction {
        insertVardaDecision(
            it,
            VardaDecisionTableRow(
                id = id,
                vardaDecisionId = 123L,
                evakaDecisionId = decisionId,
                evakaPlacementId = placementId,
                createdAt = Instant.now(),
                uploadedAt = Instant.now()
            )
        )
    }
    return id
}

internal fun insertPlacement(
    db: Database.Connection,
    childId: UUID,
    period: ClosedPeriod,
    type: PlacementType = DAYCARE,
    unitId: UUID = testDaycare.id
): UUID {
    return db.transaction {
        insertTestPlacement(
            it.handle,
            childId = childId,
            type = type,
            startDate = period.start,
            endDate = period.end,
            unitId = unitId
        )
    }
}

internal fun updatePlacement(db: Database.Connection, id: UUID, updatedAt: Instant) {
    db.transaction {
        it.createUpdate("UPDATE placement SET updated = :updatedAt WHERE id = :id")
            .bind("id", id)
            .bind("updatedAt", updatedAt)
            .execute()
    }
}
