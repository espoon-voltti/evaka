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
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
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
import org.jdbi.v3.core.Handle
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
        jdbi.handle { h ->
            insertGeneralTestFixtures(h)
            insertVardaUnit(h)
            assertEquals(0, getVardaFeeDataRows(h).size)
            mockEndpoint.feeData.clear()
        }
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
    }

    @Test
    fun `fee data is sent when placement is sent to Varda`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h = h, child = testChild_1)
            insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)

            updateFeeData(h)

            assertEquals(1, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is saved to database correctly after upload`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)
            val placementId = insertTestPlacement(
                h = h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = period.start,
                endDate = period.end
            )
            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)

            val feeDecision = insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )

            updateFeeData(h)
            val feeDataRows = getVardaFeeDataRows(h)
            assertEquals(1, feeDataRows.size)
            assertEquals(feeDecision.id, feeDataRows[0].evakaFeeDecisionId)
            assertEquals(placementId, feeDataRows[0].evakaPlacementId)
            assertNotNull(feeDataRows[0].vardaFeeDataId)
            assertNotNull(feeDataRows[0].createdAt)
            assertNotNull(feeDataRows[0].uploadedAt)
        }
    }

    @Test
    fun `fee data is mapped correctly`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h = h, child = testChild_1)
            val feeDecision = insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                placementType = PlacementType.DAYCARE
            )

            updateFeeData(h)

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
    }

    @Test
    fun `five year old daycare is mapped correctly`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h = h, child = testChild_1)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                placementType = PlacementType.FIVE_YEARS_OLD_DAYCARE
            )

            updateFeeData(h)

            val results = mockEndpoint.feeData
            assertEquals(1, results.size)

            assertEquals("MP02", results[0].feeCode)
        }
    }

    @Test
    fun `fee decision with two children generates two fee data entries`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h, testChild_1)
            createDecisionsAndPlacements(h, testChild_2)
            insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(testChild_1, testChild_2), objectMapper)

            updateFeeData(h)

            assertEquals(2, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is not sent when there is no placement sent to Varda`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h, testChild_1)
            insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)
            clearVardaPlacements(h)

            updateFeeData(h)

            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is not sent when the fee decision is annulled`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h, testChild_1)
            insertFeeDecision(h, FeeDecisionStatus.ANNULLED, listOf(testChild_1), objectMapper)

            updateFeeData(h)

            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is not sent when the fee decision is draft`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h, testChild_1)
            val sentFeeDecision = insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(testChild_1), objectMapper)

            updateFeeData(h)

            insertFeeDecision(h, FeeDecisionStatus.DRAFT, listOf(testChild_1), objectMapper)
            h.createUpdate("UPDATE placement SET updated = now() + interval '30 second' WHERE 1 = 1").execute()

            updateFeeData(h)

            val feeDataRows = getVardaFeeDataRows(h)
            assertEquals(1, feeDataRows.size)
            assertEquals(sentFeeDecision.id, feeDataRows[0].evakaFeeDecisionId)
        }
    }

    @Test
    fun `child with no guardians is not sent to Varda`() {
        jdbi.handle { h ->
            // testChild_3 doesn't have guardians in mock VTJ data
            createDecisionsAndPlacements(h = h, child = testChild_3)
            insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(testChild_3), objectMapper)

            updateFeeData(h)

            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is sent and updated to database only once`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
            createDecisionsAndPlacements(h = h, child = testChild_1, period = period)

            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )

            updateFeeData(h)
            val originalEntry = getVardaFeeDataRows(h)[0]

            updateFeeData(h)
            updateFeeData(h)
            updateFeeData(h)
            updateFeeData(h)

            val finalEntry = getVardaFeeDataRows(h)[0]
            assertEquals(1, mockEndpoint.feeData.size)
            assertEquals(1, getVardaFeeDataRows(h).size)
            assertEquals(originalEntry, finalEntry)
        }
    }

    @Test
    fun `child with various decisions in Varda is handled correctly`() {
        jdbi.handle { h ->
            val period1 = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
            val period2 = ClosedPeriod(period1.end.plusMonths(1), LocalDate.now().plusMonths(6))
            val child = testChild_1
            createDecisionsAndPlacements(h = h, child = child, period = period1)

            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period1.start, period1.end)
            )

            updateFeeData(h)
            assertEquals(1, getVardaFeeDataRows(h).size)

            insertTestPlacement(
                h = h,
                childId = child.id,
                unitId = testDaycare.id,
                startDate = period2.start,
                endDate = period2.end
            )
            insertServiceNeed(h, child.id, period2)
            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period2.start, period2.end)
            )

            updateFeeData(h)
            assertEquals(2, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `child with decision to municipal and purchased units is handled correctly`() {
        jdbi.handle { h ->
            val period1 = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(5))
            val period2 = ClosedPeriod(period1.end.plusMonths(1), LocalDate.now().plusMonths(6))

            val child = testChild_1

            val paosDaycareId = testPurchasedDaycare.id

            updateUnits(h, vardaClient, vardaOrganizerName)

            createDecisionsAndPlacements(h = h, child = child, period = period1, daycareId = paosDaycareId)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(child),
                objectMapper = objectMapper,
                period = Period(period1.start, period1.end),
                daycareId = paosDaycareId
            )
            updateFeeData(h)
            assertEquals(1, getVardaFeeDataRows(h).size)

            val daycareId = testDaycare.id
            createDecisionsAndPlacements(h = h, child = child, period = period2, daycareId = daycareId)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(child),
                objectMapper = objectMapper,
                period = Period(period2.start, period2.end),
                daycareId = daycareId
            )
            updateFeeData(h)

            assertEquals(2, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `two fee data entries is sent with two placements inside one fee decision`() {
        /*
            Fee decision |--------------------|
            Placement       |-------|  |--|
            Fee data        |-------|  |--|
        */
        jdbi.handle { h ->
            val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
            val firstPlacementPeriod = ClosedPeriod(decisionPeriod.start.plusMonths(1), decisionPeriod.start.plusMonths(2))
            val secondPlacementPeriod = ClosedPeriod(firstPlacementPeriod.end.plusMonths(1), decisionPeriod.end.minusMonths(1))
            insertDecisionWithApplication(h, testChild_1, decisionPeriod)
            insertServiceNeed(h, testChild_1.id, decisionPeriod)
            insertVardaChild(h, testChild_1.id, Instant.now().minusSeconds(60 * 60 * 24))
            insertTestPlacement(h = h, childId = testChild_1.id, unitId = testDaycare.id, startDate = firstPlacementPeriod.start, endDate = firstPlacementPeriod.end)
            insertTestPlacement(h = h, childId = testChild_1.id, unitId = testDaycare.id, startDate = secondPlacementPeriod.start, endDate = secondPlacementPeriod.end)
            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(decisionPeriod.start, decisionPeriod.end)
            )
            updateFeeData(h)

            assertEquals(2, getVardaFeeDataRows(h).size)
            assertEquals(2, getVardaPlacements(h).size)

            val uploadedFeeData: List<VardaFeeData> = mockEndpoint.feeData.sortedBy { data -> data.startDate }
            assertEquals(firstPlacementPeriod.start, uploadedFeeData[0].startDate)
            assertEquals(firstPlacementPeriod.end, uploadedFeeData[0].endDate)
            assertEquals(secondPlacementPeriod.start, uploadedFeeData[1].startDate)
            assertEquals(secondPlacementPeriod.end, uploadedFeeData[1].endDate)
        }
    }

    @Test
    fun `two different fee data entries are sent when placement has two fee decisions`() {
        /*
            Fee decision |-----------||-----------|
            Placement       |----------------|
            Fee data        |--------||------|
        */
        jdbi.handle { h ->
            val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
            val feeDecisionsPeriod1 = ClosedPeriod(decisionPeriod.start, decisionPeriod.start.plusMonths(2))
            val feeDecisionPeriod2 = ClosedPeriod(feeDecisionsPeriod1.end.plusMonths(1), decisionPeriod.end)
            val placementPeriod = ClosedPeriod(feeDecisionsPeriod1.start.plusMonths(1), feeDecisionPeriod2.end.minusMonths(1))

            insertDecisionWithApplication(h, testChild_1, decisionPeriod)
            insertServiceNeed(h, testChild_1.id, decisionPeriod)
            insertVardaChild(h, testChild_1.id)
            insertTestPlacement(h = h, childId = testChild_1.id, unitId = testDaycare.id, startDate = placementPeriod.start, endDate = placementPeriod.end)
            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(feeDecisionsPeriod1.start, feeDecisionsPeriod1.end)
            )
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1, testChild_2),
                objectMapper = objectMapper,
                period = Period(feeDecisionPeriod2.start, feeDecisionPeriod2.end)
            )

            updateFeeData(h)
            assertEquals(2, getVardaFeeDataRows(h).size)
            assertEquals(1, getVardaPlacements(h).size)

            val uploadedFeeData: List<VardaFeeData> = mockEndpoint.feeData.sortedBy { data -> data.startDate }
            assertEquals(placementPeriod.start, uploadedFeeData[0].startDate)
            assertEquals(feeDecisionsPeriod1.end, uploadedFeeData[0].endDate)
            assertEquals(2, uploadedFeeData[0].familySize)

            assertEquals(feeDecisionPeriod2.start, uploadedFeeData[1].startDate)
            assertEquals(placementPeriod.end, uploadedFeeData[1].endDate)
            assertEquals(3, uploadedFeeData[1].familySize)
        }
    }

    @Test
    fun `delete fee data entry if its placement is removed`() {
        jdbi.handle { h ->
            createDecisionsAndPlacements(h)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            )
            updateFeeData(h)

            assertEquals(1, getVardaPlacements(h).size)
            assertEquals(1, getVardaFeeDataRows(h).size)

            clearPlacements(h)
            updatePlacements(h, vardaClient)
            updateFeeData(h)

            assertEquals(0, getVardaPlacements(h).size)
            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `delete all fee data entries of annulled fee decisions`() {
        jdbi.handle { h ->
            val decisionPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6))
            val firstPlacementPeriod = ClosedPeriod(decisionPeriod.start.plusMonths(1), decisionPeriod.start.plusMonths(2))
            val secondPlacementPeriod = ClosedPeriod(firstPlacementPeriod.end.plusMonths(1), decisionPeriod.end.minusMonths(1))
            insertDecisionWithApplication(h, testChild_1, decisionPeriod)
            insertServiceNeed(h, testChild_1.id, decisionPeriod)
            insertVardaChild(h, testChild_1.id, Instant.now().minusSeconds(60 * 60 * 24))
            insertTestPlacement(h = h, childId = testChild_1.id, unitId = testDaycare.id, startDate = firstPlacementPeriod.start, endDate = firstPlacementPeriod.end)
            insertTestPlacement(h = h, childId = testChild_1.id, unitId = testDaycare.id, startDate = secondPlacementPeriod.start, endDate = secondPlacementPeriod.end)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(decisionPeriod.start, decisionPeriod.end)
            )
            updateDecisions(h, vardaClient)
            updatePlacements(h, vardaClient)
            updateFeeData(h)

            assertEquals(2, getVardaFeeDataRows(h).size)

            setFeeDecisionsAnnulled(h)
            updateFeeData(h)

            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `deleting legacy fee data works`() {
        jdbi.handle { h ->
            val feeDecision = insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            )
            // language=SQL
            val sql =
                """
                    INSERT INTO varda_fee_data (evaka_fee_decision_id, varda_fee_data_id, evaka_placement_id) 
                    VALUES (:feeDecisionId, 12333, NULL)
                """.trimIndent()

            h.createUpdate(sql)
                .bind("feeDecisionId", feeDecision.id)
                .execute()

            assertEquals(1, getVardaFeeDataRows(h).size)
            updateFeeData(h)
            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `modify fee data entry when placement is modified`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )
            updateFeeData(h)

            assertEquals(1, getVardaFeeDataRows(h).size)
            val originalFeeData = mockEndpoint.feeData[0]
            assertEquals(period.start, originalFeeData.startDate)
            assertEquals(period.end, originalFeeData.endDate)

            val newStart = period.start.plusDays(1)
            val newEnd = period.end.minusDays(1)
            // language=SQL
            val sql =
                """
                    UPDATE placement SET start_date = :newStart, end_date = :newEnd WHERE 1 = 1
                """.trimIndent()

            h.createUpdate(sql)
                .bind("newStart", newStart)
                .bind("newEnd", newEnd)
                .execute()

            updateFeeData(h)

            val newFeeData = mockEndpoint.feeData[0]
            assertNotEquals(originalFeeData.startDate, newFeeData.startDate)
            assertNotEquals(originalFeeData.endDate, newFeeData.endDate)
            assertEquals(newStart, newFeeData.startDate)
            assertEquals(newEnd, newFeeData.endDate)
        }
    }

    @Test
    fun `modifying fee data updates the timestamp`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )
            updateFeeData(h)

            assertEquals(1, getVardaFeeDataRows(h).size)
            val originalEntry = getVardaFeeDataRows(h)[0]

            val newStart = period.start.plusDays(1)
            // language=SQL
            val sql =
                """
                    UPDATE placement SET start_date = :newStart WHERE 1 = 1
                """.trimIndent()

            h.createUpdate(sql)
                .bind("newStart", newStart)
                .execute()

            updateFeeData(h)

            val finalEntry = getVardaFeeDataRows(h)[0]
            assertNotEquals(originalEntry, finalEntry)
            assertTrue(originalEntry.uploadedAt < finalEntry.uploadedAt)
        }
    }

    @Test
    fun `fee data is soft deleted if it is flagged with should_be_deleted`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )
            updateFeeData(h)

            assertEquals(1, getVardaFeeDataRows(h).size)
            assertEquals(0, getSoftDeletedVardaFeeData(h).size)

            h.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute()

            removeMarkedFeeDataFromVarda(h, vardaClient)
            assertEquals(1, getSoftDeletedVardaFeeData(h).size)
        }
    }

    @Test
    fun `fee_data is not updated if upload flag is turned off`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )

            updateAll(h)

            assertEquals(1, mockEndpoint.feeData.size)

            h.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id").bind("id", testDaycare.id).execute()

            h.createUpdate("UPDATE placement SET start_date = :newStart")
                .bind("newStart", period.start.minusMonths(1))
                .execute()

            updateAll(h)

            val feeData = mockEndpoint.feeData
            assertEquals(1, feeData.size)
            assertEquals(period.start, feeData[0].startDate)
        }
    }

    @Test
    fun `updating daycare organizer oid yields new varda fee data row if old is soft deleted`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )

            updateAll(h)

            assertEquals(1, getVardaFeeDataRows(h).size)

            h.createUpdate("update varda_fee_data set should_be_deleted = true, deleted_at = NOW()").execute()
            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()

            updateAll(h)

            assertEquals(2, getVardaFeeDataRows(h).size)
        }
    }

    @Test
    fun `fee data is sent once if it's soft deleted`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            createDecisionsAndPlacements(h, period = period)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )
            updateAll(h)

            assertEquals(1, getVardaFeeDataRows(h).size)
            assertEquals(0, getSoftDeletedVardaFeeData(h).size)

            h.createUpdate("UPDATE varda_placement SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id").bind("id", testDaycare.id).execute()

            removeMarkedPlacementsFromVarda(h, vardaClient)
            removeMarkedDecisionsFromVarda(h, vardaClient)
            removeMarkedFeeDataFromVarda(h, vardaClient)
            mockEndpoint.feeData.clear()

            updateAll(h)
            assertEquals(2, getVardaFeeDataRows(h).size)
            assertEquals(1, getSoftDeletedVardaFeeData(h).size)
            assertEquals(1, mockEndpoint.feeData.size)

            updateAll(h)
            assertEquals(2, getVardaFeeDataRows(h).size)
            assertEquals(1, mockEndpoint.feeData.size)
        }
    }

    @Test
    fun `fee data is not sent multiple times for multiple fee decisions`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))
            val period2 = ClosedPeriod(period.start.minusMonths(6), period.start.minusMonths(1))
            createDecisionsAndPlacements(h, period = period)
            createDecisionsAndPlacements(h, period = period2)
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period.start, period.end)
            )
            insertFeeDecision(
                h = h,
                status = FeeDecisionStatus.SENT,
                children = listOf(testChild_1),
                objectMapper = objectMapper,
                period = Period(period2.start, period2.end)
            )
            updateAll(h)

            assertEquals(2, getVardaFeeDataRows(h).size)
            assertEquals(0, getSoftDeletedVardaFeeData(h).size)

            h.createUpdate("UPDATE varda_placement SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE varda_fee_data SET should_be_deleted = true").execute()
            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id").bind("id", testDaycare.id).execute()

            removeMarkedPlacementsFromVarda(h, vardaClient)
            removeMarkedDecisionsFromVarda(h, vardaClient)
            removeMarkedFeeDataFromVarda(h, vardaClient)
            mockEndpoint.feeData.clear()

            updateAll(h)
            assertEquals(4, getVardaFeeDataRows(h).size)
            assertEquals(2, getSoftDeletedVardaFeeData(h).size)
            assertEquals(2, mockEndpoint.feeData.size)

            updateAll(h)
            assertEquals(4, getVardaFeeDataRows(h).size)
            assertEquals(2, mockEndpoint.feeData.size)
        }
    }

    @Test
    fun `child whose guardians has no name is not sent to Varda`() {
        jdbi.handle { h ->
            h.insertTestPerson(
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
            h.insertTestChild(DevChild(id = childWithNamelessParent.id))

            // Niilo Nimettömänpoika's guardian doesn't have a name in mock VTJ data
            createDecisionsAndPlacements(h = h, child = childWithNamelessParent)
            insertFeeDecision(h, FeeDecisionStatus.SENT, listOf(childWithNamelessParent), objectMapper)

            updateFeeData(h)

            assertEquals(0, getVardaFeeDataRows(h).size)
        }
    }

    private fun updateAll(h: Handle) {
        updateChildren(h, vardaClient, vardaOrganizerName)
        updateDecisions(h, vardaClient)
        updatePlacements(h, vardaClient)
        updateFeeData(h)
    }

    private fun updateFeeData(h: Handle) {
        h.transaction {
            updateFeeData(Database.Transaction.wrap(it), vardaClient, objectMapper, personService)
        }
    }

    private fun createDecisionsAndPlacements(
        h: Handle,
        child: PersonData.Detailed = testChild_1,
        period: ClosedPeriod = ClosedPeriod(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
        daycareId: UUID = testDaycare.id
    ) {
        insertDecisionWithApplication(h, child, period, unitId = daycareId)
        insertServiceNeed(h, child.id, period)
        insertTestPlacement(
            h = h,
            childId = child.id,
            unitId = daycareId,
            startDate = period.start,
            endDate = period.end
        )
        updateChildren(h, vardaClient, organizerName = vardaOrganizerName)
        updateDecisions(h, vardaClient)
        updatePlacements(h, vardaClient)
    }
}

private fun getSoftDeletedVardaFeeData(h: Handle): List<VardaFeeDataRow> =
    h.createQuery("SELECT * FROM varda_fee_data WHERE deleted_at IS NOT NULL")
        .mapTo<VardaFeeDataRow>().list() ?: emptyList()

private fun clearVardaPlacements(h: Handle) {
    h.createUpdate("TRUNCATE varda_placement")
        .execute()
}

private fun setFeeDecisionsAnnulled(h: Handle) {
    h.createUpdate("UPDATE fee_decision SET status = :annulledStatus")
        .bind("annulledStatus", FeeDecisionStatus.ANNULLED)
        .execute()
}

private fun getVardaFeeDataRows(h: Handle): List<VardaFeeDataRow> =
    h.createQuery("SELECT * FROM varda_fee_data")
        .mapTo<VardaFeeDataRow>().list() ?: emptyList()

private fun insertFeeDecision(
    h: Handle,
    status: FeeDecisionStatus,
    children: List<PersonData.Detailed>,
    objectMapper: ObjectMapper,
    period: Period = Period(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6)),
    placementType: PlacementType = PlacementType.DAYCARE,
    daycareId: UUID = testDaycare.id
): FeeDecision {
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
    upsertFeeDecisions(h, objectMapper, listOf(feeDecision))
    return feeDecision
}

private fun clearPlacements(h: Handle) {
    h.createUpdate("DELETE FROM placement WHERE 1 = 1")
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
