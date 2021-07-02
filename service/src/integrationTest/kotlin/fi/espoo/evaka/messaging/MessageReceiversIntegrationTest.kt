// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.message.MessageController
import fi.espoo.evaka.messaging.message.MessageReceiversResponse
import fi.espoo.evaka.messaging.message.MessageType
import fi.espoo.evaka.messaging.message.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.createPersonMessageAccount
import fi.espoo.evaka.messaging.message.getCitizenMessageAccount
import fi.espoo.evaka.messaging.message.getDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MessageReceiversIntegrationTest : FullApplicationTest() {

    private val childId = testChild_1.id
    private val unitId = testDaycare.id
    private val secondUnitId = testDaycare2.id

    private val supervisorId = UUID.randomUUID()
    private val supervisor1 = AuthenticatedUser.Employee(supervisorId, setOf(UserRole.UNIT_SUPERVISOR))
    private val supervisor2Id = UUID.randomUUID()
    private val supervisor2 = AuthenticatedUser.Employee(supervisor2Id, setOf(UserRole.UNIT_SUPERVISOR))
    private val guardianPerson = testAdult_6
    private val groupId = GroupId(UUID.randomUUID())
    private val groupName = "Testaajat"
    private val secondGroupId = GroupId(UUID.randomUUID())
    private val secondGroupName = "Koekaniinit"
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    private fun insertChildToUnit(
        tx: Database.Transaction,
        childId: UUID,
        guardianId: UUID,
        unitId: UUID
    ): UUID {
        tx.insertGuardian(guardianId, childId)
        return tx.insertTestPlacement(
            DevPlacement(
                childId = childId,
                unitId = unitId,
                startDate = placementStart,
                endDate = placementEnd
            )
        )
    }

    private fun insertChildToGroup(
        tx: Database.Transaction,
        childId: UUID,
        guardianId: UUID,
        groupId: GroupId,
        unitId: UUID
    ) {
        val daycarePlacementId = insertChildToUnit(tx, childId, guardianId, unitId)
        tx.insertTestDaycareGroupPlacement(
            daycarePlacementId = daycarePlacementId,
            groupId = groupId,
            startDate = placementStart,
            endDate = placementEnd
        )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()

            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    name = groupName
                )
            )
            tx.createDaycareGroupMessageAccount(groupId)

            tx.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = secondGroupId,
                    daycareId = secondUnitId,
                    name = secondGroupName
                )
            )

            insertChildToGroup(tx, childId, guardianPerson.id, groupId, unitId)
            insertChildToGroup(tx, testChild_3.id, testAdult_3.id, secondGroupId, secondUnitId)
            insertChildToGroup(tx, testChild_4.id, testAdult_4.id, secondGroupId, secondUnitId)

            // Child 2 has a placement but is not in any group => should not
            // (currently) show up in receivers list
            insertChildToUnit(tx, testChild_2.id, testAdult_2.id, unitId)

            listOf(guardianPerson.id, testAdult_2.id, testAdult_3.id, testAdult_4.id)
                .forEach { tx.createPersonMessageAccount(it) }

            tx.createParentship(testChild_3.id, testAdult_2.id, placementStart, placementEnd)

            tx.insertTestEmployee(DevEmployee(id = supervisorId))
            tx.upsertEmployeeMessageAccount(supervisorId)
            tx.insertTestEmployee(DevEmployee(id = supervisor2Id))
            tx.upsertEmployeeMessageAccount(supervisor2Id)
            tx.insertDaycareAclRow(unitId, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(secondUnitId, supervisor2Id, UserRole.UNIT_SUPERVISOR)
        }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `message receiver endpoint works for unit 1`() {
        val (_, res, result) = http.get("/messages/receivers?unitId=$unitId")
            .asUser(supervisor1)
            .responseObject<List<MessageReceiversResponse>>(objectMapper)

        Assertions.assertEquals(200, res.statusCode)

        val receivers = result.get()

        Assertions.assertEquals(1, receivers.size)

        val groupTestaajat = receivers.find { it.groupName == groupName }!!
        Assertions.assertEquals(1, groupTestaajat.receivers.size)

        val receiverChild = groupTestaajat.receivers[0]
        Assertions.assertEquals(childId, receiverChild.childId)
    }

    @Test
    fun `message receiver endpoint works for unit 2`() {
        val (_, res, result) = http.get("/messages/receivers?unitId=$secondUnitId")
            .asUser(supervisor2)
            .responseObject<List<MessageReceiversResponse>>(objectMapper)

        Assertions.assertEquals(200, res.statusCode)

        val receivers = result.get()

        Assertions.assertEquals(1, receivers.size)

        val groupKoekaniinit = receivers.find { it.groupName == secondGroupName }!!
        Assertions.assertEquals(2, groupKoekaniinit.receivers.size)
        Assertions.assertEquals(
            setOf(testChild_3.id, testChild_4.id),
            groupKoekaniinit.receivers.map { it.childId }.toSet()
        )
        val childWithTwoReceiverPersons = groupKoekaniinit.receivers.find { it.childId == testChild_3.id }!!
        Assertions.assertEquals(2, childWithTwoReceiverPersons.receiverPersons.size)
    }

    @Test
    fun `allowed to send to own unit's parents`() {
        // supervisor of unit 1
        val sender = getEmployeeOwnMessageAccount(supervisor1)

        // parent of unit 1
        val recipient = db.read { it.getCitizenMessageAccount(guardianPerson.id) }

        val (_, response) = postNewThread(
            sender = sender,
            recipients = setOf(recipient),
            user = supervisor1
        )

        Assertions.assertEquals(200, response.statusCode)
    }

    @Test
    fun `not allowed to send to another unit's parents`() {
        // supervisor of unit 1
        val sender = getEmployeeOwnMessageAccount(supervisor1)

        // parent of unit 2
        val recipient = db.read { it.getCitizenMessageAccount(testAdult_3.id) }

        val (_, response) = postNewThread(
            sender = sender,
            recipients = setOf(recipient),
            user = supervisor1
        )

        Assertions.assertEquals(403, response.statusCode)
    }

    @Test
    fun `not allowed to send to employee`() {
        // supervisor of unit 1
        val sender = getEmployeeOwnMessageAccount(supervisor1)

        // supervisor of unit 2
        val recipient = getEmployeeOwnMessageAccount(supervisor2)

        val (_, response) = postNewThread(
            sender = sender,
            recipients = setOf(recipient),
            user = supervisor1
        )

        Assertions.assertEquals(403, response.statusCode)
    }

    @Test
    fun `not allowed to send to daycare group`() {
        // supervisor of unit 1
        val sender = getEmployeeOwnMessageAccount(supervisor1)

        // daycare group of unit 1
        val recipient = db.read { it.getDaycareGroupMessageAccount(groupId) }

        val (_, response) = postNewThread(
            sender = sender,
            recipients = setOf(recipient),
            user = supervisor1
        )

        Assertions.assertEquals(403, response.statusCode)
    }

    private fun postNewThread(
        sender: UUID,
        recipients: Set<UUID>,
        user: AuthenticatedUser.Employee,
    ) = http.post("/messages/$sender")
        .jsonBody(
            objectMapper.writeValueAsString(
                MessageController.PostMessageBody(
                    "Juhannus",
                    "Juhannus tulee pian",
                    MessageType.MESSAGE,
                    recipientAccountIds = recipients,
                    recipientNames = listOf()
                )
            )
        )
        .asUser(user)
        .response()

    private fun getEmployeeOwnMessageAccount(user: AuthenticatedUser): UUID {
        // language=SQL
        val sql = """
SELECT acc.id FROM message_account acc
WHERE acc.employee_id = :userId AND acc.active = true
        """.trimIndent()
        return db.read {
            it.createQuery(sql)
                .bind("userId", user.id)
                .mapTo<UUID>()
                .one()
        }
    }
}
