// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PendingDecisionEmailServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val applicationId = ApplicationId(UUID.randomUUID())
    private val childId = testChild_1.id
    private val guardianId = testAdult_6.id
    private val unitId = testDaycare.id
    private val startDate = LocalDate.now()
    private val endDate = startDate.plusYears(1)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_6, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                childId = childId,
                guardianId = guardianId,
                type = ApplicationType.DAYCARE,
                document = DaycareFormV0.fromApplication2(validDaycareApplication)
            )
        }
    }

    @Test
    fun `Pending decision newer than one week does not get reminder email`() {
        createPendingDecision(LocalDate.now(), null, null, 0)
        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `Pending decision older than one week sends reminder email`() {
        createPendingDecision(
            LocalDate.now().minusDays(8),
            null,
            null,
            0,
            type = DecisionType.PRESCHOOL
        )
        createPendingDecision(
            LocalDate.now().minusDays(8),
            null,
            null,
            0,
            type = DecisionType.PRESCHOOL_DAYCARE
        )

        runPendingDecisionEmailAsyncJobs()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
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
        createPendingDecision(LocalDate.now().minusDays(8), null, HelsinkiDateTime.now(), 1)
        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
    }

    val eightDaySeconds: Long = 60L * 60L * 24L * 8L

    @Test
    fun `Pending decision older than one week with sent reminder older than one week sends a new email`() {
        createPendingDecision(
            LocalDate.now().minusDays(16),
            null,
            HelsinkiDateTime.now().minusSeconds(eightDaySeconds),
            1
        )
        runPendingDecisionEmailAsyncJobs()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
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
        runPendingDecisionEmailAsyncJobs()
        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `Bug verification - Pending decision with pending_decision_email_sent older than 1 week but already two reminders should not send reminder`() {
        createPendingDecision(
            LocalDate.now().minusDays(8),
            null,
            HelsinkiDateTime.from(
                LocalDate.now().minusDays(8).atStartOfDay().toInstant(ZoneOffset.UTC)
            ),
            2
        )
        runPendingDecisionEmailAsyncJobs()
        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `Pending decision older than two months does not send an email`() {
        createPendingDecision(LocalDate.now().minusMonths(2), null, null, 0)
        runPendingDecisionEmailAsyncJobs()
        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    private fun assertEmail(
        email: Email?,
        expectedToAddress: String,
        expectedFromAddress: String,
        expectedSubject: String,
        expectedHtmlPart: String,
        expectedTextPart: String
    ) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email.toAddress)
        assertEquals(expectedFromAddress, email.fromAddress)
        assertEquals(expectedSubject, email.content.subject)
        assert(email.content.html.contains(expectedHtmlPart, true))
        assert(email.content.text.contains(expectedTextPart, true))
    }

    private fun createPendingDecision(
        sentDate: LocalDate,
        resolved: HelsinkiDateTime?,
        pendingDecisionEmailSent: HelsinkiDateTime?,
        pendingDecisionEmailsSentCount: Int,
        type: DecisionType = DecisionType.DAYCARE
    ) {
        db.transaction { tx ->
            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(testDecisionMaker_1.id.raw),
                    unitId = unitId,
                    type = type,
                    startDate = startDate,
                    endDate = endDate,
                    resolvedBy = testDecisionMaker_1.id.raw,
                    sentDate = sentDate,
                    resolved = resolved,
                    pendingDecisionEmailSent = pendingDecisionEmailSent,
                    pendingDecisionEmailsSentCount = pendingDecisionEmailsSentCount
                )
            )
        }
    }

    private fun runPendingDecisionEmailAsyncJobs(clock: EvakaClock = RealEvakaClock()): Int {
        scheduledJobs.sendPendingDecisionReminderEmails(db, clock)
        return asyncJobRunner.runPendingJobsSync(clock)
    }
}
