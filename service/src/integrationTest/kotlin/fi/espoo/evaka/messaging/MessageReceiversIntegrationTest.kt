// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.message.MessageReceiversResponse
import fi.espoo.evaka.messaging.message.createMessageAccountForPerson
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
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
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
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
    private val supervisor = AuthenticatedUser.Employee(supervisorId, emptySet())
    private val staffId = UUID.randomUUID()
    private val guardianPerson = testAdult_6
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val secondGroupId = UUID.randomUUID()
    private val secondGroupName = "Koekaniinit"
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    private fun insertChildToGroup(
        tx: Database.Transaction,
        childId: UUID,
        guardianId: UUID,
        groupId: UUID,
        unitId: UUID
    ) {
        val daycarePlacementId = tx.insertTestPlacement(
            DevPlacement(
                childId = childId,
                unitId = unitId,
                startDate = placementStart,
                endDate = placementEnd
            )
        )
        tx.insertTestDaycareGroupPlacement(
            daycarePlacementId = daycarePlacementId,
            groupId = groupId,
            startDate = placementStart,
            endDate = placementEnd
        )
        tx.insertGuardian(guardianId, childId)
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()

            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
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

            listOf(guardianPerson.id, testAdult_2.id, testAdult_3.id, testAdult_4.id)
                .forEach { tx.createMessageAccountForPerson(it) }

            tx.createParentship(testChild_3.id, testAdult_2.id, placementStart, placementEnd)

            tx.insertTestEmployee(
                DevEmployee(
                    id = supervisorId,
                    firstName = "Elina",
                    lastName = "Esimies"
                )
            )
            tx.insertTestEmployee(
                DevEmployee(
                    id = staffId
                )
            )
            tx.insertDaycareAclRow(unitId, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(unitId, staffId, UserRole.STAFF)
            tx.insertDaycareAclRow(secondUnitId, supervisorId, UserRole.UNIT_SUPERVISOR)
        }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `message receiver endpoint works for unit 1`() {
        val (_, res, result) = http.get("/messages/receivers?unitId=$unitId")
            .asUser(supervisor)
            .responseObject<List<MessageReceiversResponse>>(objectMapper)

        Assertions.assertEquals(200, res.statusCode)

        val receivers = result.get()

        Assertions.assertEquals(1, receivers.size)

        val groupTestaajat = receivers.find { it.groupName == groupName }!!
        Assertions.assertEquals(1, groupTestaajat.receivers.size)
    }

    @Test
    fun `message receiver endpoint works for unit 2`() {
        val (_, res, result) = http.get("/messages/receivers?unitId=$secondUnitId")
            .asUser(supervisor)
            .responseObject<List<MessageReceiversResponse>>(objectMapper)

        Assertions.assertEquals(200, res.statusCode)

        val receivers = result.get()

        Assertions.assertEquals(1, receivers.size)

        val groupKoekaniinit = receivers.find { it.groupName == secondGroupName }!!
        Assertions.assertEquals(2, groupKoekaniinit.receivers.size)
        val childWithTwoReceiverPersons = groupKoekaniinit.receivers.find { it.childId == testChild_3.id }!!
        Assertions.assertEquals(2, childWithTwoReceiverPersons.receiverPersons.size)
    }
}
