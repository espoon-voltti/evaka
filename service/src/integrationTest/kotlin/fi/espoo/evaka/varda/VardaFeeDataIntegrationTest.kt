// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createFeeDecisionPartFixture
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class VardaFeeDataIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var personService: PersonService

    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        db.transaction { insertGeneralTestFixtures(it.handle) }
        insertVardaUnit(db)
        assertEquals(0, getVardaFeeDataRows(db).size)
        mockEndpoint.feeData.clear()
    }

    @AfterEach
    fun afterEach() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `fee data is sent when placement is sent to Varda`() {
        createDecisionsAndPlacements(child = testChild_1)
        insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)

        updateFeeData()

        assertEquals(1, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is saved to database correctly after upload`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
        insertDecisionWithApplication(db, testChild_1, period)
        insertServiceNeed(db, testChild_1.id, period)
        insertVardaChild(db, testChild_1.id)
        val placementId = db.transaction {
            insertTestPlacement(
                it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end
            )
        }
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)

        val feeDecision = insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )

        updateFeeData()
        val feeDataRows = getVardaFeeDataRows(db)
        assertEquals(1, feeDataRows.size)
        assertEquals(feeDecision.id, feeDataRows[0].evakaFeeDecisionId)
        assertEquals(placementId, feeDataRows[0].evakaPlacementId)
        assertNotNull(feeDataRows[0].vardaFeeDataId)
        assertNotNull(feeDataRows[0].createdAt)
        assertNotNull(feeDataRows[0].uploadedAt)
    }

    @Test
    fun `fee data is mapped correctly`() {
        createDecisionsAndPlacements(child = testChild_1)
        val feeDecision = insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            placementType = PlacementType.DAYCARE
        )

        updateFeeData()

        val results = mockEndpoint.feeData
        assertEquals(1, results.size)

        val result = results[0]
        assertEquals("MP03", result.feeCode)
        assertEquals(0.0, result.voucherAmount)
        assertEquals(feeDecision.familySize, result.familySize)
        assertEquals(289.0, result.feeAmount)
        assertEquals(feeDecision.validFrom, result.startDate)
        assertEquals(feeDecision.validTo, result.endDate)
    }

    @Test
    fun `five year old daycare is mapped correctly`() {
        createDecisionsAndPlacements(child = testChild_1)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            placementType = PlacementType.FIVE_YEARS_OLD_DAYCARE
        )

        updateFeeData()

        val results = mockEndpoint.feeData
        assertEquals(1, results.size)

        assertEquals("MP02", results[0].feeCode)
    }

    @Test
    fun `fee decision with two children generates two fee data entries`() {
        createDecisionsAndPlacements(testChild_1)
        createDecisionsAndPlacements(testChild_2)
        insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(testChild_1, testChild_2), objectMapper)

        updateFeeData()

        assertEquals(2, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is not sent when there is no placement sent to Varda`() {
        createDecisionsAndPlacements(testChild_1)
        insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)
        clearVardaPlacements(db)

        updateFeeData()

        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is not sent when the fee decision is annulled`() {
        createDecisionsAndPlacements(testChild_1)
        insertFeeDecision(db, FeeDecisionStatus.ANNULLED, listOf(testChild_1), objectMapper)

        updateFeeData()

        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is not sent when the fee decision is draft`() {
        createDecisionsAndPlacements(testChild_1)
        val sentFeeDecision = insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)

        updateFeeData()

        insertFeeDecision(db, FeeDecisionStatus.DRAFT, listOf(testChild_1), objectMapper)
        db.transaction {
            it.createUpdate("UPDATE placement SET updated = now() + interval '30 second' WHERE 1 = 1").execute()
        }

        updateFeeData()

        val feeDataRows = getVardaFeeDataRows(db)
        assertEquals(1, feeDataRows.size)
        assertEquals(sentFeeDecision.id, feeDataRows[0].evakaFeeDecisionId)
    }

    @Test
    fun `child with no guardians is not sent to Varda`() {
        // testChild_3 doesn't have guardians in mock VTJ data
        createDecisionsAndPlacements(child = testChild_3)
        insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(testChild_3), objectMapper)

        updateFeeData()

        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is sent and updated to database only once`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
        createDecisionsAndPlacements(child = testChild_1, period = period)

        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )

        updateFeeData()
        val originalEntry = getVardaFeeDataRows(db)[0]

        updateFeeData()
        updateFeeData()
        updateFeeData()
        updateFeeData()

        val finalEntry = getVardaFeeDataRows(db)[0]
        assertEquals(1, mockEndpoint.feeData.size)
        assertEquals(1, getVardaFeeDataRows(db).size)
        assertEquals(originalEntry, finalEntry)
    }

    @Test
    fun `child with various decisions in Varda is handled correctly`() {
        val period1 = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
        val period2 = ClosedPeriod(period1.end.plusMonths(1), LocalDate.now().plusMonths(6))
        val child = testChild_1
        createDecisionsAndPlacements(child = child, period = period1)

        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period1.start, period1.end)
        )

        updateFeeData()
        assertEquals(1, getVardaFeeDataRows(db).size)

        db.transaction {
            insertTestPlacement(
                it.handle,
                childId = child.id,
                unitId = testDaycare.id,
                startDate = period2.start,
                endDate = period2.end
            )
        }
        insertServiceNeed(db, child.id, period2)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period2.start, period2.end)
        )

        updateFeeData()
        assertEquals(2, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `child with decision to municipal and purchased units is handled correctly`() {
        val period1 = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
        val period2 = ClosedPeriod(period1.end.plusMonths(1), LocalDate.now().plusMonths(6))

        val child = testChild_1

        val paosDaycareId = testPurchasedDaycare.id

        updateUnits(db, vardaClient, vardaOrganizerName)

        createDecisionsAndPlacements(child = child, period = period1, daycareId = paosDaycareId)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(child),
            objectMapper = objectMapper,
            period = Period(period1.start, period1.end),
            daycareId = paosDaycareId
        )
        updateFeeData()
        assertEquals(1, getVardaFeeDataRows(db).size)

        val daycareId = testDaycare.id
        createDecisionsAndPlacements(child = child, period = period2, daycareId = daycareId)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(child),
            objectMapper = objectMapper,
            period = Period(period2.start, period2.end),
            daycareId = daycareId
        )
        updateFeeData()

        assertEquals(2, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `two fee data entries is sent with two placements inside one fee decision`() {
        /*
            Fee decision |--------------------|
            Placement       |-------|  |--|
            Fee data        |-------|  |--|
        */
        val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
        val firstPlacementPeriod = ClosedPeriod(decisionPeriod.start.plusMonths(1), decisionPeriod.start.plusMonths(2))
        val secondPlacementPeriod = ClosedPeriod(firstPlacementPeriod.end.plusMonths(1), decisionPeriod.end.minusMonths(1))
        insertDecisionWithApplication(db, testChild_1, decisionPeriod)
        insertServiceNeed(db, testChild_1.id, decisionPeriod)
        insertVardaChild(db, testChild_1.id, Instant.now().minusSeconds(60 * 60 * 24))
        db.transaction {
            insertTestPlacement(it.handle, childId = testChild_1.id, unitId = testDaycare.id, startDate = firstPlacementPeriod.start, endDate = firstPlacementPeriod.end)
            insertTestPlacement(it.handle, childId = testChild_1.id, unitId = testDaycare.id, startDate = secondPlacementPeriod.start, endDate = secondPlacementPeriod.end)
        }
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(decisionPeriod.start, decisionPeriod.end)
        )
        updateFeeData()

        assertEquals(2, getVardaFeeDataRows(db).size)
        assertEquals(2, getVardaPlacements(db).size)

        val uploadedFeeData: List<VardaFeeData> = mockEndpoint.feeData.sortedBy { data -> data.startDate }
        assertEquals(firstPlacementPeriod.start, uploadedFeeData[0].startDate)
        assertEquals(firstPlacementPeriod.end, uploadedFeeData[0].endDate)
        assertEquals(secondPlacementPeriod.start, uploadedFeeData[1].startDate)
        assertEquals(secondPlacementPeriod.end, uploadedFeeData[1].endDate)
    }

    @Test
    fun `two different fee data entries are sent when placement has two fee decisions`() {
        /*
            Fee decision |-----------||-----------|
            Placement       |----------------|
            Fee data        |--------||------|
        */
        val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
        val feeDecisionsPeriod1 = ClosedPeriod(decisionPeriod.start, decisionPeriod.start.plusMonths(2))
        val feeDecisionPeriod2 = ClosedPeriod(feeDecisionsPeriod1.end.plusMonths(1), decisionPeriod.end)
        val placementPeriod = ClosedPeriod(feeDecisionsPeriod1.start.plusMonths(1), feeDecisionPeriod2.end.minusMonths(1))

        insertDecisionWithApplication(db, testChild_1, decisionPeriod)
        insertServiceNeed(db, testChild_1.id, decisionPeriod)
        insertVardaChild(db, testChild_1.id)
        db.transaction {
            insertTestPlacement(it.handle, childId = testChild_1.id, unitId = testDaycare.id, startDate = placementPeriod.start, endDate = placementPeriod.end)
        }
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(feeDecisionsPeriod1.start, feeDecisionsPeriod1.end)
        )
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1, testChild_2),
            objectMapper = objectMapper,
            period = Period(feeDecisionPeriod2.start, feeDecisionPeriod2.end)
        )

        updateFeeData()
        assertEquals(2, getVardaFeeDataRows(db).size)
        assertEquals(1, getVardaPlacements(db).size)

        val uploadedFeeData: List<VardaFeeData> = mockEndpoint.feeData.sortedBy { data -> data.startDate }
        assertEquals(placementPeriod.start, uploadedFeeData[0].startDate)
        assertEquals(feeDecisionsPeriod1.end, uploadedFeeData[0].endDate)
        assertEquals(2, uploadedFeeData[0].familySize)

        assertEquals(feeDecisionPeriod2.start, uploadedFeeData[1].startDate)
        assertEquals(placementPeriod.end, uploadedFeeData[1].endDate)
        assertEquals(3, uploadedFeeData[1].familySize)
    }

    @Test
    fun `delete fee data entry if its placement is removed`() {
        createDecisionsAndPlacements()
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        )
        updateFeeData()

        assertEquals(1, getVardaPlacements(db).size)
        assertEquals(1, getVardaFeeDataRows(db).size)

        clearPlacements(db)
        updatePlacements(db, vardaClient)
        updateFeeData()

        assertEquals(0, getVardaPlacements(db).size)
        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `delete all fee data entries of annulled fee decisions`() {
        val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
        val firstPlacementPeriod = ClosedPeriod(decisionPeriod.start.plusMonths(1), decisionPeriod.start.plusMonths(2))
        val secondPlacementPeriod = ClosedPeriod(firstPlacementPeriod.end.plusMonths(1), decisionPeriod.end.minusMonths(1))
        insertDecisionWithApplication(db, testChild_1, decisionPeriod)
        insertServiceNeed(db, testChild_1.id, decisionPeriod)
        insertVardaChild(db, testChild_1.id, Instant.now().minusSeconds(60 * 60 * 24))
        db.transaction {
            insertTestPlacement(it.handle, childId = testChild_1.id, unitId = testDaycare.id, startDate = secondPlacementPeriod.start, endDate = secondPlacementPeriod.end)
            insertTestPlacement(it.handle, childId = testChild_1.id, unitId = testDaycare.id, startDate = firstPlacementPeriod.start, endDate = firstPlacementPeriod.end)
        }
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(decisionPeriod.start, decisionPeriod.end)
        )
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        updateFeeData()

        assertEquals(2, getVardaFeeDataRows(db).size)

        setFeeDecisionsAnnulled(db)
        updateFeeData()

        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `deleting legacy fee data works`() {
        val feeDecision = insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        )
        db.transaction {
            it.createUpdate(
                """
                INSERT INTO varda_fee_data (evaka_fee_decision_id, varda_fee_data_id, evaka_placement_id)
                VALUES (:feeDecisionId, 12333, NULL)
                """
            )
                .bind("feeDecisionId", feeDecision.id)
                .execute()
        }

        assertEquals(1, getVardaFeeDataRows(db).size)
        updateFeeData()
        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `modify fee data entry when placement is modified`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )
        updateFeeData()

        assertEquals(1, getVardaFeeDataRows(db).size)
        val originalFeeData = mockEndpoint.feeData[0]
        assertEquals(period.start, originalFeeData.startDate)
        assertEquals(period.end, originalFeeData.endDate)

        val newStart = period.start.plusDays(1)
        val newEnd = period.end.minusDays(1)
        db.transaction {
            it.createUpdate("UPDATE placement SET start_date = :newStart, end_date = :newEnd WHERE 1 = 1")
                .bind("newStart", newStart)
                .bind("newEnd", newEnd)
                .execute()
        }

        updateFeeData()

        val newFeeData = mockEndpoint.feeData[0]
        assertNotEquals(originalFeeData.startDate, newFeeData.startDate)
        assertNotEquals(originalFeeData.endDate, newFeeData.endDate)
        assertEquals(newStart, newFeeData.startDate)
        assertEquals(newEnd, newFeeData.endDate)
    }

    @Test
    fun `modifying fee data updates the timestamp`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )
        updateFeeData()

        assertEquals(1, getVardaFeeDataRows(db).size)
        val originalEntry = getVardaFeeDataRows(db)[0]

        val newStart = period.start.plusDays(1)
        db.transaction {
            it.createUpdate("UPDATE placement SET start_date = :newStart WHERE 1 = 1")
                .bind("newStart", newStart)
                .execute()
        }

        updateFeeData()

        val finalEntry = getVardaFeeDataRows(db)[0]
        assertNotEquals(originalEntry, finalEntry)
        assertTrue(originalEntry.uploadedAt < finalEntry.uploadedAt)
    }

    @Test
    fun `fee data is soft deleted if it is flagged with should_be_deleted`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )
        updateFeeData()

        assertEquals(1, getVardaFeeDataRows(db).size)
        assertEquals(0, getSoftDeletedVardaFeeData(db).size)

        db.transaction { it.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute() }

        removeMarkedFeeDataFromVarda(db, vardaClient)
        assertEquals(1, getSoftDeletedVardaFeeData(db).size)
    }

    @Test
    fun `fee_data is not updated if upload flag is turned off`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )

        updateAll()

        assertEquals(1, mockEndpoint.feeData.size)

        db.transaction {
            it.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id").bind("id", testDaycare.id).execute()
            it.createUpdate("UPDATE placement SET start_date = :newStart")
                .bind("newStart", period.start.minusMonths(1))
                .execute()
        }

        updateAll()

        val feeData = mockEndpoint.feeData
        assertEquals(1, feeData.size)
        assertEquals(period.start, feeData[0].startDate)
    }

    @Test
    fun `updating daycare organizer oid yields new varda fee data row if old is soft deleted`() {
        val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )

        updateAll()

        assertEquals(1, getVardaFeeDataRows(db).size)

        db.transaction {
            it.createUpdate("update varda_fee_data set should_be_deleted = true, deleted_at = NOW()").execute()
            it.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        updateAll()

        assertEquals(2, getVardaFeeDataRows(db).size)
    }

    @Test
    fun `fee data is sent once if it's soft deleted`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        createDecisionsAndPlacements(period = period)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )
        updateAll()

        assertEquals(1, getVardaFeeDataRows(db).size)
        assertEquals(0, getSoftDeletedVardaFeeData(db).size)

        db.transaction {
            it.createUpdate("UPDATE varda_placement SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id").bind("id", testDaycare.id).execute()
        }

        removeMarkedPlacementsFromVarda(db, vardaClient)
        removeMarkedDecisionsFromVarda(db, vardaClient)
        removeMarkedFeeDataFromVarda(db, vardaClient)
        mockEndpoint.feeData.clear()

        updateAll()
        assertEquals(2, getVardaFeeDataRows(db).size)
        assertEquals(1, getSoftDeletedVardaFeeData(db).size)
        assertEquals(1, mockEndpoint.feeData.size)

        updateAll()
        assertEquals(2, getVardaFeeDataRows(db).size)
        assertEquals(1, mockEndpoint.feeData.size)
    }

    @Test
    fun `fee data is not sent multiple times for multiple fee decisions`() {
        val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
        val period2 = ClosedPeriod(period.start.minusMonths(6), period.start.minusMonths(1))
        createDecisionsAndPlacements(period = period)
        createDecisionsAndPlacements(period = period2)
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period.start, period.end)
        )
        insertFeeDecision(
            db,
            status = FeeDecisionStatus.SENT,
            children = listOf(testChild_1),
            objectMapper = objectMapper,
            period = Period(period2.start, period2.end)
        )
        updateAll()

        assertEquals(2, getVardaFeeDataRows(db).size)
        assertEquals(0, getSoftDeletedVardaFeeData(db).size)

        db.transaction {
            it.createUpdate("UPDATE varda_placement SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute()
            it.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id").bind("id", testDaycare.id).execute()
        }

        removeMarkedPlacementsFromVarda(db, vardaClient)
        removeMarkedDecisionsFromVarda(db, vardaClient)
        removeMarkedFeeDataFromVarda(db, vardaClient)
        mockEndpoint.feeData.clear()

        updateAll()
        assertEquals(4, getVardaFeeDataRows(db).size)
        assertEquals(2, getSoftDeletedVardaFeeData(db).size)
        assertEquals(2, mockEndpoint.feeData.size)

        updateAll()
        assertEquals(4, getVardaFeeDataRows(db).size)
        assertEquals(2, mockEndpoint.feeData.size)
    }

    @Test
    fun `child whose guardians has no name is not sent to Varda`() {
        db.transaction {
            it.handle.insertTestPerson(
                DevPerson(
                    id = childWithNamelessParent.id,
                    dateOfBirth = childWithNamelessParent.dateOfBirth,
                    ssn = childWithNamelessParent.ssn,
                    firstName = childWithNamelessParent.firstName,
                    lastName = childWithNamelessParent.lastName,
                    streetAddress = childWithNamelessParent.streetAddress!!,
                    postalCode = childWithNamelessParent.postalCode!!,
                    postOffice = childWithNamelessParent.postOffice!!
                )
            )
            it.handle.insertTestChild(DevChild(id = childWithNamelessParent.id))
        }

        // Niilo Nimettömänpoika's guardian doesn't have a name in mock VTJ data
        createDecisionsAndPlacements(child = childWithNamelessParent)
        insertFeeDecision(db, FeeDecisionStatus.SENT, listOf(childWithNamelessParent), objectMapper)

        updateFeeData()

        assertEquals(0, getVardaFeeDataRows(db).size)
    }

    private fun updateAll() {
        updateChildren(db, vardaClient, vardaOrganizerName)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        updateFeeData()
    }

    private fun updateFeeData() {
        updateFeeData(db, vardaClient, objectMapper, personService)
    }

    private fun createDecisionsAndPlacements(
        child: PersonData.Detailed = testChild_1,
        period: ClosedPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
        daycareId: UUID = testDaycare.id
    ) {
        insertDecisionWithApplication(db, child, period, unitId = daycareId)
        insertServiceNeed(db, child.id, period)
        db.transaction {
            insertTestPlacement(
                h = it.handle,
                childId = child.id,
                unitId = daycareId,
                startDate = period.start,
                endDate = period.end
            )
        }
        updateChildren(db, vardaClient, organizerName = vardaOrganizerName)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
    }
}

private fun getSoftDeletedVardaFeeData(db: Database.Connection): List<VardaFeeDataRow> = db.read {
    it.createQuery("SELECT * FROM varda_fee_data WHERE deleted_at IS NOT NULL")
        .mapTo<VardaFeeDataRow>().list() ?: emptyList()
}

private fun clearVardaPlacements(db: Database.Connection) = db.transaction {
    it.createUpdate("TRUNCATE varda_placement")
        .execute()
}

private fun setFeeDecisionsAnnulled(db: Database.Connection) = db.transaction {
    it.createUpdate("UPDATE fee_decision SET status = :annulledStatus")
        .bind("annulledStatus", FeeDecisionStatus.ANNULLED)
        .execute()
}

private fun getVardaFeeDataRows(db: Database.Connection): List<VardaFeeDataRow> = db.read {
    it.createQuery("SELECT * FROM varda_fee_data")
        .mapTo<VardaFeeDataRow>().list() ?: emptyList()
}

private fun insertFeeDecision(
    db: Database.Connection,
    status: FeeDecisionStatus,
    children: List<PersonData.Detailed>,
    objectMapper: ObjectMapper,
    period: Period = Period(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
    placementType: PlacementType = PlacementType.DAYCARE,
    daycareId: UUID = testDaycare.id
): FeeDecision = db.transaction {
    val feeDecision = createFeeDecisionFixture(
        status = status,
        decisionType = FeeDecisionType.NORMAL,
        headOfFamilyId = testAdult_1.id,
        period = period,
        parts = children.map { child ->
            createFeeDecisionPartFixture(
                childId = child.id,
                dateOfBirth = testChild_1.dateOfBirth,
                daycareId = daycareId,
                placementType = placementType
            )
        }
    )
    upsertFeeDecisions(it.handle, objectMapper, listOf(feeDecision))
    feeDecision
}

private fun clearPlacements(db: Database.Connection) = db.transaction {
    it.createUpdate("DELETE FROM placement WHERE 1 = 1")
        .execute()
}

val childWithNamelessParent = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2018, 12, 31),
    ssn = "311218A999J",
    firstName = "Niilo",
    lastName = "Nimettömänpoika",
    streetAddress = "Kankkulankaivo 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)
