// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.FullApplicationTest
import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.pis.NotificationCategory
import evaka.core.pis.updateDisabledPushTypes
import evaka.core.shared.CalendarEventTimeId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.MockEvakaClock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenPushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var pushNotifications: CitizenPushNotifications
    @Autowired private lateinit var mockEndpoint: MockWebPushEndpoint

    private val clock = MockEvakaClock(2026, 1, 1, 12, 0)
    private val citizen = DevPerson()
    private val child = DevPerson()

    private val decision =
        CitizenPushNotification.Decision(DecisionPushNotificationKind.APPLICATION)

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
        db.transaction { tx ->
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `a planned notification is sent to every device of the citizen`() {
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort, "1"))
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort, "2"))
        }

        val planned = db.transaction {
            pushNotifications.plan(it, clock.now(), citizen.id, decision)
        }
        assertEquals(2, planned)
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(1, mockEndpoint.getCapturedRequests("1").size)
        assertEquals(1, mockEndpoint.getCapturedRequests("2").size)
        db.read { it.getCitizenPushDevices(citizen.id) }.forEach { assertNotNull(it.lastSentAt) }
    }

    @Test
    fun `nothing is planned for a citizen without devices`() {
        val planned = db.transaction {
            pushNotifications.plan(it, clock.now(), citizen.id, decision)
        }
        assertEquals(0, planned)
        assertEquals(emptyList(), db.read { it.getPlannedCitizenPushNotifications() })
    }

    @Test
    fun `a notification of a category the citizen has switched off is not sent`() {
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
            tx.updateDisabledPushTypes(
                citizen.id,
                setOf(NotificationCategory.DECISION_NOTIFICATION),
            )
            pushNotifications.plan(tx, clock.now(), citizen.id, decision)
        }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(emptyList(), mockEndpoint.getCapturedRequests("1234"))
        assertNull(db.read { it.getCitizenPushDevices(citizen.id) }.single().lastSentAt)
    }

    @Test
    fun `a time-limited notification expires at its deadline`() {
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
            pushNotifications.plan(
                tx,
                clock.now(),
                citizen.id,
                CitizenPushNotification.MissingReservations(
                    range = FiniteDateRange(clock.today().plusDays(7), clock.today().plusDays(13)),
                    deadline = clock.now().plusHours(2),
                ),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        val request = mockEndpoint.getCapturedRequests("1234").single()
        assertEquals("7200", request.headers["ttl"])
    }

    @Test
    fun `a notification is not sent after its deadline`() {
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
            pushNotifications.plan(
                tx,
                clock.now(),
                citizen.id,
                CitizenPushNotification.MissingReservations(
                    range = FiniteDateRange(clock.today().plusDays(7), clock.today().plusDays(13)),
                    deadline = clock.now().plusHours(2),
                ),
            )
        }
        // the job is late, for example because of a long job queue
        clock.tick(Duration.ofHours(2))
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(emptyList(), mockEndpoint.getCapturedRequests("1234"))
    }

    @Test
    fun `a notification with a long body is shortened to fit in the push payload`() {
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
            pushNotifications.plan(
                tx,
                clock.now(),
                citizen.id,
                CitizenPushNotification.DiscussionSurvey("Pitkä otsikko ".repeat(1000)),
            )
        }
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(1, mockEndpoint.getCapturedRequests("1234").size)
    }

    @Test
    fun `a throttled notification is sent again when the push service told us to`() {
        subscribeAndPlanDecision()
        mockEndpoint.respondOnceWith("1234", status = 429, retryAfter = "600")

        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(1, mockEndpoint.getCapturedRequests("1234").size)
        assertNull(db.read { it.getCitizenPushDevices(citizen.id) }.single().lastSentAt)

        // the ordinary retry interval is 5 minutes, and it must not override Retry-After
        clock.tick(Duration.ofMinutes(5))
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(1, mockEndpoint.getCapturedRequests("1234").size)

        clock.tick(Duration.ofMinutes(5))
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(2, mockEndpoint.getCapturedRequests("1234").size)
        assertNotNull(db.read { it.getCitizenPushDevices(citizen.id) }.single().lastSentAt)
    }

    @Test
    fun `a wait longer than an hour is shortened to an hour`() {
        subscribeAndPlanDecision()
        mockEndpoint.respondOnceWith("1234", status = 503, retryAfter = "7200")

        asyncJobRunner.runPendingJobsSync(clock)

        clock.tick(Duration.ofHours(1))
        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(2, mockEndpoint.getCapturedRequests("1234").size)
    }

    @Test
    fun `every notification kind survives the job queue`() {
        val notifications =
            listOf(
                decision,
                CitizenPushNotification.Income(IncomeNotificationType.REMINDER_EMAIL),
                CitizenPushNotification.ChildApplicationDecision(
                    ChildApplicationDecisionKind.SERVICE_APPLICATION,
                    child.id,
                ),
                CitizenPushNotification.CalendarEvents(
                    count = 1,
                    single = SingleCalendarEvent("Retki", clock.today().plusDays(3)),
                ),
                CitizenPushNotification.CalendarEvents(count = 3, single = null),
                CitizenPushNotification.Document(
                    ChildDocumentId(UUID.randomUUID()),
                    ChildDocumentNotificationType.EDITABLE_DOCUMENT,
                ),
                CitizenPushNotification.InformalDocument(child.id),
                CitizenPushNotification.MissingReservations(
                    range = FiniteDateRange(clock.today().plusDays(7), clock.today().plusDays(13)),
                    deadline = clock.now().plusDays(1),
                ),
                CitizenPushNotification.MissingHolidayReservations(clock.today().plusDays(2)),
                CitizenPushNotification.DiscussionSurvey("Vasukeskustelut"),
                CitizenPushNotification.DiscussionTime(
                    CalendarEventTimeId(UUID.randomUUID()),
                    DiscussionTimePushNotificationEvent.REMINDER,
                    LocalDate.of(2026, 1, 8),
                    LocalTime.of(10, 0),
                    LocalTime.of(10, 30),
                ),
            )
        db.transaction { tx ->
            tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
            notifications.forEach { pushNotifications.plan(tx, clock.now(), citizen.id, it) }
        }

        assertEquals(
            notifications.toSet(),
            db.read { it.getPlannedCitizenPushNotifications() }.toSet(),
        )

        asyncJobRunner.runPendingJobsSync(clock)
        assertEquals(notifications.size, mockEndpoint.getCapturedRequests("1234").size)
    }

    private fun subscribeAndPlanDecision() = db.transaction { tx ->
        tx.insertTestCitizenPushSubscription(citizen.id, mockWebPushEndpoint(httpPort))
        pushNotifications.plan(tx, clock.now(), citizen.id, decision)
    }
}
