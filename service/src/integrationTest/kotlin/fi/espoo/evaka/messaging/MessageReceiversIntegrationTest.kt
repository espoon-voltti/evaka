// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
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
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageReceiversIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val childId = testChild_1.id
    private val unitId = testDaycare.id
    private val secondUnitId = testDaycare2.id

    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val supervisor1 =
        AuthenticatedUser.Employee(supervisorId, setOf(UserRole.UNIT_SUPERVISOR))
    private val supervisor2Id = EmployeeId(UUID.randomUUID())
    private val supervisor2 =
        AuthenticatedUser.Employee(supervisor2Id, setOf(UserRole.UNIT_SUPERVISOR))
    private val guardianPerson = testAdult_6
    private val groupId = GroupId(UUID.randomUUID())
    private val groupName = "Testaajat"
    private val secondGroupId = GroupId(UUID.randomUUID())
    private val secondGroupName = "Koekaniinit"
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    private fun insertChildToUnit(
        tx: Database.Transaction,
        childId: ChildId,
        guardianId: PersonId?,
        unitId: DaycareId
    ): PlacementId {
        if (guardianId != null) tx.insertGuardian(guardianId, childId)
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
        childId: ChildId,
        guardianId: PersonId?,
        groupId: GroupId,
        unitId: DaycareId
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
            tx.insertGeneralTestFixtures()

            tx.insertTestDaycareGroup(
                DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName)
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
            insertChildToGroup(tx, testChild_4.id, testAdult_4.id, secondGroupId, secondUnitId)

            // Child 2 has a placement but is not in any group => should not show up in receivers
            // list
            insertChildToUnit(tx, testChild_2.id, testAdult_2.id, unitId)

            listOf(guardianPerson.id, testAdult_2.id, testAdult_3.id, testAdult_4.id).forEach {
                tx.createPersonMessageAccount(it)
            }

            // Child 3 has no guardians => should not show up in receivers list
            insertChildToGroup(tx, testChild_3.id, null, secondGroupId, secondUnitId)
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
        val (_, res, result) =
            http
                .get("/messages/receivers?unitId=$unitId")
                .asUser(supervisor1)
                .responseObject<List<MessageReceiversResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()

        assertEquals(1, receivers.size)

        val groupTestaajat = receivers.find { it.groupName == groupName }!!
        assertEquals(1, groupTestaajat.receivers.size)

        val receiverChild = groupTestaajat.receivers[0]
        assertEquals(childId, receiverChild.childId)
    }

    @Test
    fun `message receiver endpoint works for unit 2`() {
        val (_, res, result) =
            http
                .get("/messages/receivers?unitId=$secondUnitId")
                .asUser(supervisor2)
                .responseObject<List<MessageReceiversResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()

        assertEquals(1, receivers.size)

        val groupKoekaniinit = receivers.find { it.groupName == secondGroupName }!!
        assertEquals(1, groupKoekaniinit.receivers.size)
        assertEquals(setOf(testChild_4.id), groupKoekaniinit.receivers.map { it.childId }.toSet())

        val childWithOnlyFridgeParentGuardian =
            groupKoekaniinit.receivers.find { it.childId == testChild_3.id }
        assertNull(childWithOnlyFridgeParentGuardian)
    }
}
