// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.ProcessMetadataController
import fi.espoo.evaka.caseprocess.getCaseProcessByVoucherValueDecisionId
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptionVoucherValues
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.controller.SearchVoucherValueDecisionRequest
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionDistinctiveParams
import fi.espoo.evaka.invoicing.data.PagedVoucherValueDecisionSummaries
import fi.espoo.evaka.invoicing.data.approveValueDecisionDraftsForSending
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.controllers.ParentshipController
import fi.espoo.evaka.placement.PlacementController
import fi.espoo.evaka.placement.PlacementCreateRequestBody
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.SfiAsyncJobs
import fi.espoo.evaka.sficlient.getSfiMessageEventsByMessageId
import fi.espoo.evaka.sficlient.rest.EventType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired lateinit var emailMessageProvider: IEmailMessageProvider
    @Autowired lateinit var emailEnv: EmailEnv
    @Autowired lateinit var processMetadataController: ProcessMetadataController
    @Autowired private lateinit var sfiAsyncJobs: SfiAsyncJobs
    @Autowired private lateinit var placementController: PlacementController
    @Autowired private lateinit var parentshipController: ParentshipController
    @Autowired private lateinit var voucherValueDecisionController: VoucherValueDecisionController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, name = "Municipal Daycare")
    private val decisionMaker = DevEmployee()
    private val decisionMaker2 = DevEmployee()
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val voucherDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Voucher Daycare",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            financeDecisionHandler = decisionMaker2.id,
        )
    private val voucherDaycare2 =
        DevDaycare(
            areaId = area.id,
            name = "Voucher Daycare 2",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )

    // Head of family — needs SSN and address for PDF generation
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 =
        DevPerson(
            ssn = "010280-952L",
            firstName = "Joan",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )

    // Has email but no SSN/address — triggers manual sending
    private val adult3 =
        DevPerson(firstName = "Mark", lastName = "Foo", email = "mark.foo@example.com")
    private val adult4 = DevPerson()

    // Alternative head of family — needs SSN and address for PDF
    private val adult5 =
        DevPerson(
            ssn = "070644-937X",
            streetAddress = "Kamreerintie 1",
            postalCode = "00340",
            postOffice = "Espoo",
        )

    // Restricted details enabled — needs SSN and address
    private val adult7 =
        DevPerson(
            ssn = "010180-969B",
            streetAddress = "Suojatie 112",
            postalCode = "02230",
            postOffice = "Espoo",
            restrictedDetailsEnabled = true,
        )

    private val child1 =
        DevPerson(
            dateOfBirth = LocalDate.of(2017, 6, 1),
            ssn = "010617A123U",
            firstName = "Ricky",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val child2 =
        DevPerson(
            dateOfBirth = LocalDate.of(2016, 3, 1),
            ssn = "010316A1235",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.reset()
        MockEmailClient.clear()

        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(decisionMaker)
            tx.insert(decisionMaker2)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare)
            tx.insert(voucherDaycare2)
            listOf(adult1, adult2, adult3, adult4, adult5, adult7).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOptionVoucherValues()
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = child1.dateOfBirth,
                    endDate = child1.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
        }
    }

    private val now = HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(9, 0))
    private val clock = MockEvakaClock(now)
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

        val ex = assertThrows<BadRequest> { sendAllValueDecisions() }
        assertEquals("voucherValueDecisions.confirmation.tooFarInFuture", ex.errorCode)

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
    fun `sending value decision updates process id`() {
        createPlacement(startDate, endDate)
        sendAllValueDecisions()

        getAllValueDecisions().first().let { decision ->
            val process = db.read { tx -> tx.getCaseProcessByVoucherValueDecisionId(decision.id) }
            assertNotNull(process)
            assertEquals("1/123.789.b/${now.toLocalDate().year}", process.caseIdentifier)
            assertEquals(
                listOf(
                    CaseProcessState.INITIAL,
                    CaseProcessState.DECIDING,
                    CaseProcessState.COMPLETED,
                ),
                process.history.map { it.state },
            )
            assertEquals(
                listOf(
                    AuthenticatedUser.SystemInternalUser.evakaUserId,
                    financeWorker.evakaUserId,
                    AuthenticatedUser.SystemInternalUser.evakaUserId,
                ),
                process.history.map { it.enteredBy.id },
            )

            val metadata =
                processMetadataController.getVoucherValueDecisionMetadata(
                    dbInstance(),
                    admin.user,
                    clock,
                    decision.id,
                )
            assertEquals(process.caseIdentifier, metadata.data?.process?.caseIdentifier)
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
            assertEquals(adult1.id, decisions.first().headOfFamilyId)
        }

        changeHeadOfFamily(child1, adult5.id)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(2, decisions.size)
            val annulled = decisions.find { it.status == VoucherValueDecisionStatus.ANNULLED }
            assertNotNull(annulled)
            assertEquals(adult1.id, annulled.headOfFamilyId)
            val sent = decisions.find { it.status == VoucherValueDecisionStatus.SENT }
            assertNotNull(sent)
            assertEquals(adult5.id, sent.headOfFamilyId)
        }
    }

    @Test
    fun `value decision handler is set to approver for relief decision`() {
        val approvedDecision = createReliefDecision(false)
        assertEquals(decisionMaker.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `value decision handler is set to the daycare handler when forced when decision is not normal`() {
        val approvedDecision = createReliefDecision(true)
        assertEquals(decisionMaker2.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_SENDING is set to SENT`() {
        val optInAdult =
            DevPerson(
                ssn = "291090-9986",
                email = "optin@test.com",
                forceManualFeeDecisions = false,
                streetAddress = "Toistie 33",
                postalCode = "02230",
                postOffice = "Espoo",
            )
        db.transaction {
            it.insert(optInAdult, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = optInAdult.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = optInAdult.id, adult2 = adult7.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(clock)

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
            getEmailFor(optInAdult).fromAddress.address,
        )
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_MANUAL_SENDING is set to SENT`() {
        db.transaction {
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult3.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = adult3.id, adult2 = adult4.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())

        markValueDecisionsSent(listOf(decisionId))
        asyncJobRunner.runPendingJobsSync(clock)

        val emailContent =
            emailMessageProvider.financeDecisionNotification(
                FinanceDecisionType.VOUCHER_VALUE_DECISION
            )

        assertEquals(
            setOfNotNull(adult3.email),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(emailContent.subject, getEmailFor(adult3).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(adult3).fromAddress.address,
        )
    }

    @Test
    fun `Email notification is not sent to hof when opted out of decision emails`() {
        val optOutAdult =
            DevPerson(
                ssn = "291090-9986",
                email = "optout@test.com",
                forceManualFeeDecisions = false,
                disabledEmailTypes = setOf(EmailMessageType.DECISION_NOTIFICATION),
                streetAddress = "Toistie 33",
                postalCode = "02230",
                postOffice = "Espoo",
            )
        db.transaction {
            it.insert(optOutAdult, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = optOutAdult.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = optOutAdult.id, adult2 = adult7.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())
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
        createPlacement(newStartDate, endDate, voucherDaycare2.id)

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
        createPlacement(newStartDate, endDate, daycare.id)
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
            assertEquals(adult1.id, decisions.first().headOfFamilyId)
        }

        deletePlacement(placementId)
        sendAllValueDecisions()

        getAllValueDecisions().let { decisions ->
            assertEquals(1, decisions.size)
            assertEquals(VoucherValueDecisionStatus.ANNULLED, decisions.first().status)
            assertEquals(adult1.id, decisions.first().headOfFamilyId)
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

        assertEquals(1, searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT).total)
        assertEquals(
            1,
            searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT, searchTerms = "Ricky")
                .total,
        )
        assertEquals(
            1,
            searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT, searchTerms = "John")
                .total,
        )
        assertEquals(
            1,
            searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT, searchTerms = "Joan")
                .total,
        )
        assertEquals(
            0,
            searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT, searchTerms = "Foobar")
                .total,
        )
        assertEquals(0, searchValueDecisions(status = VoucherValueDecisionStatus.SENT).total)

        sendAllValueDecisions()
        assertEquals(0, searchValueDecisions(status = VoucherValueDecisionStatus.DRAFT).total)
        assertEquals(1, searchValueDecisions(status = VoucherValueDecisionStatus.SENT).total)
        assertEquals(
            1,
            searchValueDecisions(status = VoucherValueDecisionStatus.SENT, searchTerms = "Ricky")
                .total,
        )
        assertEquals(
            0,
            searchValueDecisions(status = VoucherValueDecisionStatus.SENT, searchTerms = "Foobar")
                .total,
        )
    }

    @Test
    fun `filter out starting placements`() {
        val placementId =
            createPlacement(now.toLocalDate().minusMonths(1), now.toLocalDate().plusMonths(1))

        sendAllValueDecisions()

        searchValueDecisions(
                VoucherValueDecisionStatus.SENT,
                distinctions = listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS),
            )
            .let { decisions -> assertEquals(1, decisions.data.size) }

        db.transaction {
            it.execute {
                sql(
                    "UPDATE placement SET start_date = ${bind(now.toLocalDate())} WHERE id = ${bind(placementId)}"
                )
            }
        }

        searchValueDecisions(
                VoucherValueDecisionStatus.SENT,
                distinctions = listOf(VoucherValueDecisionDistinctiveParams.NO_STARTING_PLACEMENTS),
            )
            .let { decisions -> assertEquals(0, decisions.data.size) }
    }

    @Test
    fun `PDF can be downloaded`() {
        createPlacement(startDate, endDate)
        val decisionId = sendAllValueDecisions().first()

        getVoucherValueDecisionPdf(decisionId, financeWorker)
    }

    @Test
    fun `Legacy PDF can be downloaded`() {
        createPlacement(startDate, endDate)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        getVoucherValueDecisionPdf(decisionId, financeWorker)
    }

    @Test
    fun `Legacy PDF can not be downloaded if head of family has restricted details but sfi message is sent`() {
        db.transaction {
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult7.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = adult7.id, adult2 = adult3.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertThrows<Forbidden> { getVoucherValueDecisionPdf(decisionId, financeWorker) }

        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(1, MockSfiMessagesClient.getMessages().size)

        val messageId = MockSfiMessagesClient.getMessages().first().messageId
        sfiAsyncJobs.getEvents(db, MockEvakaClock(HelsinkiDateTime.now()))

        db.read {
            val processedEvents = it.getSfiMessageEventsByMessageId(messageId)
            assertEquals(1, processedEvents.size)
            assertEquals(EventType.ELECTRONIC_MESSAGE_CREATED, processedEvents[0].eventType)
        }
    }

    @Test
    fun `Legacy PDF can not be downloaded if child has restricted details`() {
        val childRestricted =
            DevPerson(
                dateOfBirth = child1.dateOfBirth,
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
                streetAddress = "Kamreerintie 2",
                postalCode = "02770",
                postOffice = "Espoo",
            )

        db.transaction {
            it.insert(childRestricted, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = childRestricted.id,
                    headOfChildId = adult3.id,
                    startDate = childRestricted.dateOfBirth,
                    endDate = childRestricted.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }
        createPlacement(startDate, endDate, childId = childRestricted.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        assertThrows<Forbidden> { getVoucherValueDecisionPdf(decisionId, financeWorker) }
    }

    @Test
    fun `PDF without contact info can be downloaded even if head of family has restricted details`() {
        db.transaction {
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult7.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = adult7.id, adult2 = adult3.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()

        getVoucherValueDecisionPdf(decisionId, financeWorker)
    }

    @Test
    fun `PDF without contact info can be downloaded even if child has restricted details`() {
        val childRestricted =
            DevPerson(
                dateOfBirth = child1.dateOfBirth,
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
                streetAddress = "Kamreerintie 2",
                postalCode = "02770",
                postOffice = "Espoo",
            )

        db.transaction {
            it.insert(childRestricted, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = childRestricted.id,
                    headOfChildId = adult3.id,
                    startDate = childRestricted.dateOfBirth,
                    endDate = childRestricted.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
        }
        createPlacement(startDate, endDate, childId = childRestricted.id)
        val decisionId = sendAllValueDecisions().first()

        getVoucherValueDecisionPdf(decisionId, financeWorker)
    }

    @Test
    fun `Legacy PDF can be downloaded by admin even if someone in the family has restricted details`() {
        db.transaction {
            it.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult7.id,
                    startDate = child2.dateOfBirth,
                    endDate = child2.dateOfBirth.plusYears(18).minusDays(1),
                )
            )
            it.insertTestPartnership(adult1 = adult7.id, adult2 = adult3.id)
        }
        createPlacement(startDate, endDate, childId = child2.id)
        val decisionId = sendAllValueDecisions().first()
        db.transaction { it.setDocumentContainsContactInfo(decisionId) }

        getVoucherValueDecisionPdf(decisionId, adminUser)
    }

    @Test
    fun `VoucherValueDecision handler is set to the user when decision is not normal`() {
        val approvedDecision = createReliefDecision(false)
        assertEquals(decisionMaker.id.raw, approvedDecision.decisionHandler)
    }

    @Test
    fun `VoucherValueDecision handler is set to the daycare handler when forced when decision is not normal`() {
        val approvedDecision = createReliefDecision(true)
        assertEquals(decisionMaker2.id.raw, approvedDecision.decisionHandler)
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
                decisionMaker.id,
                HelsinkiDateTime.now(),
                null,
                forceDaycareHandler,
            )
        }

        return getAllValueDecisions().getOrNull(0)!!
    }

    private val serviceWorker =
        AuthenticatedUser.Employee(decisionMaker.id, setOf(UserRole.SERVICE_WORKER))
    private val financeWorker =
        AuthenticatedUser.Employee(decisionMaker.id, setOf(UserRole.FINANCE_ADMIN))
    private val adminUser = AuthenticatedUser.Employee(decisionMaker.id, setOf(UserRole.ADMIN))

    private fun createPlacement(
        startDate: LocalDate,
        endDate: LocalDate,
        unitId: DaycareId = voucherDaycare.id,
        childId: ChildId = child1.id,
        type: PlacementType = PlacementType.DAYCARE,
    ): PlacementId {
        placementController.createPlacement(
            dbInstance(),
            serviceWorker,
            clock,
            PlacementCreateRequestBody(
                type = type,
                childId = childId,
                unitId = unitId,
                startDate = startDate,
                endDate = endDate,
                placeGuarantee = false,
            ),
        )

        val placements =
            placementController.getChildPlacements(dbInstance(), serviceWorker, clock, childId)

        asyncJobRunner.runPendingJobsSync(clock)

        return placements.placements.first().id
    }

    private fun updatePlacement(id: PlacementId, startDate: LocalDate, endDate: LocalDate) {
        placementController.updatePlacementById(
            dbInstance(),
            serviceWorker,
            clock,
            id,
            PlacementUpdateRequestBody(startDate = startDate, endDate = endDate),
        )

        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun deletePlacement(id: PlacementId) {
        placementController.deletePlacement(dbInstance(), serviceWorker, clock, id)

        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun changeHeadOfFamily(child: DevPerson, headOfFamilyId: PersonId) {
        db.transaction {
            it.execute { sql("DELETE FROM fridge_child WHERE child_id = ${bind(child.id)}") }
        }

        parentshipController.createParentship(
            dbInstance(),
            serviceWorker,
            clock,
            ParentshipController.ParentshipRequest(
                childId = child.id,
                headOfChildId = headOfFamilyId,
                startDate = child.dateOfBirth,
                endDate = child.dateOfBirth.plusYears(18).minusDays(1),
            ),
        )

        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun searchValueDecisions(
        status: VoucherValueDecisionStatus,
        searchTerms: String = "",
        distinctions: List<VoucherValueDecisionDistinctiveParams> = emptyList(),
    ): PagedVoucherValueDecisionSummaries {
        return voucherValueDecisionController.searchVoucherValueDecisions(
            dbInstance(),
            financeWorker,
            clock,
            SearchVoucherValueDecisionRequest(
                page = 0,
                sortBy = null,
                sortDirection = null,
                statuses = listOf(status),
                area = null,
                unit = null,
                distinctions = distinctions,
                searchTerms = searchTerms,
                financeDecisionHandlerId = null,
                difference = null,
                startDate = null,
                endDate = null,
            ),
        )
    }

    private fun sendAllValueDecisions(): List<VoucherValueDecisionId> {
        val data =
            voucherValueDecisionController.searchVoucherValueDecisions(
                dbInstance(),
                financeWorker,
                clock,
                SearchVoucherValueDecisionRequest(
                    page = 0,
                    sortBy = null,
                    sortDirection = null,
                    statuses = listOf(VoucherValueDecisionStatus.DRAFT),
                    area = null,
                    unit = null,
                    distinctions = null,
                    searchTerms = null,
                    financeDecisionHandlerId = null,
                    difference = null,
                    startDate = null,
                    endDate = null,
                ),
            )

        val decisionIds = data.data.map { it.id }
        voucherValueDecisionController.sendVoucherValueDecisionDrafts(
            dbInstance(),
            financeWorker,
            clock,
            decisionIds,
            null,
        )

        asyncJobRunner.runPendingJobsSync(clock)
        return decisionIds
    }

    private fun getAllValueDecisions(): List<VoucherValueDecision> {
        return db.read {
                it.createQuery { sql("SELECT * FROM voucher_value_decision") }
                    .toList<VoucherValueDecision>()
            }
            .shuffled()
    }

    private fun getVoucherValueDecisionPdf(
        decisionId: VoucherValueDecisionId,
        user: AuthenticatedUser.Employee,
    ) {
        voucherValueDecisionController.getVoucherValueDecisionPdf(
            dbInstance(),
            user,
            clock,
            decisionId,
        )
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

    private fun markValueDecisionsSent(decisionIds: List<VoucherValueDecisionId>) {
        voucherValueDecisionController.markVoucherValueDecisionSent(
            dbInstance(),
            financeWorker,
            clock,
            decisionIds,
        )
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }
}
