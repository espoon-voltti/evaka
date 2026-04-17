// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.FullApplicationTest
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.emailclient.Email
import evaka.core.emailclient.MockEmailClient
import evaka.core.shared.ApplicationId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.job.ScheduledJobs
import java.time.LocalDate
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
    private val startDate = today
    private val endDate = startDate.plusYears(1)

    private lateinit var applicationId: ApplicationId

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(decisionMaker)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)

            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))

            applicationId =
                tx.insertTestApplication(
                    status = ApplicationStatus.WAITING_CONFIRMATION,
                    confidential = true,
                    childId = child.id,
                    guardianId = adult.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
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

    @Test
    fun `Non-guardian does not receive pending decision email`() {
        val nonGuardian = DevPerson(email = "non-guardian@example.com")
        val nonGuardianChild = DevPerson()

        createApplicationAndDecision(
            adult = nonGuardian,
            child = nonGuardianChild,
            createGuardianship = false,
        )

        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `Foster parent receives pending decision email`() {
        val fosterParent = DevPerson(email = "foster@example.com")
        val fosterChild = DevPerson()

        createApplicationAndDecision(
            adult = fosterParent,
            child = fosterChild,
            createGuardianship = false,
            fosterParentValidDuring = DateRange(today.minusYears(1), today.plusYears(1)),
        )

        runPendingDecisionEmailAsyncJobs()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            fosterParent.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Päätös varhaiskasvatuksesta / Beslut om förskoleundervisning / Decision on early childhood education",
            "kirjautumalla osoitteeseen <a",
            "kirjautumalla osoitteeseen https",
        )
    }

    @Test
    fun `Expired foster parent does not receive pending decision email`() {
        val expiredFosterParent = DevPerson(email = "expired-foster@example.com")
        val fosterChild = DevPerson()

        createApplicationAndDecision(
            adult = expiredFosterParent,
            child = fosterChild,
            createGuardianship = false,
            fosterParentValidDuring = DateRange(today.minusYears(2), today.minusYears(1)),
        )

        runPendingDecisionEmailAsyncJobs()
        assertEquals(0, MockEmailClient.emails.size)
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
                    createdBy = decisionMaker.evakaUserId,
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

    private fun createApplicationAndDecision(
        adult: DevPerson,
        child: DevPerson,
        createGuardianship: Boolean = true,
        fosterParentValidDuring: DateRange? = null,
    ) {
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)

            if (createGuardianship) {
                tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            }

            if (fosterParentValidDuring != null) {
                tx.insert(
                    DevFosterParent(
                        parentId = adult.id,
                        childId = child.id,
                        validDuring = fosterParentValidDuring,
                        modifiedAt = clock.now(),
                        modifiedBy = decisionMaker.evakaUserId,
                    )
                )
            }

            val appId =
                tx.insertTestApplication(
                    status = ApplicationStatus.WAITING_CONFIRMATION,
                    confidential = true,
                    childId = child.id,
                    guardianId = adult.id,
                    type = ApplicationType.DAYCARE,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
                )

            tx.insertTestDecision(
                TestDecision(
                    applicationId = appId,
                    status = DecisionStatus.PENDING,
                    createdBy = decisionMaker.evakaUserId,
                    unitId = daycare.id,
                    type = DecisionType.DAYCARE,
                    startDate = startDate,
                    endDate = endDate,
                    resolvedBy = decisionMaker.id.raw,
                    sentDate = today.minusDays(8),
                    resolved = null,
                    pendingDecisionEmailSent = null,
                    pendingDecisionEmailsSentCount = 0,
                )
            )
        }
    }
}
