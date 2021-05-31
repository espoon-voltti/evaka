package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.deleteDaycareGroup
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.message.MessageNotificationEmailService
import fi.espoo.evaka.messaging.message.MessageService
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.messaging.message.deleteOrDeactivateDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.getAccountName
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.testDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class MessageAccountIntegrationTest : PureJdbiTest() {
    private val messageNotificationEmailService = Mockito.mock(MessageNotificationEmailService::class.java)
    private val messageService = MessageService(messageNotificationEmailService)

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `account is deleted if group has no messages`() {
        val groupId = UUID.randomUUID()
        val groupAccountId = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createDaycareGroupMessageAccount(groupId)
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccountId))

        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
        }

        assertEquals(MessageAccountState.NO_ACCOUNT, getMessageAccountState(groupAccountId))
    }

    @Test
    fun `account is kept if group has sent messages`() {
        // given
        val groupId = UUID.randomUUID()
        val groupAccount = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createDaycareGroupMessageAccount(groupId)
        }

        val receiverId = UUID.randomUUID()
        val receiverAccount = db.transaction { tx ->
            tx.insertTestPerson(DevPerson(id = receiverId))
            tx.createPersonMessageAccount(receiverId)
        }

        db.transaction { tx ->
            messageService.createMessageThreadsForRecipientGroups(
                tx,
                title = "Juhannus",
                content = "Juhannus tulee kohta",
                type = MessageType.MESSAGE,
                sender = groupAccount,
                recipientGroups = setOf(setOf(receiverAccount)),
                recipientNames = listOf(tx.getAccountName(receiverAccount))
            )
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccount))

        // when deleting the daycare message account
        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
        }

        // the account should actually stay
        assertEquals(MessageAccountState.INACTIVE, getMessageAccountState(groupAccount))
    }

    @Test
    fun `account is kept if group has received messages`() {
        // given
        val groupId = UUID.randomUUID()
        val groupAccount = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createDaycareGroupMessageAccount(groupId)
        }

        val senderId = UUID.randomUUID()
        val senderAccount = db.transaction { tx ->
            tx.insertTestPerson(DevPerson(id = senderId))
            tx.createPersonMessageAccount(senderId)
        }

        db.transaction { tx ->
            messageService.createMessageThreadsForRecipientGroups(
                tx,
                title = "Juhannus",
                content = "Juhannus tulee kohta",
                type = MessageType.MESSAGE,
                sender = senderAccount,
                recipientGroups = setOf(setOf(groupAccount)),
                recipientNames = listOf(tx.getAccountName(groupAccount))
            )
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccount))
        assertEquals("Test Daycare - Testiläiset", db.read { it.getAccountName(groupAccount) })

        // when deleting the daycare message account
        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
            it.deleteDaycareGroup(groupId)
        }

        // the account should actually stay
        assertEquals(MessageAccountState.INACTIVE, getMessageAccountState(groupAccount))

        // account name should be available even though the group has been deleted
        assertEquals("Test Daycare - Testiläiset", db.read { it.getAccountName(groupAccount) })
    }

    private enum class MessageAccountState {
        ACTIVE,
        INACTIVE,
        NO_ACCOUNT
    }

    private fun getMessageAccountState(accountId: UUID): MessageAccountState {
        // language=SQL
        val sql = """
            SELECT active FROM message_account WHERE id = :accountId
        """.trimIndent()
        return db.read {
            it.createQuery(sql)
                .bind("accountId", accountId)
                .mapTo<Boolean>().toList()
        }.let { accounts ->
            if (accounts.size == 1) {
                if (accounts[0]) {
                    MessageAccountState.ACTIVE
                } else {
                    MessageAccountState.INACTIVE
                }
            } else if (accounts.isEmpty()) MessageAccountState.NO_ACCOUNT
            else throw RuntimeException("Employee has more than one account")
        }
    }
}
