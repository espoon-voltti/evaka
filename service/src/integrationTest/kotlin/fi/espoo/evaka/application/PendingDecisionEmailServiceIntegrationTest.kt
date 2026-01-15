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
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.test.getValidDaycareApplication
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PendingDecisionEmailServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired lateinit var scheduledJobs: ScheduledJobs

    private val clock = MockEvakaClock(2023, 6, 1, 8, 0)
    private val today = clock.today()

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val decisionMaker = DevEmployee()
    private val adult = DevPerson(email = "test@example.com")
    private val child = DevPerson()
    private val applicationId = ApplicationId(UUID.randomUUID())
    private val startDate = today
    private val endDate = startDate.plusYears(1)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(decisionMaker)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)

            tx.insertTestApplication(
                id = applicationId,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                childId = child.id,
                guardianId = adult.id,
                type = ApplicationType.DAYCARE,
                document = DaycareFormV0.fromApplication2(getValidDaycareApplication(daycare)),
            )
        }
    }

    @Test
    fun `Pending decision newer than one week does not get reminder email`() {
        createPendingDecision(today, null, null, 0)
        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `Pending decision older than one week sends reminder email`() {
        createPendingDecision(today.minusDays(8), null, null, 0, type = DecisionType.PRESCHOOL)
        createPendingDecision(
            today.minusDays(8),
            null,
            null,
            0,
            type = DecisionType.PRESCHOOL_DAYCARE,
        )

        runPendingDecisionEmailAsyncJobs()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            adult.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            "kirjautumalla osoitteeseen <a",
            "kirjautumalla osoitteeseen https",
        )
    }

    @Test
    fun `Pending decision older than one week with sent reminder less than week ago does not send a new one`() {
        createPendingDecision(today.minusDays(8), null, clock.now(), 1)
        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `Pending decision older than one week with sent reminder older than one week sends a new email`() {
        createPendingDecision(today.minusDays(16), null, clock.now().minusDays(8), 1)
        runPendingDecisionEmailAsyncJobs()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            adult.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            "kirjautumalla osoitteeseen <a",
            "kirjautumalla osoitteeseen https",
        )
    }

    @Test
    fun `Pending decision older than one week but already two reminders does not send third`() {
        createPendingDecision(today.minusDays(8), null, null, 2)
        runPendingDecisionEmailAsyncJobs()
        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `Bug verification - Pending decision with pending_decision_email_sent older than 1 week but already two reminders should not send reminder`() {
        createPendingDecision(today.minusDays(8), null, clock.now().minusDays(8), 2)
        runPendingDecisionEmailAsyncJobs()
        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `Pending decision older than two months does not send an email`() {
        createPendingDecision(today.minusMonths(2), null, null, 0)
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
        expectedTextPart: String,
    ) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email.toAddress)
        assertEquals(expectedFromAddress, email.fromAddress.address)
        assertEquals(expectedSubject, email.content.subject)
        assert(email.content.html.contains(expectedHtmlPart, true))
        assert(email.content.text.contains(expectedTextPart, true))
    }

    private fun createPendingDecision(
        sentDate: LocalDate,
        resolved: HelsinkiDateTime?,
        pendingDecisionEmailSent: HelsinkiDateTime?,
        pendingDecisionEmailsSentCount: Int,
        type: DecisionType = DecisionType.DAYCARE,
    ) {
        db.transaction { tx ->
            tx.insertTestDecision(
                TestDecision(
                    applicationId = applicationId,
                    status = DecisionStatus.PENDING,
                    createdBy = EvakaUserId(decisionMaker.id.raw),
                    unitId = daycare.id,
                    type = type,
                    startDate = startDate,
                    endDate = endDate,
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = sentDate,
                    resolved = resolved,
                    pendingDecisionEmailSent = pendingDecisionEmailSent,
                    pendingDecisionEmailsSentCount = pendingDecisionEmailsSentCount,
                )
            )
        }
    }

    private fun runPendingDecisionEmailAsyncJobs(testClock: EvakaClock = clock): Int {
        scheduledJobs.sendPendingDecisionReminderEmails(db, testClock)
        return asyncJobRunner.runPendingJobsSync(testClock)
    }
}
