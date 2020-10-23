// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.FormType
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
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testPurchasedDaycare
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.varda.integration.MockVardaIntegrationEndpoint
import org.jdbi.v3.core.Handle
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

class VardaDecisionsIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var mockEndpoint: MockVardaIntegrationEndpoint

    @BeforeEach
    fun beforeEach() {
        jdbi.handle(::insertGeneralTestFixtures)
    }

    @AfterEach
    fun afterEach() {
        jdbi.handle(::resetDatabase)
        mockEndpoint.decisions.clear()
    }

    @Test
    fun `municipal daycare decision is sent when it has all required data`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val decisionId = insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertEquals(decisionId, result.first().evakaDecisionId)
        }
    }

    @Test
    fun `PAOS decision is sent when it has all required data`() {
        jdbi.handle { h ->
            val decisionId = insertPlacementWithDecision(h, testChild_1, testPurchasedDaycare.id, ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))).first
            updateChildren(h)

            val children = getUploadedChildren(h)
            assertEquals(1, children.size)

            updateDecisions(h, vardaClient)

            val vardaDecisions = mockEndpoint.decisions
            assertEquals(1, vardaDecisions.size)
            assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, vardaDecisions[0].providerTypeCode)

            val decisionRows = getVardaDecisions(h)
            assertEquals(1, decisionRows.size)
            assertEquals(decisionId, decisionRows.first().evakaDecisionId)
        }
    }

    @Test
    fun `child with municipal and PAOS decisions is handled correctly`() {
        jdbi.handle { h ->
            val firstPeriod = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val municipalDecisionId = insertPlacementWithDecision(h, testChild_1, testDaycare.id, firstPeriod).first
            val paosDecisionId = insertPlacementWithDecision(h, testChild_1, testPurchasedDaycare.id, ClosedPeriod(firstPeriod.end.plusDays(1), firstPeriod.end.plusDays(300))).first
            updateChildren(h)

            updateDecisions(h, vardaClient)

            val decisionRows = getVardaDecisions(h)
            assertEquals(2, decisionRows.size)
            assertNotNull(decisionRows.find { it.evakaDecisionId == municipalDecisionId })
            assertNotNull(decisionRows.find { it.evakaDecisionId == paosDecisionId })

            val children = getUploadedChildren(h)
            val decisions = mockEndpoint.decisions
            assertEquals(2, children.size)
            assertEquals(2, decisions.size)

            val paosChild = children.firstOrNull { it.ophOrganizerOid == defaultPurchasedOrganizerOid }
            assertNotNull(paosChild)

            val paosDecision = decisions.firstOrNull { it.childUrl.contains(paosChild!!.vardaChildId.toString()) }
            assertNotNull(paosDecision)

            val municipalChild = children.firstOrNull { it.ophOrganizerOid == defaultMunicipalOrganizerOid }
            assertNotNull(municipalChild)

            val municipalDecision = decisions.firstOrNull { it.childUrl.contains(municipalChild!!.vardaChildId.toString()) }
            assertNotNull(municipalDecision)

            assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, paosDecision!!.providerTypeCode)
            assertEquals(VardaUnitProviderType.MUNICIPAL.vardaCode, municipalDecision!!.providerTypeCode)
        }
    }

    @Test
    fun `a daycare decision is not sent when there is no existing service need`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `a daycare decision is not sent when service need does not overlap with decision`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, ClosedPeriod(period.start.minusYears(1), period.end.minusYears(1)))
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `a daycare decision is not sent when the child has not been imported to varda yet`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `preschool decisions are not sent`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `a preschool daycare decision is sent`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val decisionId =
                insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertEquals(decisionId, result.first().evakaDecisionId)
        }
    }

    @Test
    fun `a daycare decision is updated when a new service need is created for the period`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val serviceNeedEndDate = period.start.plusMonths(1)
            insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            insertServiceNeed(h, testChild_1.id, period.copy(end = serviceNeedEndDate))
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalUploadedAt = getVardaDecisions(h).first().uploadedAt

            insertServiceNeed(h, testChild_1.id, period.copy(start = serviceNeedEndDate.plusDays(1)))
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertTrue(originalUploadedAt < result.first().uploadedAt)
        }
    }

    @Test
    fun `a daycare decision is updated when service need is updated`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            val serviceNeedId = insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalUploadedAt = getVardaDecisions(h).first().uploadedAt

            updateServiceNeed(h, serviceNeedId, originalUploadedAt.plusSeconds(1))
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertTrue(originalUploadedAt < result.first().uploadedAt)
        }
    }

    @Test
    fun `a daycare decision is updated when a new overlapping decision is made`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalUploadedAt = getVardaDecisions(h).first().uploadedAt

            insertDecisionWithApplication(h, testChild_1, period.copy(start = period.start.plusMonths(1)))
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(2, result.size)
            assertTrue(result.all { originalUploadedAt < it.uploadedAt })
        }
    }

    @Test
    fun `a daycare decision is deleted when a new decision that completely replaced the old one is made`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val firstDecisionId =
                insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalDecisionId = getVardaDecisions(h).first().evakaDecisionId
            assertEquals(firstDecisionId, originalDecisionId)

            val secondDecisionId = insertDecisionWithApplication(h, testChild_1, period)
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertNotEquals(originalDecisionId, result.first().evakaDecisionId)
            assertEquals(secondDecisionId, result.first().evakaDecisionId)
        }
    }

    @Test
    fun `a derived daycare decision is sent when there is a placement without a decision`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val placementId = insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertEquals(placementId, result.first().evakaPlacementId)
        }
    }

    @Test
    fun `multiple decisions are created from multiple derived daycare decisions`() {
        jdbi.handle { h ->
            insertVardaChild(h, testChild_1.id)
            val firstSemester = ClosedPeriod(LocalDate.of(2018, 1, 8), LocalDate.of(2019, 5, 31))
            val firstSummer = ClosedPeriod(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 7, 31))
            insertPlacement(h, testChild_1.id, firstSemester, PlacementType.PRESCHOOL_DAYCARE)
            insertPlacement(h, testChild_1.id, firstSummer, PlacementType.DAYCARE)
            insertServiceNeed(h, testChild_1.id, firstSemester)
            insertServiceNeed(h, testChild_1.id, firstSummer)

            val secondSemester = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 5, 31))
            val secondSummer = ClosedPeriod(LocalDate.of(2020, 6, 1), LocalDate.of(2020, 7, 31))
            insertPlacement(h, testChild_1.id, secondSemester, PlacementType.PRESCHOOL_DAYCARE)
            insertPlacement(h, testChild_1.id, secondSummer, PlacementType.DAYCARE)
            insertServiceNeed(h, testChild_1.id, secondSemester)
            insertServiceNeed(h, testChild_1.id, secondSummer)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(4, result.size)
        }
    }

    @Test
    fun `a derived daycare decision is updated when the placement is updated`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val placementId = insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalUploadedAt = getVardaDecisions(h).first().uploadedAt

            updatePlacement(h, placementId, originalUploadedAt.plusSeconds(1))
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertTrue(originalUploadedAt < result.first().uploadedAt)
        }
    }

    @Test
    fun `a derived daycare decision is updated when service need is updated`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertPlacement(h, testChild_1.id, period)
            val serviceNeedId = insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val originalUploadedAt = getVardaDecisions(h).first().uploadedAt

            updateServiceNeed(h, serviceNeedId, originalUploadedAt.plusSeconds(1))
            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
            assertTrue(originalUploadedAt < result.first().uploadedAt)
        }
    }

    @Test
    fun `a derived daycare decision is deleted when the placement is removed`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            val placementId = insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val beforeDelete = getVardaDecisions(h)
            assertEquals(1, beforeDelete.size)

            deletePlacement(h, placementId)
            updateDecisions(h, vardaClient)
            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `a derived decision is sent when child has a rejected decision`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)
            insertDecisionWithApplication(
                h = h,
                child = testChild_1,
                period = period,
                decisionStatus = DecisionStatus.REJECTED
            )

            updateDecisions(h, vardaClient)
            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `a derived decision is sent when child has a pending decision`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)
            insertDecisionWithApplication(
                h = h,
                child = testChild_1,
                period = period,
                decisionStatus = DecisionStatus.PENDING
            )

            updateDecisions(h, vardaClient)
            val result = getVardaDecisions(h)
            assertEquals(1, result.size)
        }
    }

    @Test
    fun `daycare decision is sent with correct data`() {
        jdbi.handle { h ->
            val sentDate = LocalDate.of(2019, 6, 1)
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, sentDate = sentDate)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val decisions = mockEndpoint.decisions
            assertEquals(1, decisions.size)

            val decision = decisions[0]
            assertEquals(period.start, decision.startDate)
            assertEquals(period.end, decision.endDate)
            assertEquals(40.0, decision.hoursPerWeek)
            assertEquals(true, decision.daily)
            assertEquals(true, decision.fullDay)
            assertEquals(false, decision.shiftCare)
            assertEquals("jm01", decision.providerTypeCode)
            assertEquals(false, decision.urgent)
            assertEquals(sentDate, decision.applicationDate)
        }
    }

    @Test
    fun `PAOS daycare decision is sent with correct data`() {
        jdbi.handle { h ->
            val sentDate = LocalDate.of(2019, 6, 1)
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, sentDate = sentDate, unitId = testPurchasedDaycare.id)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id, ophOrganizationOid = defaultPurchasedOrganizerOid)

            updateDecisions(h, vardaClient)

            val decisions = mockEndpoint.decisions
            assertEquals(1, decisions.size)

            val decision = decisions[0]
            assertEquals(period.start, decision.startDate)
            assertEquals(period.end, decision.endDate)
            assertEquals(40.0, decision.hoursPerWeek)
            assertEquals(true, decision.daily)
            assertEquals(true, decision.fullDay)
            assertEquals(false, decision.shiftCare)
            assertEquals(VardaUnitProviderType.PURCHASED.vardaCode, decision.providerTypeCode)
            assertEquals(false, decision.urgent)
            assertEquals(sentDate, decision.applicationDate)
        }
    }

    @Test
    fun `a preschool daycare decision without preparatory is sent with correct hours per week`() {
        jdbi.handle { h ->
            val weeklyHours = 50.0
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            insertServiceNeed(h, testChild_1.id, period, weeklyHours)
            insertVardaChild(h, testChild_1.id)
            insertPlacement(h, testChild_1.id, period, type = PlacementType.PRESCHOOL_DAYCARE)
            updateDecisions(h, vardaClient)

            val decisions = mockEndpoint.decisions
            assertEquals(1, decisions.size)
            assertEquals(weeklyHours - 20, decisions[0].hoursPerWeek)
        }
    }

    @Test
    fun `a preschool daycare decision with preparatory is sent with correct hours per week`() {
        jdbi.handle { h ->
            val weeklyHours = 50.0
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period, decisionType = DecisionType.PRESCHOOL_DAYCARE)
            insertServiceNeed(h, testChild_1.id, period, weeklyHours)
            insertVardaChild(h, testChild_1.id)
            insertPlacement(h, testChild_1.id, period, type = PlacementType.PREPARATORY_DAYCARE)
            updateDecisions(h, vardaClient)

            val decisions = mockEndpoint.decisions
            assertEquals(1, decisions.size)
            assertEquals(weeklyHours - 25, decisions[0].hoursPerWeek)
        }
    }

    @Test
    fun `a decision has a correct end date when another decision replaces it midway`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertDecisionWithApplication(h, testChild_1, period.copy(start = period.start.plusMonths(1)))
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val result = mockEndpoint.decisions.toList().sortedBy { it.startDate }
            assertEquals(2, result.size)
            assertEquals(period.start, result[0].startDate)
            assertEquals(period.start.plusMonths(1).minusDays(1), result[0].endDate)
            assertEquals(period.start.plusMonths(1), result[1].startDate)
            assertEquals(period.end, result[1].endDate)
        }
    }

    @Test
    fun `a decision is not affected by a new decision that does not overlap with it`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertDecisionWithApplication(
                h,
                testChild_1,
                ClosedPeriod(period.end.plusDays(1), period.end.plusMonths(1))
            )
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val result = mockEndpoint.decisions[0]
            assertEquals(period.start, result.startDate)
            assertEquals(period.end, result.endDate)
        }
    }

    @Test
    fun `a decision is not full day with 25 hours`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period, hours = 25.0)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val result = mockEndpoint.decisions[0]
            assertEquals(false, result.fullDay)
        }
    }

    @Test
    fun `a decision is full day with more than 25 hours`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period, hours = 25.5)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)
            val result = mockEndpoint.decisions[0]
            assertEquals(true, result.fullDay)
        }
    }

    @Test
    fun `decision in the future is not sent to Varda`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2))
            insertDecisionWithApplication(h, testChild_1, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `derived decision in the future is not sent to Varda`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.now().plusDays(1), LocalDate.now().plusMonths(2))
            insertPlacement(h, testChild_1.id, period)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            val result = getVardaDecisions(h)
            assertEquals(0, result.size)
        }
    }

    @Test
    fun `only one decision is sent to Varda when it has multiple placements inside it`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaUnit(h)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)
            val newMiddleStart = period.start.plusMonths(1)
            val newMiddleEnd = period.end.minusMonths(1)
            insertPlacement(h, testChild_1.id, ClosedPeriod(period.start, newMiddleStart.minusDays(1)))
            insertPlacement(h, testChild_1.id, ClosedPeriod(newMiddleStart, newMiddleEnd))
            insertPlacement(h, testChild_1.id, ClosedPeriod(newMiddleEnd.plusDays(1), period.end))

            updateDecisions(h, vardaClient)

            assertEquals(1, getVardaDecisions(h).size)
            val decisions = mockEndpoint.decisions
            assertEquals(1, decisions.size)

            val decision = decisions[0]
            assertEquals(period.start, decision.startDate)
            assertEquals(period.end, decision.endDate)
        }
    }

    @Test
    fun `a decision dates are prolonged when its placement is prolonged`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaUnit(h)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)
            updateDecisions(h, vardaClient)
            val decisions = getVardaDecisions(h)
            assertEquals(1, decisions.size)

            val placementId = insertPlacement(h, testChild_1.id, period)
            updatePlacements(h, vardaClient)
            val placements = getVardaPlacements(h)
            assertEquals(1, placements.size)
            assertEquals(placementId, placements.first().evakaPlacementId)
            assertEquals(decisions.first().id, placements.first().decisionId)

            val newStart = period.start.minusMonths(1)
            val newEnd = period.end.plusMonths(1)
            h.updatePlacementStartAndEndDate(placementId, newStart, newEnd)
            updateDecisions(h, vardaClient)

            val vardaDecisions = mockEndpoint.decisions
            assertEquals(1, vardaDecisions.size)
            assertEquals(newStart, vardaDecisions.first().startDate)
            assertEquals(newEnd, vardaDecisions.first().endDate)
        }
    }

    @Test
    fun `decision has correct dates when multiple placements that prolong it are added`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaUnit(h)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            assertEquals(1, getVardaDecisions(h).size)
            assertEquals(1, mockEndpoint.decisions.size)
            val initialDecision = mockEndpoint.decisions[0]
            assertEquals(period.start, initialDecision.startDate)
            assertEquals(period.end, initialDecision.endDate)

            val newStart = period.start.minusMonths(1)
            val newEnd = period.end.plusMonths(1)
            val newMiddleStart = period.start.plusMonths(1)
            val newMiddleEnd = period.end.minusMonths(1)
            insertPlacement(h, testChild_1.id, ClosedPeriod(newStart, newMiddleStart.minusDays(1)))
            insertPlacement(h, testChild_1.id, ClosedPeriod(newMiddleStart, newMiddleEnd))
            insertPlacement(h, testChild_1.id, ClosedPeriod(newMiddleEnd.plusDays(1), newEnd))

            updateDecisions(h, vardaClient)

            assertEquals(1, getVardaDecisions(h).size)
            assertEquals(1, mockEndpoint.decisions.size)
            val finalDecision = mockEndpoint.decisions[0]
            assertEquals(newStart, finalDecision.startDate)
            assertEquals(newEnd, finalDecision.endDate)
        }
    }

    @Test
    fun `decision is soft deleted if it is flagged with should_be_deleted`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaUnit(h)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            assertEquals(1, getVardaDecisions(h).size)
            assertEquals(0, getSoftDeletedVardaDecisions(h).size)

            h.createUpdate("UPDATE varda_decision SET should_be_deleted = true").execute()

            removeMarkedDecisions(h, vardaClient)
            updateDecisions(h, vardaClient)

            assertEquals(1, getSoftDeletedVardaDecisions(h).size)
        }
    }

    @Test
    fun `decision is not updated if upload flag is turned off`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))
            insertDecisionWithApplication(h, testChild_1, period)
            insertVardaUnit(h, unitId = testDaycare.id)
            insertServiceNeed(h, testChild_1.id, period)
            insertVardaChild(h, testChild_1.id)

            updateDecisions(h, vardaClient)

            h.createUpdate("UPDATE daycare SET upload_to_varda = false WHERE id = :id").bind("id", testDaycare.id).execute()

            val newStart = period.start.minusMonths(1)
            insertPlacement(h, testChild_1.id, ClosedPeriod(newStart, period.end))
            updateDecisions(h, vardaClient)

            val decision = mockEndpoint.decisions[0]
            assertEquals(period.start, decision.startDate)
        }
    }

    @Test
    fun `updating daycare organizer oid yields new varda decision if old is soft deleted`() {
        jdbi.handle { h ->
            val period = ClosedPeriod(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 7, 31))

            insertPlacementWithDecision(h, child = testChild_1, unitId = testDaycare.id, period = period)
            updateChildren(h)
            updateDecisions(h, vardaClient)

            assertEquals(1, getVardaDecisions(h).size)

            h.createUpdate("update varda_decision set deleted = NOW()").execute()

            h.createUpdate("UPDATE daycare SET oph_organizer_oid = '1.22.333.4444.1' where id = :id")
                .bind("id", testDaycare.id)
                .execute()

            updateChildren(h)
            updateDecisions(h, vardaClient)
            assertEquals(2, getVardaDecisions(h).size)
        }
    }

    private fun getVardaDecisions(h: Handle) = h.createQuery("SELECT * FROM varda_decision")
        .map(toVardaDecisionRow)
        .toList()

    private fun getSoftDeletedVardaDecisions(h: Handle) = h.createQuery("SELECT * FROM varda_decision WHERE deleted IS NOT NULL")
        .map(toVardaDecisionRow)
        .toList()

    private fun updateServiceNeed(h: Handle, id: UUID, updatedAt: Instant) {
        h.createUpdate("UPDATE service_need SET updated = :updatedAt WHERE id = :id")
            .bind("id", id)
            .bind("updatedAt", updatedAt)
            .execute()
    }

    private fun updateChildren(h: Handle) {
        updateChildren(h, vardaClient, vardaOrganizerName)
    }
}

internal fun insertServiceNeed(
    h: Handle,
    childId: UUID,
    period: ClosedPeriod,
    hours: Double = 40.0
): UUID {
    return insertTestServiceNeed(
        h,
        childId,
        testDecisionMaker_1.id,
        startDate = period.start,
        endDate = period.end,
        hoursPerWeek = hours
    )
}

internal fun insertVardaChild(h: Handle, childId: UUID, createdAt: Instant = Instant.now(), ophOrganizationOid: String = defaultMunicipalOrganizerOid) {
    h.createUpdate(
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
    h: Handle,
    child: PersonData.Detailed,
    period: ClosedPeriod,
    unitId: UUID = testDaycare.id,
    decisionType: DecisionType = DecisionType.DAYCARE,
    sentDate: LocalDate = LocalDate.of(2019, 1, 1),
    decisionStatus: DecisionStatus = DecisionStatus.ACCEPTED
): UUID {
    val applicationId = insertTestApplication(h, childId = child.id, sentDate = sentDate)
    insertTestApplicationForm(
        h, applicationId,
        DaycareFormV0(
            type = FormType.DAYCARE,
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
    return h.insertTestDecision(acceptedDecision)
}

fun insertPlacementWithDecision(h: Handle, child: PersonData.Detailed, unitId: UUID, period: ClosedPeriod): Pair<UUID, UUID> {
    val decisionId = insertDecisionWithApplication(h = h, child = child, period = period, unitId = unitId)
    insertServiceNeed(h, child.id, period)
    val placementId = insertTestPlacement(
        h = h,
        childId = child.id,
        unitId = unitId,
        startDate = period.start,
        endDate = period.end
    )
    return Pair(decisionId, placementId)
}

private fun deletePlacement(h: Handle, id: UUID) {
    h.createUpdate("DELETE FROM placement WHERE id = :id")
        .bind("id", id)
        .execute()
}
