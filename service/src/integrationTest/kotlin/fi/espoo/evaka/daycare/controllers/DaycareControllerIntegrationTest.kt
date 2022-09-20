// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestBackupCare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DaycareControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val childId = testChild_1.id
    private val daycareId = testDaycare.id
    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val supervisor = AuthenticatedUser.Employee(supervisorId, emptySet())
    private val staffId = EmployeeId(UUID.randomUUID())
    private val staffMember = AuthenticatedUser.Employee(staffId, emptySet())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()

            tx.insertTestEmployee(
                DevEmployee(
                    id = supervisorId,
                    firstName = "Elina",
                    lastName = "Esimies",
                )
            )
            tx.insertTestEmployee(
                DevEmployee(
                    id = staffId,
                )
            )
            tx.insertDaycareAclRow(daycareId, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(daycareId, staffId, UserRole.STAFF)
        }
    }

    @Test
    fun `get daycare`() {
        val (daycare, _, _) = getDaycare(daycareId)

        db.read { assertEquals(it.getDaycare(daycareId), daycare) }
    }

    @Test
    fun `create group`() {
        val name = "Ryh mä"
        val startDate = LocalDate.of(2019, 1, 1)
        val caretakers = 42.0
        val group = createDaycareGroup(daycareId, name, startDate, caretakers)

        assertEquals(daycareId, group.daycareId)
        assertEquals(name, group.name)
        assertEquals(startDate, group.startDate)
        assertNull(group.endDate)
        assertTrue(group.deletable)

        db.read { assertEquals(it.getDaycareGroup(group.id), group) }
        assertTrue(groupHasMessageAccount(group.id))
    }

    @Test
    fun `delete group`() {
        val group = createDaycareGroup(daycareId, "Test", LocalDate.of(2019, 1, 1), 5.0)

        deleteDaycareGroup(daycareId, group.id)

        db.read { assertNull(it.getDaycareGroup(group.id)) }
        assertFalse(groupHasMessageAccount(group.id))
    }

    @Test
    fun `cannot delete a group when it has a placement`() {
        val group = DevDaycareGroup(daycareId = daycareId)
        val placement = DevPlacement(unitId = daycareId, childId = childId)
        db.transaction {
            it.insertTestPlacement(placement)
            it.insertTestDaycareGroup(group)
            it.createDaycareGroupMessageAccount(group.id)
            it.insertTestDaycareGroupPlacement(placement.id, group.id)
        }

        deleteDaycareGroup(daycareId, group.id, expectedStatus = 409)

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it is a backup care`() {
        val group = DevDaycareGroup(daycareId = daycareId)
        db.transaction {
            it.insertTestDaycareGroup(group)
            it.createDaycareGroupMessageAccount(group.id)
            it.insertTestPlacement(childId = childId, unitId = daycareId)
            it.insertTestBackupCare(
                DevBackupCare(
                    childId = childId,
                    unitId = daycareId,
                    groupId = group.id,
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
                )
            )
        }

        deleteDaycareGroup(daycareId, group.id, expectedStatus = 409)

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it has messages`() {
        val group = DevDaycareGroup(daycareId = daycareId)
        db.transaction {
            it.insertTestDaycareGroup(group)
            val accountId = it.createDaycareGroupMessageAccount(group.id)
            it.insertMessageContent("Juhannus tulee pian", accountId)
        }

        deleteDaycareGroup(daycareId, group.id, expectedStatus = 409)

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    private fun getDaycare(daycareId: DaycareId): DaycareController.DaycareResponse {
        val (_, res, body) =
            http
                .get("/daycares/$daycareId")
                .asUser(staffMember)
                .responseObject<DaycareController.DaycareResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return body.get()
    }

    private fun createDaycareGroup(
        daycareId: DaycareId,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double
    ): DaycareGroup {
        val (_, res, body) =
            http
                .post("/daycares/$daycareId/groups")
                .jsonBody(
                    jsonMapper.writeValueAsString(
                        DaycareController.CreateGroupRequest(
                            name = name,
                            startDate = startDate,
                            initialCaretakers = initialCaretakers
                        )
                    )
                )
                .asUser(supervisor)
                .responseObject<DaycareGroup>(jsonMapper)

        assertEquals(200, res.statusCode)
        return body.get()
    }

    private fun deleteDaycareGroup(
        daycareId: DaycareId,
        groupId: GroupId,
        expectedStatus: Int = 200
    ) {
        val (_, res) =
            http.delete("/daycares/$daycareId/groups/$groupId").asUser(supervisor).response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun groupHasMessageAccount(groupId: GroupId): Boolean {
        // language=SQL
        val sql =
            """
            SELECT EXISTS(
                SELECT * FROM message_account WHERE daycare_group_id = :daycareGroupId AND active = true
            )
        """.trimIndent(
            )
        return db.read {
            it.createQuery(sql).bind("daycareGroupId", groupId).mapTo<Boolean>().first()
        }
    }
}
