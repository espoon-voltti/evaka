// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.messaging.MessageController
import fi.espoo.evaka.messaging.MessageRecipient
import fi.espoo.evaka.messaging.MessageService
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.syncDaycareGroupAcl
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessageControllerMobileIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: MessageControllerMobile
    @Autowired private lateinit var messageController: MessageController
    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private lateinit var clock: MockEvakaClock

    private val placementStart = LocalDate.of(2022, 5, 14)
    private val placementEnd = placementStart.plusMonths(1)
    private val sendTime = MockEvakaClock(2022, 5, 14, 12, 0)

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val groupId = GroupId(UUID.randomUUID())
    private val devEmployee = DevEmployee()
    private val employee =
        AuthenticatedUser.Employee(id = devEmployee.id, roles = setOf(UserRole.UNIT_SUPERVISOR))
    private val citizen = DevPerson(firstName = "John", lastName = "Doe", ssn = "010180-1232")
    private val child = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))

    private lateinit var groupAccount: MessageAccountId
    private lateinit var citizenAccount: MessageAccountId

    @BeforeEach
    fun setUp() {
        clock = MockEvakaClock(2022, 5, 14, 12, 30)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(DevDaycareGroup(id = groupId, daycareId = daycare.id, startDate = placementStart))
            groupAccount = tx.createDaycareGroupMessageAccount(groupId)

            tx.insert(citizen, DevPersonType.ADULT)
            citizenAccount = tx.getCitizenMessageAccount(citizen.id)

            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(citizen.id, child.id)

            val placementId = tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insertServiceNeedOptions()
            tx.insert(devEmployee)
            tx.insert(
                DevServiceNeed(
                    confirmedBy = employee.evakaUserId,
                    placementId = placementId,
                    startDate = placementStart,
                    endDate = placementEnd,
                    optionId = snDefaultDaycare.id,
                )
            )
            tx.insertDaycareAclRow(daycare.id, devEmployee.id, UserRole.UNIT_SUPERVISOR)
            tx.syncDaycareGroupAcl(daycare.id, devEmployee.id, listOf(groupId), sendTime.now())
        }

        // Send a message from group → citizen
        messageController.createMessage(
            dbInstance(),
            employee,
            sendTime,
            groupAccount,
            null,
            MessageController.PostMessageBody(
                title = "Hello",
                content = "Hello from group",
                type = MessageType.MESSAGE,
                recipients = setOf(MessageRecipient.Child(child.id)),
                recipientNames = listOf(),
                attachmentIds = setOf(),
                draftId = null,
                urgent = false,
                sensitive = false,
                relatedApplicationId = null,
                filters = null,
            ),
        )
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(sendTime.now().plusSeconds(30)))
    }

    private fun user() = AuthenticatedUser.CitizenMobile(citizen.id, CitizenAuthLevel.WEAK)

    @Test
    fun `thread list returns the inbox`() {
        val response = controller.getThreads(dbInstance(), user(), clock, pageSize = 20, page = 1)
        assertEquals(1, response.data.size)
        assertTrue(response.data[0].title.isNotEmpty())
        assertTrue(response.data[0].senderName.isNotEmpty())
        assertTrue(response.data[0].unreadCount >= 1)
    }

    @Test
    fun `unread count returns at least 1 after a new message arrives`() {
        assertTrue(controller.getUnreadCount(dbInstance(), user(), clock) >= 1)
    }

    @Test
    fun `reply adds a message to the thread`() {
        val threadId = controller.getThreads(dbInstance(), user(), clock, 20, 1).data.first().id
        val beforeReply = controller.getThread(dbInstance(), user(), clock, threadId)
        val otherParticipants =
            beforeReply.messages.map { it.senderAccountId }.toSet() -
                setOf(controller.getMyAccount(dbInstance(), user(), clock).accountId)
        controller.replyToThread(
            dbInstance(),
            user(),
            clock,
            threadId,
            MessageControllerMobile.ReplyBody(
                content = "Thank you!",
                recipientAccountIds = otherParticipants,
            ),
        )
        val after = controller.getThread(dbInstance(), user(), clock, threadId)
        assertEquals(beforeReply.messages.size + 1, after.messages.size)
        assertEquals("Thank you!", after.messages.last().content)
    }

    @Test
    fun `mark-read zeroes the unread count`() {
        val threadId = controller.getThreads(dbInstance(), user(), clock, 20, 1).data.first().id
        controller.markThreadRead(dbInstance(), user(), clock, threadId)
        assertEquals(0, controller.getUnreadCount(dbInstance(), user(), clock))
    }
}
