// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class PendingDecisionEmailServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var scheduledJobs: ScheduledJobs

    private val applicationId = UUID.randomUUID()
    private val childId = testChild_1.id
    private val guardianId = testAdult_6.id
    private val unitId = testDaycare.id
    private val startDate = LocalDate.now()
    private val endDate = startDate.plusYears(1)

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                childId = childId,
                guardianId = guardianId
            )
            tx.insertTestApplicationForm(
                applicationId = applicationId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication)
            )
        }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `Pending decision newer than one week does not get reminder email`() {
        createPendingDecision(LocalDate.now(), null, null, 0)
        Assertions.assertEquals(0, runPendingDecisionEmailAsyncJobs())
        Assertions.assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `Pending decision older than one week sends reminder email`() {
        createPendingDecision(LocalDate.now().minusDays(8), null, null, 0, type = DecisionType.PRESCHOOL)
        createPendingDecision(LocalDate.now().minusDays(8), null, null, 0, type = DecisionType.PRESCHOOL_DAYCARE)

        Assertions.assertEquals(1, runPendingDecisionEmailAsyncJobs())

        val sentMails = MockEmailClient.emails
        Assertions.assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            testAdult_6.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            "kirjautumalla osoitteeseen <a",
            "kirjautumalla osoitteeseen https"
        )
    }

    @Test
    fun `Pending decision older than one week with sent reminder less than week ago does not send a new one`() {
        createPendingDecision(LocalDate.now().minusDays(8), null, Instant.now(), 1)
        Assertions.assertEquals(0, runPendingDecisionEmailAsyncJobs())
        Assertions.assertEquals(0, MockEmailClient.emails.size)
    }

    val eightDaySeconds: Long = 60 * 60 * 24 * 8

    @Test
    fun `Pending decision older than one week with sent reminder older than one week sends a new email`() {
        createPendingDecision(LocalDate.now().minusDays(16), null, Instant.now().minusSeconds(eightDaySeconds), 1)
        Assertions.assertEquals(1, runPendingDecisionEmailAsyncJobs())

        val sentMails = MockEmailClient.emails
        Assertions.assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            testAdult_6.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            "kirjautumalla osoitteeseen <a",
            "kirjautumalla osoitteeseen https"
        )
    }

    @Test
    fun `Pending decision older than one week but already two reminders does not send third`() {
        createPendingDecision(LocalDate.now().minusDays(8), null, null, 2)
        Assertions.assertEquals(0, runPendingDecisionEmailAsyncJobs())
        val sentMails = MockEmailClient.emails
        Assertions.assertEquals(0, sentMails.size)
    }

    @Test
    fun `Bug verification - Pending decision with pending_decision_email_sent older than 1 week but already two reminders should not send reminder`() {
        createPendingDecision(LocalDate.now().minusDays(8), null, LocalDate.now().minusDays(8).atStartOfDay().toInstant(ZoneOffset.UTC), 2)
        Assertions.assertEquals(0, runPendingDecisionEmailAsyncJobs())
        val sentMails = MockEmailClient.emails
        Assertions.assertEquals(0, sentMails.size)
    }

    @Test
    fun `Pending decision older than two months does not send an email`() {
        createPendingDecision(LocalDate.now().minusMonths(2), null, null, 0)
        Assertions.assertEquals(0, runPendingDecisionEmailAsyncJobs())
        val sentMails = MockEmailClient.emails
        Assertions.assertEquals(0, sentMails.size)
    }

    private fun assertEmail(email: MockEmail?, expectedToAddress: String, expectedFromAddress: String, expectedSubject: String, expectedHtmlPart: String, expectedTextPart: String) {
        Assertions.assertNotNull(email)
        Assertions.assertEquals(expectedToAddress, email?.toAddress)
        Assertions.assertEquals(expectedFromAddress, email?.fromAddress)
        Assertions.assertEquals(expectedSubject, email?.subject)
        assert(email!!.htmlBody.contains(expectedHtmlPart, true))
        assert(email.textBody.contains(expectedTextPart, true))
    }

    private fun createPendingDecision(sentDate: LocalDate, resolved: Instant?, pendingDecisionEmailSent: Instant?, pendingDecisionEmailsSentCount: Int, type: DecisionType = DecisionType.DAYCARE) {
        db.transaction { tx ->
            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = testDecisionMaker_1.id,
                    unitId = unitId,
                    type = type,
                    startDate = startDate,
                    endDate = endDate,
                    resolvedBy = testDecisionMaker_1.id,
                    sentDate = sentDate,
                    resolved = resolved,
                    pendingDecisionEmailSent = pendingDecisionEmailSent,
                    pendingDecisionEmailsSentCount = pendingDecisionEmailsSentCount
                )
            )
        }
    }

    private fun runPendingDecisionEmailAsyncJobs(): Int {
        scheduledJobs.sendPendingDecisionReminderEmails(db)
        val jobCount = asyncJobRunner.getPendingJobCount()
        asyncJobRunner.runPendingJobsSync()

        return jobCount
    }
}
