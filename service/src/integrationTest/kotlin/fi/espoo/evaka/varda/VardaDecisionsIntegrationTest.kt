// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.defaultPurchasedOrganizerOid
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.updatePlacementStartAndEndDate
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.serviceneed.deleteServiceNeed
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultPreschool
import fi.espoo.evaka.snDefaultPreschoolDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VardaDecisionsIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.insertGeneralTestFixtures() }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { it.resetDatabase() }
        mockEndpoint.cleanUp()
    }

    @Test
    fun `municipal daycare decision is sent when it has all required data`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val payload = mockEndpoint.decisions.values.first()
        assertEquals("SourceSystemVarda", payload.sourceSystem)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertEquals(decisionId, result.first().evakaDecisionId)
    }

    @Test
    fun `PAOS decision to purchased daycare is sent when it has all required data`() {
        val decisionId = insertPlacementWithDecision(db, testChild_1, testPurchasedDaycare.id, FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))).first
        updateChildren()

        val children = getUploadedChildren(db)
        assertEquals(1, children.size)

        updateDecisions(db, vardaClient)

        val vardaDecisions = mockEndpoint.decisions
        assertEquals(1, vardaDecisions.size)
        assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, vardaDecisions.values.first().providerTypeCode)

        val decisionRows = getVardaDecisions()
        assertEquals(1, decisionRows.size)
        assertEquals(decisionId, decisionRows.first().evakaDecisionId)
    }

    @Test
    fun `PAOS decision to voucher daycare is sent when it has all required data`() {
        val decisionId = insertPlacementWithDecision(db, testChild_1, testVoucherDaycare.id, FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))).first
        updateChildren()

        val children = getUploadedChildren(db)
        assertEquals(1, children.size)

        updateDecisions(db, vardaClient)

        val vardaDecisions = mockEndpoint.decisions
        assertEquals(1, vardaDecisions.size)
        assertEquals(VardaUnitProviderType.PRIVATE_SERVICE_VOUCHER.vardaCode, vardaDecisions.values.first().providerTypeCode)

        val decisionRows = getVardaDecisions()
        assertEquals(1, decisionRows.size)
        assertEquals(decisionId, decisionRows.first().evakaDecisionId)
    }

    @Test
    fun `child with municipal and PAOS decisions is handled correctly`() {
        val firstPeriod = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val municipalDecisionId = insertPlacementWithDecision(db, testChild_1, testDaycare.id, firstPeriod).first
        val paosDecisionId = insertPlacementWithDecision(db, testChild_1, testPurchasedDaycare.id, FiniteDateRange(firstPeriod.end.plusDays(1), firstPeriod.end.plusDays(300))).first
        updateChildren()

        updateDecisions(db, vardaClient)

        val decisionRows = getVardaDecisions()
        assertEquals(2, decisionRows.size)
        assertNotNull(decisionRows.find { it.evakaDecisionId == municipalDecisionId })
        assertNotNull(decisionRows.find { it.evakaDecisionId == paosDecisionId })

        val children = getUploadedChildren(db)
        val decisions = mockEndpoint.decisions
        assertEquals(2, children.size)
        assertEquals(2, decisions.size)

        val paosChild = children.firstOrNull { it.ophOrganizerOid == defaultPurchasedOrganizerOid }
        assertNotNull(paosChild)

        val paosDecision = decisions.values.firstOrNull { it.childUrl.contains("/lapset/${paosChild!!.vardaChildId}") }
        assertNotNull(paosDecision)

        val municipalChild = children.firstOrNull { it.ophOrganizerOid == defaultMunicipalOrganizerOid }
        assertNotNull(municipalChild)

        val municipalDecision = decisions.values.firstOrNull { it.childUrl.contains("/lapset/${municipalChild!!.vardaChildId}/") }
        assertNotNull(municipalDecision)

        assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, paosDecision!!.providerTypeCode)
        assertEquals(VardaUnitProviderType.MUNICIPAL.vardaCode, municipalDecision!!.providerTypeCode)
    }

    @Test
    fun `a daycare decision is not sent when there is no existing service need`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `a daycare decision is not sent when the child has not been imported to varda yet`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `preschool decisions are not sent`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL)
        insertServiceNeed(db, placementId, period, snDefaultPreschool.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `a preschool daycare decision is sent`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val decisionId =
            insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultPreschoolDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertEquals(decisionId, result.first().evakaDecisionId)
    }

    @Test
    fun `a daycare decision is updated when a new service need is created for the period`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val serviceNeedEndDate = period.start.plusMonths(1)
        insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)
        insertServiceNeed(db, placementId, period.copy(end = serviceNeedEndDate), snDefaultPreschoolDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalUploadedAt = getVardaDecisions().first().uploadedAt

        insertServiceNeed(db, placementId, period.copy(start = serviceNeedEndDate.plusDays(1)), snDefaultPreschoolDaycare.id)
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertTrue(originalUploadedAt < result.first().uploadedAt)
    }

    @Test
    fun `a daycare decision is updated when service need is updated`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)
        val serviceNeedId = insertServiceNeed(db, placementId, period, snDefaultPreschoolDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalUploadedAt = getVardaDecisions().first().uploadedAt

        updateServiceNeed(serviceNeedId, originalUploadedAt.plusSeconds(1))
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertTrue(originalUploadedAt < result.first().uploadedAt)
    }

    @Test
    fun `a daycare decision is updated when a new overlapping decision is made`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalUploadedAt = getVardaDecisions().first().uploadedAt

        insertDecisionWithApplication(db, testChild_1, period.copy(start = period.start.plusMonths(1)))
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(2, result.size)
        assertTrue(result.all { originalUploadedAt < it.uploadedAt })
    }

    @Test
    fun `a daycare decision is deleted when a new decision that completely replaced the old one is made`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val firstDecisionId =
            insertDecisionWithApplication(db, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultPreschoolDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalDecisionId = getVardaDecisions().first().evakaDecisionId
        assertEquals(firstDecisionId, originalDecisionId)

        val secondDecisionId = insertDecisionWithApplication(db, testChild_1, period)
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertNotEquals(originalDecisionId, result.first().evakaDecisionId)
        assertEquals(secondDecisionId, result.first().evakaDecisionId)
    }

    @Test
    fun `a derived daycare decision is sent when there is a placement without a decision`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period, type = PlacementType.DAYCARE)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertEquals(placementId, result.first().evakaPlacementId)
    }

    @Test
    fun `multiple decisions are created from multiple derived daycare decisions`() {
        insertVardaChild(db, testChild_1.id)
        val firstSemester = FiniteDateRange(LocalDate.of(2018, 1, 8), LocalDate.of(2019, 5, 31))
        val firstSummer = FiniteDateRange(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 7, 31))
        val placementId1 = insertPlacement(db, testChild_1.id, firstSemester, PlacementType.PRESCHOOL_DAYCARE)
        val placementId2 = insertPlacement(db, testChild_1.id, firstSummer, PlacementType.DAYCARE)
        insertServiceNeed(db, placementId1, firstSemester, snDefaultPreschoolDaycare.id)
        insertServiceNeed(db, placementId2, firstSummer, snDefaultDaycare.id)

        val secondSemester = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31))
        val secondSummer = FiniteDateRange(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31))
        val placementId3 = insertPlacement(db, testChild_1.id, secondSemester, PlacementType.PRESCHOOL_DAYCARE)
        val placementId4 = insertPlacement(db, testChild_1.id, secondSummer, PlacementType.DAYCARE)
        insertServiceNeed(db, placementId3, secondSemester, snDefaultPreschoolDaycare.id)
        insertServiceNeed(db, placementId4, secondSummer, snDefaultDaycare.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(4, result.size)
    }

    @Test
    fun `a derived daycare decision is updated when the placement is updated`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalUploadedAt = getVardaDecisions().first().uploadedAt

        updatePlacement(db, placementId, originalUploadedAt.plusSeconds(1))
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertTrue(originalUploadedAt < result.first().uploadedAt)
    }

    @Test
    fun `a derived daycare decision is updated when service need is updated`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        val serviceNeedId = insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val originalUploadedAt = getVardaDecisions().first().uploadedAt

        updateServiceNeed(serviceNeedId, originalUploadedAt.plusSeconds(1))
        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        assertTrue(originalUploadedAt < result.first().uploadedAt)
    }

    @Test
    fun `a derived daycare decision is deleted when the placement is removed`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val beforeDelete = getVardaDecisions()
        assertEquals(1, beforeDelete.size)

        deletePlacement(db, placementId)
        updateDecisions(db, vardaClient)
        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `a derived daycare decision is deleted when its service need is removed`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        val serviceNeedId = insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val beforeDelete = getVardaDecisions()
        assertEquals(1, beforeDelete.size)

        db.transaction { it.deleteServiceNeed(serviceNeedId) }
        updateDecisions(db, vardaClient)
        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `a derived decision is sent when child has a rejected decision`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)
        insertDecisionWithApplication(
            db,
            child = testChild_1,
            period = period,
            decisionStatus = DecisionStatus.REJECTED
        )

        updateDecisions(db, vardaClient)
        val result = getVardaDecisions()
        assertEquals(1, result.size)
    }

    @Test
    fun `a derived decision is sent when child has a pending decision`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)
        insertDecisionWithApplication(
            db,
            child = testChild_1,
            period = period,
            decisionStatus = DecisionStatus.PENDING
        )

        updateDecisions(db, vardaClient)
        val result = getVardaDecisions()
        assertEquals(1, result.size)
    }

    @Test
    fun `a derived daycare decision is sent as temporary when the placement type is temporary`() {
        val period = FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 7, 3))
        insertPlacement(db, testChild_1.id, period, type = PlacementType.TEMPORARY_DAYCARE)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        val decisions = mockEndpoint.decisions
        assertEquals(1, decisions.size)
        decisions.values.first().let { decision ->
            assertTrue(decision.temporary)
            assertTrue(decision.fullDay)
            assertFalse(decision.daily)
        }
    }

    @Test
    fun `a derived daycare decision is sent as temporary when the placement type is temporary part day`() {
        val period = FiniteDateRange(LocalDate.of(2019, 7, 1), LocalDate.of(2020, 7, 3))
        insertPlacement(db, testChild_1.id, period, type = PlacementType.TEMPORARY_DAYCARE_PART_DAY)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(1, result.size)
        val decisions = mockEndpoint.decisions
        assertEquals(1, decisions.size)
        decisions.values.first().let { decision ->
            assertTrue(decision.temporary)
            assertFalse(decision.daily)
        }
    }

    @Test
    fun `daycare decision is sent with correct data`() {
        val sentDate = LocalDate.of(2019, 6, 1)
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period, sentDate = sentDate)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val decisions = mockEndpoint.decisions
        assertEquals(1, decisions.size)

        val decision = decisions.values.first()
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
        assertEquals(snDefaultDaycare.daycareHoursPerWeek.toDouble(), decision.hoursPerWeek)
        assertEquals(false, decision.temporary)
        assertEquals(true, decision.daily)
        assertEquals(true, decision.fullDay)
        assertEquals(false, decision.shiftCare)
        assertEquals(VardaUnitProviderType.MUNICIPAL.vardaCode, decision.providerTypeCode)
        assertEquals(false, decision.urgent)
        assertEquals(sentDate, decision.applicationDate)
    }

    @Test
    fun `application date is never after decision start date`() {
        val sentDate = LocalDate.of(2019, 6, 1)
        val period = FiniteDateRange(sentDate.minusMonths(1), sentDate.plusMonths(1))
        insertDecisionWithApplication(db, testChild_1, period, sentDate = sentDate)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val decision = mockEndpoint.decisions.values.first()
        assertNotEquals(sentDate, decision.applicationDate)
        assertEquals(period.start, decision.applicationDate)
    }

    @Test
    fun `PAOS daycare decision is sent with correct data`() {
        val sentDate = LocalDate.of(2019, 6, 1)
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period, sentDate = sentDate, unitId = testPurchasedDaycare.id)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id, ophOrganizationOid = defaultPurchasedOrganizerOid)

        updateDecisions(db, vardaClient)

        val decisions = mockEndpoint.decisions
        assertEquals(1, decisions.size)

        val decision = decisions.values.first()
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
        assertEquals(snDefaultDaycare.daycareHoursPerWeek.toDouble(), decision.hoursPerWeek)
        assertEquals(false, decision.temporary)
        assertEquals(true, decision.daily)
        assertEquals(true, decision.fullDay)
        assertEquals(false, decision.shiftCare)
        assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, decision.providerTypeCode)
        assertEquals(false, decision.urgent)
        assertEquals(sentDate, decision.applicationDate)
    }

    @Test
    fun `a decision has a correct end date when another decision replaces it midway`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        val periodFirstHalf = period.copy(end = period.start.plusMonths(1).minusDays(1))
        val periodSecondHalf = period.copy(start = period.start.plusMonths(1))
        insertDecisionWithApplication(db, testChild_1, period)
        insertDecisionWithApplication(db, testChild_1, periodSecondHalf)
        val placementId1 = insertPlacement(db, testChild_1.id, periodFirstHalf)
        insertServiceNeed(db, placementId1, periodFirstHalf, snDefaultDaycare.id)
        val placementId2 = insertPlacement(db, testChild_1.id, periodSecondHalf)
        insertServiceNeed(db, placementId2, periodSecondHalf, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val result = mockEndpoint.decisions.values.sortedBy { it.startDate }
        assertEquals(2, result.size)
        assertEquals(period.start, result[0].startDate)
        assertEquals(periodFirstHalf.end, result[0].endDate)
        assertEquals(periodSecondHalf.start, result[1].startDate)
        assertEquals(period.end, result[1].endDate)
    }

    @Test
    fun `a decision is not affected by a new decision that does not overlap with it`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertDecisionWithApplication(
            db,
            testChild_1,
            FiniteDateRange(period.end.plusDays(1), period.end.plusMonths(1))
        )
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)
        val result = mockEndpoint.decisions.values.first()
        assertEquals(period.start, result.startDate)
        assertEquals(period.end, result.endDate)
    }

    @Test
    fun `decision in the future is not sent to Varda`() {
        val period = FiniteDateRange(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2))
        insertDecisionWithApplication(db, testChild_1, period)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `derived decision in the future is not sent to Varda`() {
        val period = FiniteDateRange(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2))
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        val result = getVardaDecisions()
        assertEquals(0, result.size)
    }

    @Test
    fun `only one decision is sent to Varda when it has multiple placements inside it`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db)
        insertVardaChild(db, testChild_1.id)
        val newMiddleStart = period.start.plusMonths(1)
        val newMiddleEnd = period.end.minusMonths(1)
        FiniteDateRange(period.start, newMiddleStart.minusDays(1)).let { subPeriod ->
            val placementId = insertPlacement(db, testChild_1.id, subPeriod)
            insertServiceNeed(db, placementId, subPeriod, snDefaultDaycare.id)
        }
        FiniteDateRange(newMiddleStart, newMiddleEnd).let { subPeriod ->
            val placementId = insertPlacement(db, testChild_1.id, subPeriod)
            insertServiceNeed(db, placementId, subPeriod, snDefaultDaycare.id)
        }
        FiniteDateRange(newMiddleEnd.plusDays(1), period.end).let { subPeriod ->
            val placementId = insertPlacement(db, testChild_1.id, subPeriod)
            insertServiceNeed(db, placementId, subPeriod, snDefaultDaycare.id)
        }

        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)
        val decisions = mockEndpoint.decisions
        assertEquals(1, decisions.size)

        val decision = decisions.values.first()
        assertEquals(period.start, decision.startDate)
        assertEquals(period.end, decision.endDate)
    }

    @Test
    fun `a decision dates are prolonged when its placement is prolonged`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)
        updateDecisions(db, vardaClient)
        val decisions = getVardaDecisions()
        assertEquals(1, decisions.size)

        updatePlacements(db, vardaClient)
        val placements = getVardaPlacements(db)
        assertEquals(1, placements.size)
        assertEquals(placementId, placements.first().evakaPlacementId)
        assertEquals(decisions.first().id, placements.first().decisionId)

        val newStart = period.start.minusMonths(1)
        val newEnd = period.end.plusMonths(1)
        db.transaction { it.updatePlacementStartAndEndDate(placementId, newStart, newEnd) }
        updateDecisions(db, vardaClient)

        val vardaDecisions = mockEndpoint.decisions
        assertEquals(1, vardaDecisions.size)
        assertEquals(newStart, vardaDecisions.values.first().startDate)
        assertEquals(newEnd, vardaDecisions.values.first().endDate)
    }

    @Test
    fun `a decisions placements are deleted first when it's updated`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)
        updateDecisions(db, vardaClient)
        val decisions = getVardaDecisions()
        assertEquals(1, decisions.size)

        updatePlacements(db, vardaClient)
        val placements = getVardaPlacements(db)
        assertEquals(1, placements.size)

        db.transaction {
            it.updatePlacementStartAndEndDate(placementId, period.start.plusMonths(1), period.end.minusMonths(1))
        }
        updateDecisions(db, vardaClient)
        assertEquals(0, getVardaPlacements(db).size)

        updatePlacements(db, vardaClient)
        assertEquals(1, getVardaPlacements(db).size)
    }

    @Test
    fun `decision has correct dates when multiple placements that prolong it are added`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)
        assertEquals(1, mockEndpoint.decisions.size)
        val initialDecision = mockEndpoint.decisions.values.first()
        assertEquals(period.start, initialDecision.startDate)
        assertEquals(period.end, initialDecision.endDate)

        deletePlacement(db, placementId)
        val newStart = period.start.minusMonths(1)
        val newEnd = period.end.plusMonths(1)
        val newMiddleStart = period.start.plusMonths(1)
        val newMiddleEnd = period.end.minusMonths(1)
        insertPlacement(db, testChild_1.id, FiniteDateRange(newStart, newMiddleStart.minusDays(1))).let {
            insertServiceNeed(db, it, FiniteDateRange(newStart, newMiddleStart.minusDays(1)), snDefaultDaycare.id)
        }
        insertPlacement(db, testChild_1.id, FiniteDateRange(newMiddleStart, newMiddleEnd)).let {
            insertServiceNeed(db, it, FiniteDateRange(newMiddleStart, newMiddleEnd), snDefaultDaycare.id)
        }
        insertPlacement(db, testChild_1.id, FiniteDateRange(newMiddleEnd.plusDays(1), newEnd)).let {
            insertServiceNeed(db, it, FiniteDateRange(newMiddleEnd.plusDays(1), newEnd), snDefaultDaycare.id)
        }

        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)
        assertEquals(1, mockEndpoint.decisions.size)
        val finalDecision = mockEndpoint.decisions.values.first()
        assertEquals(newStart, finalDecision.startDate)
        assertEquals(newEnd, finalDecision.endDate)
    }

    @Test
    fun `decision is soft deleted if it is flagged with should_be_deleted`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db)
        val placementId = insertPlacement(db, testChild_1.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)
        assertEquals(0, getSoftDeletedVardaDecisions().size)

        db.transaction { it.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute() }

        removeMarkedDecisionsFromVarda(db, vardaClient)
        updateDecisions(db, vardaClient)

        assertEquals(1, getSoftDeletedVardaDecisions().size)
    }

    @Test
    fun `decision is not updated if upload flag is turned off`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
        insertDecisionWithApplication(db, testChild_1, period)
        insertVardaUnit(db, unitId = testDaycare.id)
        val newPlacementStart = period.start.plusMonths(1)
        val placementId = insertPlacement(db, testChild_1.id, period.copy(end = newPlacementStart.minusDays(1)))
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_1.id)

        updateDecisions(db, vardaClient)

        db.transaction { it.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id").bind("id", testDaycare.id).execute() }

        insertPlacement(db, testChild_1.id, FiniteDateRange(newPlacementStart, period.end))
        updateDecisions(db, vardaClient)

        val decision = mockEndpoint.decisions.values.first()
        assertEquals(period.start, decision.startDate)
    }

    @Test
    fun `updating daycare organizer oid yields new varda decision if old is soft deleted`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

        insertPlacementWithDecision(db, child = testChild_1, unitId = testDaycare.id, period = period)
        updateChildren()
        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)

        db.transaction {
            it.createUpdate("update varda_decision set deleted_at = NOW()").execute()
            it.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()
        }

        updateChildren()
        updateDecisions(db, vardaClient)
        assertEquals(2, getVardaDecisions().size)
    }

    @Test
    fun `soft deleted decisions are not sent`() {
        val period = FiniteDateRange(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1))

        insertPlacementWithDecision(db, child = testChild_1, unitId = testDaycare.id, period = period)
        updateChildren()
        updateDecisions(db, vardaClient)

        assertEquals(1, getVardaDecisions().size)
        assertEquals(1, mockEndpoint.decisions.size)

        db.transaction { it.createUpdate("update varda_decision set deleted_at = NOW()").execute() }

        removeMarkedDecisionsFromVarda(db, vardaClient)

        mockEndpoint.decisions.clear()

        updateDecisions(db, vardaClient)
        assertEquals(2, getVardaDecisions().size)
        assertEquals(1, mockEndpoint.decisions.size)

        updateDecisions(db, vardaClient)
        assertEquals(2, getVardaDecisions().size)
        assertEquals(1, mockEndpoint.decisions.size)
    }

    @Test
    fun `decisions are sent just once when data does not change`() {
        val period = FiniteDateRange(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

        val decisionId = insertDecisionWithApplication(db, testChild_1, period)
        insertPlacement(db, testChild_1.id, period).let {
            insertServiceNeed(db, it, period, snDefaultDaycare.id)
        }
        insertVardaChild(db, testChild_1.id)

        val placementId = insertPlacement(db, testChild_2.id, period)
        insertServiceNeed(db, placementId, period, snDefaultDaycare.id)
        insertVardaChild(db, testChild_2.id)

        val temporaryPlacementId = insertPlacement(db, testChild_3.id, period, type = PlacementType.TEMPORARY_DAYCARE)
        insertVardaChild(db, testChild_3.id)

        updateDecisions(db, vardaClient)

        val decisionVardaId = getVardaDecisions().find { it.evakaDecisionId == decisionId }!!.vardaDecisionId
        val placementVardaId = getVardaDecisions().find { it.evakaPlacementId == placementId }!!.vardaDecisionId
        val temporaryPlacementVardaId =
            getVardaDecisions().find { it.evakaPlacementId == temporaryPlacementId }!!.vardaDecisionId

        updateDecisions(db, vardaClient)

        val sentDecisions = mockEndpoint.decisions
        assertNotNull(sentDecisions[decisionVardaId])
        assertNotNull(sentDecisions[placementVardaId])
        assertNotNull(sentDecisions[temporaryPlacementVardaId])

        assertEquals(decisionVardaId, getVardaDecisions().find { it.evakaDecisionId == decisionId }!!.vardaDecisionId)
        assertEquals(
            placementVardaId,
            getVardaDecisions().find { it.evakaPlacementId == placementId }!!.vardaDecisionId
        )
        assertEquals(
            temporaryPlacementVardaId,
            getVardaDecisions().find { it.evakaPlacementId == temporaryPlacementId }!!.vardaDecisionId
        )
    }

    private fun getVardaDecisions() = db.read {
        it.createQuery("SELECT * FROM varda_decision")
            .map(toVardaDecisionRow)
            .toList()
    }

    private fun getSoftDeletedVardaDecisions() = db.read {
        it.createQuery("SELECT * FROM varda_decision WHERE deleted_at IS NOT NULL")
            .map(toVardaDecisionRow)
            .toList()
    }

    private fun updateServiceNeed(id: ServiceNeedId, updatedAt: Instant) = db.transaction {
        it.createUpdate("UPDATE service_need SET updated = :updatedAt WHERE id = :id")
            .bind("id", id)
            .bind("updatedAt", updatedAt)
            .execute()
    }

    private fun updateChildren() {
        updateChildren(db, vardaClient, vardaOrganizerName)
    }
}

internal fun insertServiceNeed(
    db: Database.Connection,
    placementId: PlacementId,
    period: FiniteDateRange,
    optionId: UUID
): ServiceNeedId {
    return db.transaction {
        it.insertTestServiceNeed(
            confirmedBy = testDecisionMaker_1.id,
            placementId = placementId,
            period = period,
            optionId = optionId
        )
    }
}

internal fun insertVardaChild(db: Database.Connection, childId: UUID, createdAt: Instant = Instant.now(), ophOrganizationOid: String = defaultMunicipalOrganizerOid) = db.transaction {
    it.createUpdate(
        """
INSERT INTO varda_child
    (id, person_id, varda_person_id, varda_person_oid, varda_child_id, created_at, modified_at, uploaded_at, oph_organizer_oid)
VALUES
    (:id, :personId, :vardaPersonId, :vardaPersonOid, :vardaChildId, :createdAt, :modifiedAt, :uploadedAt, :ophOrganizerOid)
"""
    )
        .bind("id", UUID.randomUUID())
        .bind("personId", childId)
        .bind("vardaPersonId", 123L)
        .bind("vardaPersonOid", "123")
        .bind("vardaChildId", 123L)
        .bind("createdAt", createdAt)
        .bind("modifiedAt", createdAt)
        .bind("uploadedAt", createdAt)
        .bind("ophOrganizerOid", ophOrganizationOid)
        .execute()
}

fun insertDecisionWithApplication(
    db: Database.Connection,
    child: PersonData.Detailed,
    period: FiniteDateRange,
    unitId: DaycareId = testDaycare.id,
    decisionType: DecisionType = DecisionType.DAYCARE,
    sentDate: LocalDate = LocalDate.of(2019, 1, 1),
    decisionStatus: DecisionStatus = DecisionStatus.ACCEPTED
): UUID = db.transaction {
    val applicationId = it.insertTestApplication(childId = child.id, guardianId = testAdult_1.id, sentDate = sentDate)
    it.insertTestApplicationForm(
        applicationId,
        DaycareFormV0(
            type = ApplicationType.DAYCARE,
            partTime = false,
            connectedDaycare = false,
            serviceStart = "08:00",
            serviceEnd = "16:00",
            careDetails = CareDetails(),
            child = child.toDaycareFormChild(),
            guardian = testAdult_1.toDaycareFormAdult(),
            apply = Apply(preferredUnits = listOf(unitId)),
            preferredStartDate = period.start,
            urgent = false
        )
    )
    val acceptedDecision = TestDecision(
        applicationId = applicationId,
        status = decisionStatus,
        createdBy = testDecisionMaker_1.id,
        unitId = unitId,
        type = decisionType,
        startDate = period.start,
        endDate = period.end,
        resolvedBy = testDecisionMaker_1.id,
        resolved = Instant.now()
    )
    it.insertTestDecision(acceptedDecision)
}

fun insertPlacementWithDecision(db: Database.Connection, child: PersonData.Detailed, unitId: DaycareId, period: FiniteDateRange): Pair<UUID, PlacementId> {
    val decisionId = insertDecisionWithApplication(db, child = child, period = period, unitId = unitId)
    return db.transaction {
        val placementId = it.insertTestPlacement(
            childId = child.id,
            unitId = unitId,
            startDate = period.start,
            endDate = period.end
        )
        it.insertTestServiceNeed(
            confirmedBy = testDecisionMaker_1.id,
            placementId = placementId,
            period = period,
            optionId = snDefaultDaycare.id
        )
        Pair(decisionId, placementId)
    }
}

private fun deletePlacement(db: Database.Connection, id: PlacementId) = db.transaction {
    it.createUpdate("DELETE FROM placement WHERE id = :id")
        .bind("id", id)
        .execute()
}
