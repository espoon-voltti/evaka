// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.messaging.MessageService
import fi.espoo.evaka.messaging.NewMessageStub
import fi.espoo.evaka.messaging.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.CitizenPushSubscriptionDeviceId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CitizenMessagePushNotificationsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var mockExpo: MockExpoPushEndpoint

    private lateinit var clock: MockEvakaClock

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val employee = DevEmployee()
    private val citizen = DevPerson()
    private val child = DevPerson()

    private val deviceId = CitizenPushSubscriptionDeviceId(UUID.randomUUID())
    private val expoPushToken = "ExponentPushToken[TEST1]"

    private lateinit var groupAccount: MessageAccountId
    private lateinit var citizenAccount: MessageAccountId
    private lateinit var threadId: MessageThreadId

    @BeforeEach
    fun setUp() {
        clock = MockEvakaClock(2026, 4, 14, 12, 0)
        mockExpo.reset()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            groupAccount = tx.createDaycareGroupMessageAccount(group.id)

            tx.insert(employee)

            tx.insert(citizen, DevPersonType.ADULT)
            citizenAccount = tx.getCitizenMessageAccount(citizen.id)

            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(citizen.id, child.id)

            val placement =
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusYears(1),
                )
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group.id,
                    startDate = placement.startDate,
                    endDate = placement.endDate,
                )
            )

            tx.upsertCitizenPushSubscription(citizen.id, deviceId, expoPushToken)
        }

        // Have citizen send a message to the group to create a thread
        val sent =
            db.transaction { tx ->
                messageService.sendMessageAsCitizen(
                    tx,
                    clock.now(),
                    sender = citizenAccount,
                    recipients = setOf(groupAccount),
                    children = emptySet(),
                    msg =
                        NewMessageStub(
                            title = "Initial",
                            content = "Hi",
                            urgent = false,
                            sensitive = false,
                        ),
                )
            }
        threadId = sent.threadId

        // Process initial message jobs (no citizen push since citizen is the sender)
        asyncJobRunner.runPendingJobsSync(clock)
        asyncJobRunner.runPendingJobsSync(clock)

        mockExpo.reset()
    }

    @Test
    fun `push notification is sent when a group replies to a citizen`() {
        val groupEmployee = AuthenticatedUser.Employee(employee.id, setOf(UserRole.UNIT_SUPERVISOR))

        messageService.replyToThread(
            db,
            clock.now(),
            threadId = threadId,
            senderAccount = groupAccount,
            recipientAccountIds = setOf(citizenAccount),
            content = "Hello citizen",
            municipalAccountName = "Municipal",
            serviceWorkerAccountName = "Service worker",
            financeAccountName = "Finance",
            user = groupEmployee,
        )

        // First pass: MarkMessagesAsSent -> plans NotifyCitizenOfNewMessage
        asyncJobRunner.runPendingJobsSync(clock)
        // Second pass: NotifyCitizenOfNewMessage -> plans SendCitizenMessagePushNotification
        asyncJobRunner.runPendingJobsSync(clock)
        // Third pass: SendCitizenMessagePushNotification -> calls expoPushClient.send
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(1, mockExpo.sent.size)
        val sent = mockExpo.sent.single()
        assertEquals(expoPushToken, sent.to)
        assertEquals("Uusi viesti", sent.title)
        assertEquals("Sinulle on uusi viesti eVakassa", sent.body)
        assertFalse(sent.data.containsKey("senderName"))
        assertFalse(sent.data.containsKey("messageContent"))
        assertTrue(sent.data.containsKey("messageId"))
    }

    @Test
    fun `subscription is deleted when Expo reports DeviceNotRegistered`() {
        val groupEmployee = AuthenticatedUser.Employee(employee.id, setOf(UserRole.UNIT_SUPERVISOR))

        mockExpo.nextResponseIsDeviceNotRegistered = true

        messageService.replyToThread(
            db,
            clock.now(),
            threadId = threadId,
            senderAccount = groupAccount,
            recipientAccountIds = setOf(citizenAccount),
            content = "Hello citizen",
            municipalAccountName = "Municipal",
            serviceWorkerAccountName = "Service worker",
            financeAccountName = "Finance",
            user = groupEmployee,
        )

        asyncJobRunner.runPendingJobsSync(clock)
        asyncJobRunner.runPendingJobsSync(clock)
        asyncJobRunner.runPendingJobsSync(clock)

        val remaining = db.read { it.getCitizenPushSubscriptions(citizen.id) }
        assertTrue(remaining.isEmpty(), "Subscription should be deleted after DeviceNotRegistered")
    }
}
