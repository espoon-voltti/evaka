package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createMessageAccountForDaycareGroup
import fi.espoo.evaka.messaging.message.createMessageAccountForPerson
import fi.espoo.evaka.messaging.message.createMessageThreadsForRecipientGroups
import fi.espoo.evaka.messaging.message.deleteOrDeactivateDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.getMessageAccountForDaycareGroup
import fi.espoo.evaka.messaging.message.getMessageAccountForEndUser
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.testDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MessageAccountIntegrationTest : PureJdbiTest() {
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
        val groupAccount = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createMessageAccountForDaycareGroup(groupId)
            it.getMessageAccountForDaycareGroup(groupId)!!
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccount.id))

        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
        }

        assertEquals(MessageAccountState.NO_ACCOUNT, getMessageAccountState(groupAccount.id))
    }

    @Test
    fun `account is kept if group has sent messages`() {
        // given
        val groupId = UUID.randomUUID()
        val groupAccount = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createMessageAccountForDaycareGroup(groupId)
            it.getMessageAccountForDaycareGroup(groupId)!!
        }

        val receiverId = UUID.randomUUID()
        val receiverAccount = db.transaction { tx ->
            tx.insertTestPerson(DevPerson(id = receiverId))
            tx.createMessageAccountForPerson(receiverId)
            tx.getMessageAccountForEndUser(AuthenticatedUser.Citizen(id = receiverId))
        }

        db.transaction { tx ->
            createMessageThreadsForRecipientGroups(
                tx,
                title = "Juhannus",
                content = "Juhannus tulee kohta",
                type = MessageType.MESSAGE,
                sender = groupAccount,
                recipientGroups = setOf(setOf(receiverAccount.id))
            )
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccount.id))

        // when deleting the daycare message account
        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
        }

        // the account should actually stay
        assertEquals(MessageAccountState.INACTIVE, getMessageAccountState(groupAccount.id))
    }

    @Test
    fun `account is kept if group has received messages`() {
        // given
        val groupId = UUID.randomUUID()
        val groupAccount = db.transaction {
            it.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            it.createMessageAccountForDaycareGroup(groupId)
            it.getMessageAccountForDaycareGroup(groupId)!!
        }

        val senderId = UUID.randomUUID()
        val senderAccount = db.transaction { tx ->
            tx.insertTestPerson(DevPerson(id = senderId))
            tx.createMessageAccountForPerson(senderId)
            tx.getMessageAccountForEndUser(AuthenticatedUser.Citizen(id = senderId))
        }

        db.transaction { tx ->
            createMessageThreadsForRecipientGroups(
                tx,
                title = "Juhannus",
                content = "Juhannus tulee kohta",
                type = MessageType.MESSAGE,
                sender = senderAccount,
                recipientGroups = setOf(setOf(groupAccount.id))
            )
        }
        assertEquals(MessageAccountState.ACTIVE, getMessageAccountState(groupAccount.id))

        // when deleting the daycare message account
        db.transaction {
            deleteOrDeactivateDaycareGroupMessageAccount(it, groupId)
        }

        // the account should actually stay
        assertEquals(MessageAccountState.INACTIVE, getMessageAccountState(groupAccount.id))
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
