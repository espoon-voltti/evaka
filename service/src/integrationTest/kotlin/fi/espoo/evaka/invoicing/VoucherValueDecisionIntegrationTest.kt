// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptionVoucherValues
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.data.PagedVoucherValueDecisionSummaries
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementCreateRequestBody
import fi.espoo.evaka.placement.PlacementResponse
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.testVoucherDaycare2
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var emailMessageProvider: IEmailMessageProvider
    @Autowired lateinit var emailEnv: EmailEnv

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.clearMessages()
        MockEmailClient.clear()

        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testDecisionMaker_2)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testVoucherDaycare.copy(financeDecisionHandler = testDecisionMaker_2.id))
            tx.insert(testVoucherDaycare2)
            listOf(
                    testAdult_1,
                    testAdult_2,
                    testAdult_3,
                    testAdult_4,
                    testAdult_5,
                    testAdult_6,
                    testAdult_7,
                )
                .forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOptionVoucherValues()
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = testChild_1.dateOfBirth,
                    endDate = testChild_1.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            tx.insertTestPartnership(adult1 = testAdult_1.id, adult2 = testAdult_2.id)
        }
    }

    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(9, 0))
    private val startDate = now.toLocalDate().minusMonths(1)
    private val endDate = now.toLocalDate().plusMonths(6)

    @Test
    fun `value decision is created after a placement is created`() {
        createPlacement(startDate, endDate)

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.DRAFT, decisions.first().status)
        }
    }

    @Test
    fun `value decision is created and it can be sent after a placement is created`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
        }
    }

    @Test
    fun `send voucher value decisions returns bad request when some decisions being in the future`() {
        val startDate =
            now.toLocalDate().plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance + 1)
        val endDate = startDate.plusMonths(6)
        createPlacement(startDate, endDate)
        sendAllValueDecisions(400, "voucherValueDecisions.confirmation.tooFarInFuture")

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.DRAFT, decisions.first().status)
        }
    }

    @Test
    fun `send voucher value decisions when decision at last possible confirmation date exists`() {
        val startDate =
            now.toLocalDate().plusDays(evakaEnv.nrOfDaysVoucherValueDecisionCanBeSentInAdvance)
        val endDate = startDate.plusMonths(6)
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
        }
    }

    @Test
    fun `sent value decision validity period ends after cleanup when corresponding placement has its past end date updated`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newEndDate = now.toLocalDate().minusDays(1)
        updatePlacement(placementId, startDate, newEndDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(newEndDate, decisions.first().validTo)
        }
    }

    @Test
    fun `sent value decision is annulled when a child's head of family changes`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }

        changeHeadOfFamily(testChild_1, testAdult_5.id)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            val annulled = decisions.find { it.status == VoucherValueDecisionStatus.ANNULLED }
            assertNotNull(annulled)
            assertEquals(testAdult_1.id, annulled.headOfFamilyId)
            val sent = decisions.find { it.status == VoucherValueDecisionStatus.SENT }
            assertNotNull(sent)
            assertEquals(testAdult_5.id, sent.headOfFamilyId)
        }
    }

    @Test
    fun `value decision handler is set to approver for relief decision`() {
        createPlacement(startDate, endDate)
        db.transaction {
            val decisionType = VoucherValueDecisionType.RELIEF_ACCEPTED
            val childId = testChild_1.id
            it.execute {
                sql(
                    "UPDATE voucher_value_decision d SET decision_type = ${bind(decisionType)} WHERE child_id = ${bind(childId)}"
                )
            }

            it.execute {
                sql(
                    "UPDATE daycare SET finance_decision_handler = ${bind(testDecisionMaker_2.id)} WHERE id = ${bind(testVoucherDaycare.id)}"
                )
            }
        }

        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                decisions.first().status,
            )
            assertEquals(financeWorker.id.raw, decisions.first().decisionHandler)
        }
    }

    @Test
    fun `value decision drafts not overlapping the period have the same id and created date after generating new value decision drafts`() {
        createPlacement(startDate, endDate)
        val initialDecisionDraft = getAllValueDecisions().first()

        createPlacement(startDate.plusYears(1), endDate.plusYears(1))
        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            assertEquals(
                1,
                decisions.count {
                    it.id == initialDecisionDraft.id && it.created == initialDecisionDraft.created
                },
            )
        }
    }

    @Test
    fun `value decision is replaced when child is placed into another voucher unit`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, testVoucherDaycare2.id)

        sendAllValueDecisions()
        getAllValueDecisions()
            .sortedBy { it.validFrom }
            .let { decisions ->
                assertEquals(2, decisions.size)

                assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
                assertEquals(startDate, decisions.first().validFrom)
                assertEquals(newStartDate.minusDays(1), decisions.first().validTo)

                assertEquals(VoucherValueDecisionStatus.SENT, decisions.last().status)
                assertEquals(newStartDate, decisions.last().validFrom)
                assertEquals(endDate, decisions.last().validTo)
            }
    }

    @Test
    fun `value decision cleanup works when child is placed into another municipal unit`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, testDaycare.id)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(startDate, decisions.first().validFrom)
            assertEquals(newStartDate.minusDays(1), decisions.first().validTo)
        }
    }

    @Test
    fun `value decision cleanup works when child's placement is deleted`() {
        val placementId = createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }

        deletePlacement(placementId)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.ANNULLED, decisions.first().status)
            assertEquals(testAdult_1.id, decisions.first().headOfFamilyId)
        }
    }

    @Test
    fun `value decision is ended when child switches to preschool`() {
        createPlacement(startDate, endDate, type = PlacementType.DAYCARE)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
            assertEquals(endDate, decisions.first().validTo)
        }

        val newStartDate = now.toLocalDate().minusDays(1)
        createPlacement(newStartDate, endDate, type = PlacementType.PRESCHOOL)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(startDate, decisions.first().validFrom)
            assertEquals(newStartDate.minusDays(1), decisions.first().validTo)
        }
    }

    @Test
    fun `value decision search`() {
        createPlacement(startDate, endDate)

        assertEquals(1, searchValueDecisions(status = "DRAFT").total)
        assertEquals(
            1,
            searchValueDecisions(status = "DRAFT", searchTerms = "Ricky").total,
        ) // child
        assertEquals(1, searchValueDecisions(status = "DRAFT", searchTerms = "John").total) // head
        assertEquals(
            1,
            searchValueDecisions(status = "DRAFT", searchTerms = "Joan").total,
        ) // partner
        assertEquals(
            0,
            searchValueDecisions(status = "DRAFT", searchTerms = "Foobar").total,
        ) // no match
        assertEquals(0, searchValueDecisions(status = "SENT").total)

        sendAllValueDecisions()
        assertEquals(0, searchValueDecisions(status = "DRAFT").total)
        assertEquals(1, searchValueDecisions(status = "SENT").total)
        assertEquals(1, searchValueDecisions(status = "SENT", searchTerms = "Ricky").total)
        assertEquals(0, searchValueDecisions(status = "SENT", searchTerms = "Foobar").total)
    }

    @Test
    fun `filter out starting placements`() {
        val placementId =
            createPlacement(now.toLocalDate().minusMonths(1), now.toLocalDate().plusMonths(1))

        sendAllValueDecisions()

        searchValueDecisions("SENT", "", """["NO_STARTING_PLACEMENTS"]""").let { decisions ->
            assertEquals(1, decisions.data.size)
        }

        db.transaction {
            it.execute {
                sql(
                    "UPDATE placement SET start_date = ${bind(now.toLocalDate())} WHERE id = ${bind(placementId)}"
                )
            }
        }

        searchValueDecisions("SENT", "", """["NO_STARTING_PLACEMENTS"]""").let { decisions ->
            assertEquals(0, decisions.data.size)
        }
    }

    @Test
    fun `PDF can be downloaded`() {
        createPlacement(startDate, endDate)
        val decisionId = sendAllValueDecisions().first()

        assertEquals(200, getPdfStatus(decisionId, financeWorker))
    }

    @Test
    fun `Legacy PDF can be downloaded`() {
        createPlacement(startDate, endDate)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertEquals(200, getPdfStatus(decisionId, financeWorker))
    }

    @Test
    fun `Legacy PDF can not be downloaded if head of family has restricted details`() {
        db.transaction {
            // testAdult_7 has restricted details on
            it.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_7.id,
                    startDate = testChild_2.dateOfBirth,
                    endDate = testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = testAdult_7.id, adult2 = testAdult_3.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertEquals(403, getPdfStatus(decisionId, financeWorker))

        // Check that message is still sent via sfi
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        assertEquals(1, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `Legacy PDF can not be downloaded if child has restricted details`() {
        val testChildRestricted =
            testChild_1.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
            )

        db.transaction {
            it.insert(testChildRestricted, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = testChildRestricted.id,
                    headOfChildId = testAdult_3.id,
                    startDate = testChildRestricted.dateOfBirth,
                    endDate = testChildRestricted.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }
        createPlacement(startDate, endDate, childId = testChildRestricted.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertEquals(403, getPdfStatus(decisionId, financeWorker))
    }

    @Test
    fun `PDF without contact info can be downloaded even if head of family has restricted details`() {
        db.transaction {
            // testAdult_7 has restricted details on
            it.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_7.id,
                    startDate = testChild_2.dateOfBirth,
                    endDate = testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = testAdult_7.id, adult2 = testAdult_3.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()

        assertEquals(200, getPdfStatus(decisionId, financeWorker))
    }

    @Test
    fun `PDF without contact info can be downloaded even if child has restricted details`() {
        val testChildRestricted =
            testChild_1.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
            )

        db.transaction {
            it.insert(testChildRestricted, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = testChildRestricted.id,
                    headOfChildId = testAdult_3.id,
                    startDate = testChildRestricted.dateOfBirth,
                    endDate = testChildRestricted.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }
        createPlacement(startDate, endDate, childId = testChildRestricted.id)
        val decisionId = sendAllValueDecisions().first()

        assertEquals(200, getPdfStatus(decisionId, financeWorker))
    }

    @Test
    fun `Legacy PDF can be downloaded by admin even if someone in the family has restricted details`() {
        db.transaction {
            // testAdult_7 has restricted details on
            it.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_7.id,
                    startDate = testChild_2.dateOfBirth,
                    endDate = testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = testAdult_7.id, adult2 = testAdult_3.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertEquals(200, getPdfStatus(decisionId, adminUser))
    }

    @Test
    fun `VoucherValueDecision handler is set to the user when decision is not normal`() {
        val approvedDecision = createReliefDecision(false)
        assertEquals(testDecisionMaker_1.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `VoucherValueDecision handler is set to the daycare handler when forced when decision is not normal`() {
        val approvedDecision = createReliefDecision(true)
        assertEquals(testDecisionMaker_2.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_SENDING is set to SENT`() {
        // optInAdult has an email address, and does not require manual sending of PDF decision
        val optInAdult =
            testAdult_6.copy(
                id = PersonId(UUID.randomUUID()),
                email = "optin@test.com",
                forceManualFeeDecisions = false,
                ssn = "291090-9986",
                enabledEmailTypes = listOf(EmailMessageType.DECISION_NOTIFICATION),
            )
        db.transaction {
            it.insert(optInAdult, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    optInAdult.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = optInAdult.id, adult2 = testAdult_7.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        val emailContent =
            emailMessageProvider.financeDecisionNotification(
                FinanceDecisionType.VOUCHER_VALUE_DECISION
            )

        assertEquals(
            setOfNotNull(optInAdult.email),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(emailContent.subject, getEmailFor(optInAdult).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(optInAdult).fromAddress,
        )
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_MANUAL_SENDING is set to SENT`() {
        db.transaction {
            // testAdult_3 has an email address, but no mail address -> marked for manual sending
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    testAdult_3.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = testAdult_3.id, adult2 = testAdult_4.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        // assert that no emails sent yet
        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())

        markValueDecisionsSent(listOf(decisionId))
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        val emailContent =
            emailMessageProvider.financeDecisionNotification(
                FinanceDecisionType.VOUCHER_VALUE_DECISION
            )

        assertEquals(
            setOfNotNull(testAdult_3.email),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(emailContent.subject, getEmailFor(testAdult_3).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(testAdult_3).fromAddress,
        )
    }

    @Test
    fun `Email notification is not sent to hof when opted out of decision emails`() {
        // optOutAdult is eligible, but has elected to not receive decision emails
        val optOutAdult =
            testAdult_6.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "291090-9986",
                email = "optout@test.com",
                forceManualFeeDecisions = false,
                enabledEmailTypes = listOf(),
            )
        db.transaction {
            it.insert(optOutAdult, DevPersonType.RAW_ROW)

            // optOutAdult has an email address, and does not require manual sending of PDF decision
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    optOutAdult.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = optOutAdult.id, adult2 = testAdult_7.id)
        }
        createPlacement(startDate, endDate, childId = testChild_2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())
    }

    fun createReliefDecision(forceDaycareHandler: Boolean): VoucherValueDecision {
        createPlacement(startDate, endDate)
        val decision = getAllValueDecisions().getOrNull(0)!!

        db.transaction {
            it.execute {
                sql(
                    "UPDATE voucher_value_decision SET decision_type='RELIEF_ACCEPTED' WHERE id = ${bind(decision.id)}"
                )
            }
        }

        db.transaction {
            it.approveValueDecisionDraftsForSending(
                listOf(decision.id),
                testDecisionMaker_1.id,
                HelsinkiDateTime.now(),
                null,
                forceDaycareHandler,
            )
        }

        return getAllValueDecisions().getOrNull(0)!!
    }

    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val financeWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val adminUser =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    private fun createPlacement(
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = testVoucherDaycare.id,
        childId: ChildId = testChild_1.id,
        type: PlacementType = PlacementType.DAYCARE,
    ): PlacementId {
        val body =
            PlacementCreateRequestBody(
                type = type,
                childId = childId,
                unitId = unitId,
                startDate = startDate,
                endDate = endDate,
                placeGuarantee = false,
            )

        http
            .post("/placements")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()
            .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        val (_, _, data) =
            http
                .get("/placements", listOf("childId" to childId))
                .asUser(serviceWorker)
                .responseObject<PlacementResponse>(jsonMapper)

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        return data.get().placements.first().id
    }

    private fun updatePlacement(id: PlacementId, startDate: LocalDate, endDate: LocalDate) {
        val body = PlacementUpdateRequestBody(startDate = startDate, endDate = endDate)

        http
            .put("/placements/$id")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .responseObject<Placement>(jsonMapper)
            .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun deletePlacement(id: PlacementId) {
        http.delete("/placements/$id").asUser(serviceWorker).withMockedTime(now).response().also {
            (_, res, _) ->
            assertEquals(200, res.statusCode)
        }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun changeHeadOfFamily(child: DevPerson, headOfFamilyId: PersonId) {
        db.transaction {
            it.execute { sql("DELETE FROM fridge_child WHERE child_id = ${bind(child.id)}") }
        }

        val body =
            ParentshipController.ParentshipRequest(
                childId = child.id,
                headOfChildId = headOfFamilyId,
                startDate = child.dateOfBirth,
                endDate = child.dateOfBirth.plusYears(18).minusDays(1),
            )

        http
            .post("/parentships")
            .asUser(serviceWorker)
            .withMockedTime(now)
            .objectBody(body, mapper = jsonMapper)
            .response()

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
    }

    private fun searchValueDecisions(
        status: String,
        searchTerms: String = "",
        distinctionsString: String = "[]",
    ): PagedVoucherValueDecisionSummaries {
        val (_, _, data) =
            http
                .post("/value-decisions/search")
                .jsonBody(
                    """{"page": 0, "pageSize": 100, "statuses": ["$status"], "searchTerms": "$searchTerms", "distinctions": $distinctionsString}"""
                )
                .withMockedTime(now)
                .asUser(financeWorker)
                .responseObject<PagedVoucherValueDecisionSummaries>(jsonMapper)
        return data.get()
    }

    private fun sendAllValueDecisions(
        expectedStatusCode: Int = 200,
        expectedErrorCode: String? = null,
    ): List<VoucherValueDecisionId> {
        val (_, _, data) =
            http
                .post("/value-decisions/search")
                .jsonBody("""{"page": 0, "pageSize": 100, "statuses": ["DRAFT"]}""")
                .withMockedTime(now)
                .asUser(financeWorker)
                .responseObject<PagedVoucherValueDecisionSummaries>(jsonMapper)
                .also { (_, res, _) -> assertEquals(200, res.statusCode) }

        val decisionIds = data.get().data.map { it.id }
        http
            .post("/value-decisions/send")
            .objectBody(decisionIds, mapper = jsonMapper)
            .withMockedTime(now)
            .asUser(financeWorker)
            .response()
            .also { (_, res, _) ->
                assertEquals(expectedStatusCode, res.statusCode)
                if (expectedStatusCode == 400) {
                    val responseJson = res.body().asString("application/json")
                    val errorCode = jsonMapper.readTree(responseJson).get("errorCode").textValue()
                    assertEquals(expectedErrorCode, errorCode)
                }
            }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        return decisionIds
    }

    private fun getAllValueDecisions(): List<VoucherValueDecision> {
        return db.read {
                it.createQuery { sql("SELECT * FROM voucher_value_decision") }
                    .toList<VoucherValueDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }

    private fun getPdfStatus(
        decisionId: VoucherValueDecisionId,
        user: AuthenticatedUser.Employee,
    ): Int {
        val (_, response, _) = http.get("/value-decisions/pdf/$decisionId").asUser(user).response()
        return response.statusCode
    }

    private fun Database.Transaction.setDocumentContainsContactInfo(id: VoucherValueDecisionId) =
        execute {
            sql(
                """
        UPDATE voucher_value_decision
        SET document_contains_contact_info = TRUE
        WHERE id = ${bind(id)}
    """
            )
        }

    private fun markValueDecisionsSent(
        decisionIds: List<VoucherValueDecisionId>,
        expectedStatusCode: Int = 200,
        expectedErrorCode: String? = null,
    ) {
        http
            .post("/value-decisions/mark-sent")
            .objectBody(decisionIds, mapper = jsonMapper)
            .withMockedTime(now)
            .asUser(financeWorker)
            .response()
            .also { (_, res, _) ->
                assertEquals(expectedStatusCode, res.statusCode)
                if (expectedStatusCode == 400) {
                    val responseJson = res.body().asString("application/json")
                    val errorCode = jsonMapper.readTree(responseJson).get("errorCode").textValue()
                    assertEquals(expectedErrorCode, errorCode)
                }
            }
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }
}
