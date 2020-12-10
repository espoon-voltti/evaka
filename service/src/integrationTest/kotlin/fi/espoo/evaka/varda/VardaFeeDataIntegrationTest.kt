// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.createFeeDecisionPartFixture
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChildWithNamelessGuardian
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    @BeforeEach
    fun beforeEach() {
        db.transaction { insertGeneralTestFixtures(it.handle) }
        insertVardaUnit(db)
        mockEndpoint.cleanUp()
    }

    @AfterEach
    fun afterEach() {
        db.transaction { it.resetDatabase() }
        mockEndpoint.cleanUp()
    }

    private val testPeriod = ClosedPeriod(start = LocalDate.now().minusMonths(1), end = LocalDate.now())

    private val guardiansTestChild1 = listOf(testAdult_1, testAdult_2)
        .map { VardaGuardian(it.ssn!!, it.firstName, it.lastName) }

    @Test
    fun `fee data is sent when a decision is sent to varda`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val feeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        assertEquals(1, mockEndpoint.feeData.size)
        mockEndpoint.feeData.values.first().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(feeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(feeDecision.familySize, it.perheen_koko)
            assertEquals(testPeriod.start, it.alkamis_pvm)
            assertEquals(testPeriod.end, it.paattymis_pvm)
        }
    }

    @Test
    fun `fee data is saved to database correctly after upload`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val feeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        assertEquals(1, mockEndpoint.feeData.size)
        val vardaFeeDataRows = getVardaFeeDataRows(db)
        assertEquals(1, vardaFeeDataRows.size)
        vardaFeeDataRows.first().let { row ->
            assertEquals(feeDecision.id, row.evakaFeeDecisionId)
            assertEquals(mockEndpoint.feeData.keys.first(), row.vardaId)
            db.read {
                val decisionId = it.createQuery("SELECT id FROM varda_decision LIMIT 1").mapTo<UUID>().first()
                assertEquals(decisionId, row.vardaDecisionId)
                val childId = it.createQuery("SELECT id FROM varda_child LIMIT 1").mapTo<UUID>().first()
                assertEquals(childId, row.vardaChildId)
            }
        }
    }

    @Test
    fun `five year old daycare has correct fee basis code`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        insertFeeDecision(
            db,
            listOf(testChild_1),
            testPeriod,
            placementType = PlacementType.FIVE_YEARS_OLD_DAYCARE
        ).send()

        updateAll()

        assertEquals(1, mockEndpoint.feeData.size)
        mockEndpoint.feeData.values.first().let {
            assertEquals(FeeBasisCode.FIVE_YEAR_OLDS_DAYCARE.code, it.maksun_peruste_koodi)
        }
    }

    @Test
    fun `fee decision with two children results in two fee data entries`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        createDecisionsAndPlacements(testPeriod, testChild_2)
        insertFeeDecision(db, listOf(testChild_1, testChild_2), testPeriod).send()

        updateAll()

        assertEquals(2, mockEndpoint.feeData.size)
    }

    @Test
    fun `fee data is not sent when there is no decision sent to Varda`() {
        insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        assertEquals(0, mockEndpoint.feeData.size)
    }

    @Test
    fun `fee data is not sent when the fee decision is still a draft`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        insertFeeDecision(db, listOf(testChild_1), testPeriod)

        updateAll()

        assertEquals(0, mockEndpoint.feeData.size)
    }

    @Test
    fun `child with no guardians is not sent to Varda`() {
        // testChild_3 doesn't have guardians in mock VTJ data
        createDecisionsAndPlacements(testPeriod, testChild_3)
        insertFeeDecision(db, listOf(testChild_3), testPeriod).send()

        updateAll()

        assertEquals(0, mockEndpoint.feeData.size)
    }

    @Test
    fun `child with a nameless guardian is not sent to Varda`() {
        createDecisionsAndPlacements(testPeriod, testChildWithNamelessGuardian)
        insertFeeDecision(db, listOf(testChildWithNamelessGuardian), testPeriod).send()

        updateAll()

        assertEquals(0, mockEndpoint.feeData.size)
    }

    @Test
    fun `fee data is not updated when fee decision has not been updated`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, newSentData.size)
        assertEquals(originalSentData, newSentData)
        val newRows = getVardaFeeDataRows(db)
        assertEquals(1, newRows.size)
        assertEquals(originalRows, newRows)
    }

    @Test
    fun `fee data is sent for both decision periods when one fee decision covers two decisions`() {
        val periodFirstHalf = testPeriod.copy(end = testPeriod.start.plusDays(7))
        val periodSecondHalf = testPeriod.copy(start = testPeriod.start.plusDays(8))
        createDecisionsAndPlacements(periodFirstHalf, testChild_1)
        createDecisionsAndPlacements(periodSecondHalf, testChild_1)
        val feeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val sortedSentData = mockEndpoint.feeData.values.sortedBy { it.alkamis_pvm }
        assertEquals(2, mockEndpoint.feeData.size)
        sortedSentData.first().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(feeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(feeDecision.familySize, it.perheen_koko)
            assertEquals(periodFirstHalf.start, it.alkamis_pvm)
            assertEquals(periodFirstHalf.end, it.paattymis_pvm)
        }
        sortedSentData.last().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(feeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(feeDecision.familySize, it.perheen_koko)
            assertEquals(periodSecondHalf.start, it.alkamis_pvm)
            assertEquals(periodSecondHalf.end, it.paattymis_pvm)
        }
    }

    @Test
    fun `fee data is sent for both decision periods when two fee decisions cover one decision`() {
        val periodFirstHalf = testPeriod.copy(end = testPeriod.start.plusDays(7))
        val periodSecondHalf = testPeriod.copy(start = testPeriod.start.plusDays(8))
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val firstFeeDecision = insertFeeDecision(db, listOf(testChild_1), periodFirstHalf).send()
        val secondFeeDecision = insertFeeDecision(
            db,
            listOf(testChild_1, testChild_2),
            periodSecondHalf
        ).send()

        updateAll()

        val sortedSentData = mockEndpoint.feeData.values.sortedBy { it.alkamis_pvm }
        assertEquals(2, mockEndpoint.feeData.size)
        sortedSentData.first().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(firstFeeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(firstFeeDecision.familySize, it.perheen_koko)
            assertEquals(periodFirstHalf.start, it.alkamis_pvm)
            assertEquals(periodFirstHalf.end, it.paattymis_pvm)
        }
        sortedSentData.last().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(secondFeeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(secondFeeDecision.familySize, it.perheen_koko)
            assertEquals(periodSecondHalf.start, it.alkamis_pvm)
            assertEquals(periodSecondHalf.end, it.paattymis_pvm)
        }
    }

    @Test
    fun `child with decision to municipal and purchased units is handled correctly`() {
        val periodFirstHalf = testPeriod.copy(end = testPeriod.start.plusDays(7))
        val periodSecondHalf = testPeriod.copy(start = testPeriod.start.plusDays(8))
        createDecisionsAndPlacements(periodFirstHalf, testChild_1, testDaycare.id)
        createDecisionsAndPlacements(periodSecondHalf, testChild_1, testPurchasedDaycare.id)
        val firstFeeDecision = insertFeeDecision(
            db,
            listOf(testChild_1),
            periodFirstHalf,
            daycareId = testDaycare.id
        ).send()
        val secondFeeDecision = insertFeeDecision(
            db,
            listOf(testChild_1, testChild_2),
            periodSecondHalf,
            daycareId = testPurchasedDaycare.id
        ).send()

        updateAll()

        val sortedSentData = mockEndpoint.feeData.values.sortedBy { it.alkamis_pvm }
        assertEquals(2, mockEndpoint.feeData.size)
        sortedSentData.first().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(firstFeeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(firstFeeDecision.familySize, it.perheen_koko)
            assertEquals(periodFirstHalf.start, it.alkamis_pvm)
            assertEquals(periodFirstHalf.end, it.paattymis_pvm)
        }
        sortedSentData.last().let {
            assertEquals(guardiansTestChild1, it.huoltajat)
            assertEquals(FeeBasisCode.DAYCARE.code, it.maksun_peruste_koodi)
            assertEquals(0, it.palveluseteli_arvo)
            assertEquals(secondFeeDecision.parts.first().finalFee(), it.asiakasmaksu)
            assertEquals(secondFeeDecision.familySize, it.perheen_koko)
            assertEquals(periodSecondHalf.start, it.alkamis_pvm)
            assertEquals(periodSecondHalf.end, it.paattymis_pvm)
        }
    }

    @Test
    fun `fee data is deleted when corresponding decision is deleted`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        db.transaction {
            it.execute("DELETE FROM decision")
            it.execute("DELETE FROM placement")
        }
        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(0, newSentData.size)
        val newRows = getVardaFeeDataRows(db)
        assertEquals(0, newRows.size)
    }

    @Test
    fun `fee data is deleted when corresponding fee decision is annulled`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val feeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        db.transaction {
            it.execute(
                "UPDATE fee_decision SET status = ? WHERE id = ?",
                FeeDecisionStatus.ANNULLED,
                feeDecision.id
            )
        }
        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(0, newSentData.size)
        val newRows = getVardaFeeDataRows(db)
        assertEquals(0, newRows.size)
    }

    @Test
    fun `fee data is replaced when corresponding fee decision is replaced by a new one`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val originalFeeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        originalSentData.values.first().let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
        }
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        val newFeeDecision = insertFeeDecision(db, listOf(testChild_1, testChild_2), testPeriod).send()
        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, newSentData.size)
        newSentData.values.first().let { data ->
            assertEquals(newFeeDecision.familySize, data.perheen_koko)
        }
        val newRows = getVardaFeeDataRows(db)
        assertEquals(1, newRows.size)
    }

    @Test
    fun `fee data is updated and a new one is created when corresponding fee decision is partially replaced by a new one`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val originalFeeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        originalSentData.values.first().let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
            assertEquals(testPeriod.start, data.alkamis_pvm)
            assertEquals(testPeriod.end, data.paattymis_pvm)
        }
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        val periodEndHalf = testPeriod.copy(start = testPeriod.start.plusDays(8))
        val newFeeDecision = insertFeeDecision(
            db,
            listOf(testChild_1, testChild_2),
            periodEndHalf
        ).send()
        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(2, newSentData.size)
        val originalData = newSentData[originalSentData.keys.first()]
        assertNotNull(originalData)
        originalData!!.let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
            assertEquals(testPeriod.start, data.alkamis_pvm)
            assertEquals(periodEndHalf.start.minusDays(1), data.paattymis_pvm)
        }
        newSentData.values.last().let { data ->
            assertEquals(newFeeDecision.familySize, data.perheen_koko)
            assertEquals(periodEndHalf.start, data.alkamis_pvm)
            assertEquals(periodEndHalf.end, data.paattymis_pvm)
        }
        val newRows = getVardaFeeDataRows(db)
        assertEquals(2, newRows.size)
    }

    @Test
    fun `fee data is updated and a new one is created when a new decision is created and fee data is updated after a new fee decision is created`() {
        createDecisionsAndPlacements(testPeriod, testChild_1)
        val originalFeeDecision = insertFeeDecision(db, listOf(testChild_1), testPeriod).send()

        updateAll()

        val originalSentData = mockEndpoint.feeData.toMap()
        assertEquals(1, originalSentData.size)
        originalSentData.values.first().let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
            assertEquals(testPeriod.start, data.alkamis_pvm)
            assertEquals(testPeriod.end, data.paattymis_pvm)
        }
        val originalRows = getVardaFeeDataRows(db)
        assertEquals(1, originalRows.size)

        val newPeriod = testPeriod.copy(start = testPeriod.start.plusDays(8))
        db.transaction {
            it.execute("UPDATE placement SET end_date = ?", newPeriod.start.minusDays(1))
            it.execute("UPDATE service_need SET end_date = ?", newPeriod.start.minusDays(1))
        }
        createDecisionsAndPlacements(newPeriod, testChild_1)
        updateAll()

        val newSentData = mockEndpoint.feeData.toMap()
        assertEquals(2, newSentData.size)
        newSentData.values.first().let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
            assertEquals(testPeriod.start, data.alkamis_pvm)
            assertEquals(newPeriod.start.minusDays(1), data.paattymis_pvm)
        }
        newSentData.values.last().let { data ->
            assertEquals(originalFeeDecision.familySize, data.perheen_koko)
            assertEquals(newPeriod.start, data.alkamis_pvm)
            assertEquals(newPeriod.end, data.paattymis_pvm)
        }
        val newRows = getVardaFeeDataRows(db)
        assertEquals(2, newRows.size)

        val newFeeDecision = insertFeeDecision(
            db,
            listOf(testChild_1, testChild_2),
            newPeriod
        ).send()
        updateAll()

        val updatedSentData = mockEndpoint.feeData.toMap()
        assertEquals(2, updatedSentData.size)
        updatedSentData.values.last().let { data ->
            assertEquals(newFeeDecision.familySize, data.perheen_koko)
            assertEquals(newPeriod.start, data.alkamis_pvm)
            assertEquals(newPeriod.end, data.paattymis_pvm)
        }
        val updatedRows = getVardaFeeDataRows(db)
        assertEquals(2, updatedRows.size)
    }

    @Test
    fun `fee data is sent in an expected way when a child's both guardians have a fee decision`() {
        // Although this shouldn't usually happen, it's not prevented at the database level
        // so we have take this case into account
        createDecisionsAndPlacements(testPeriod, testChild_1)
        insertFeeDecision(db, listOf(testChild_1), testPeriod).send()
        insertFeeDecision(db, listOf(testChild_1), testPeriod, guardian = testAdult_2.id).send()

        updateAll()
        assertEquals(2, mockEndpoint.feeData.size)

        updateAll()
        assertEquals(2, mockEndpoint.feeData.size)

        val newPeriod = testPeriod.copy(start = testPeriod.start.plusDays(8))
        db.transaction {
            it.execute("UPDATE placement SET end_date = ?", newPeriod.start.minusDays(1))
            it.execute("UPDATE service_need SET end_date = ?", newPeriod.start.minusDays(1))
        }
        createDecisionsAndPlacements(newPeriod, testChild_1)

        updateAll()
        assertEquals(4, mockEndpoint.feeData.size)

        insertFeeDecision(db, listOf(testChild_1), newPeriod).send()
        insertFeeDecision(db, listOf(testChild_1), newPeriod, guardian = testAdult_2.id).send()

        updateAll()
        assertEquals(4, mockEndpoint.feeData.size)
    }

    private fun updateAll() {
        updateChildren(db, vardaClient, vardaOrganizerName)
        updateDecisions(db, vardaClient)
        updatePlacements(db, vardaClient)
        updateFeeData(db, vardaClient, personService)
    }

    private fun createDecisionsAndPlacements(
        period: ClosedPeriod,
        child: PersonData.Detailed = testChild_1,
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
    }

    data class VardaFeeDataRow(
        val id: UUID,
        val evakaFeeDecisionId: UUID,
        val vardaId: Long?,
        val vardaDecisionId: UUID?,
        val vardaChildId: UUID?,
        val createdAt: Instant,
        val uploadedAt: Instant
    )

    private fun getVardaFeeDataRows(db: Database.Connection): List<VardaFeeDataRow> = db.read {
        it.createQuery("SELECT * FROM varda_fee_data")
            .mapTo<VardaFeeDataRow>()
            .toList()
    }

    private fun insertFeeDecision(
        db: Database.Connection,
        children: List<PersonData.Detailed>,
        period: ClosedPeriod,
        placementType: PlacementType = PlacementType.DAYCARE,
        daycareId: UUID = testDaycare.id,
        guardian: UUID = testAdult_1.id
    ): FeeDecision = db.transaction {
        val feeDecision = createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = guardian,
            period = Period(period.start, period.end),
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

    private fun FeeDecision.send(): FeeDecision {
        val (_, response, _) = http.post("/fee-decisions/confirm")
            .asUser(AuthenticatedUser(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN)))
            .jsonBody(objectMapper.writeValueAsString(listOf(this.id)))
            .response()
        assertEquals(204, response.statusCode)
        asyncJobRunner.runPendingJobsSync()
        return db.read { getFeeDecisionsByIds(it.handle, objectMapper, listOf(this.id)) }.first()
    }
}
