// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
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
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.testAdult_2
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageReceiversIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val child = testChild_1
    private val unit = testDaycare
    private val secondUnit = testDaycare2

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

    private lateinit var groupMessageAccount: MessageAccountId
    private lateinit var supervisor1MessageAccount: MessageAccountId
    private lateinit var supervisor2MessageAccount: MessageAccountId

    private fun insertChildToUnit(
        tx: Database.Transaction,
        childId: ChildId,
        guardianId: PersonId?,
        unitId: DaycareId
    ): PlacementId {
        if (guardianId != null) tx.insertGuardian(guardianId, childId)
        return tx.insert(
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

            tx.insert(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            groupMessageAccount = tx.createDaycareGroupMessageAccount(groupId)

            tx.insert(
                DevDaycareGroup(
                    id = secondGroupId,
                    daycareId = secondUnit.id,
                    name = secondGroupName
                )
            )

            insertChildToGroup(tx, child.id, guardianPerson.id, groupId, unit.id)
            insertChildToGroup(tx, testChild_4.id, testAdult_4.id, secondGroupId, secondUnit.id)

            // Child 2 has a placement but is not in any group => should not show up in receivers
            // list
            insertChildToUnit(tx, testChild_2.id, testAdult_2.id, unit.id)

            // Child 3 has no guardians => should not show up in receivers list
            insertChildToGroup(tx, testChild_3.id, null, secondGroupId, secondUnit.id)
            tx.createParentship(testChild_3.id, testAdult_2.id, placementStart, placementEnd)

            tx.insert(DevEmployee(id = supervisorId))
            supervisor1MessageAccount = tx.upsertEmployeeMessageAccount(supervisorId)
            tx.insert(DevEmployee(id = supervisor2Id))
            supervisor2MessageAccount = tx.upsertEmployeeMessageAccount(supervisor2Id)
            tx.insertDaycareAclRow(unit.id, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(secondUnit.id, supervisor2Id, UserRole.UNIT_SUPERVISOR)
        }
    }

    @Test
    fun `message receiver endpoint works for unit 1`() {
        val (_, res, result) =
            http
                .get("/messages/receivers?unitId=${unit.id}")
                .asUser(supervisor1)
                .responseObject<List<MessageReceiversResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()
        assertEquals(
            setOf(
                MessageReceiversResponse(
                    accountId = supervisor1MessageAccount,
                    receivers =
                        listOf(
                            MessageReceiver.Unit(
                                id = unit.id,
                                name = unit.name,
                                receivers =
                                    listOf(
                                        MessageReceiver.Group(
                                            id = groupId,
                                            name = groupName,
                                            receivers =
                                                listOf(
                                                    MessageReceiver.Child(
                                                        id = child.id,
                                                        name =
                                                            "${child.lastName} ${child.firstName}"
                                                    )
                                                )
                                        )
                                    )
                            )
                        )
                ),
                MessageReceiversResponse(
                    accountId = groupMessageAccount,
                    receivers =
                        listOf(
                            MessageReceiver.Group(
                                id = groupId,
                                name = groupName,
                                receivers =
                                    listOf(
                                        MessageReceiver.Child(
                                            id = child.id,
                                            name = "${child.lastName} ${child.firstName}"
                                        )
                                    )
                            )
                        )
                )
            ),
            receivers.toSet()
        )
    }

    @Test
    fun `message receiver endpoint works for unit 2`() {
        val (_, res, result) =
            http
                .get("/messages/receivers?unitId=${secondUnit.id}")
                .asUser(supervisor2)
                .responseObject<List<MessageReceiversResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()
        assertEquals(
            setOf(
                MessageReceiversResponse(
                    accountId = supervisor2MessageAccount,
                    receivers =
                        listOf(
                            MessageReceiver.Unit(
                                id = secondUnit.id,
                                name = secondUnit.name,
                                receivers =
                                    listOf(
                                        MessageReceiver.Group(
                                            id = secondGroupId,
                                            name = secondGroupName,
                                            receivers =
                                                listOf(
                                                    MessageReceiver.Child(
                                                        id = testChild_4.id,
                                                        name =
                                                            "${testChild_4.lastName} ${testChild_4.firstName}"
                                                    )
                                                )
                                        )
                                    )
                            )
                        )
                )
            ),
            receivers.toSet()
        )
    }
}
