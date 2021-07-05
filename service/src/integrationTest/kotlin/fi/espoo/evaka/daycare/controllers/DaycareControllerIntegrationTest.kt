// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.initCaretakers
import fi.espoo.evaka.daycare.service.DaycareCapacityStats
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.daycare.service.Stats
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.message.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.message.insertMessageContent
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
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DaycareControllerIntegrationTest : FullApplicationTest() {
    private val childId = testChild_1.id
    private val daycareId = testDaycare.id
    private val supervisorId = UUID.randomUUID()
    private val supervisor = AuthenticatedUser.Employee(supervisorId, emptySet())
    private val staffId = UUID.randomUUID()
    private val staffMember = AuthenticatedUser.Employee(staffId, emptySet())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
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
        val (daycare, _, currentUserRoles) = getDaycare(daycareId)

        db.read { assertEquals(it.getDaycare(daycareId), daycare) }
        assertEquals(setOf(UserRole.STAFF), currentUserRoles)
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

    @Test
    fun `get daycare stats`() {
        val group1 = DevDaycareGroup(daycareId = daycareId)
        val group2 = DevDaycareGroup(daycareId = daycareId)
        db.transaction {
            it.insertTestDaycareGroup(group1)
            it.insertTestDaycareGroup(group2)
            it.initCaretakers(group1.id, LocalDate.of(2019, 1, 1), 5.0)
            it.initCaretakers(group2.id, LocalDate.of(2019, 1, 1), 3.5)
        }

        val stats = getDaycareStats(
            daycareId,
            from = LocalDate.of(2019, 1, 1),
            to = LocalDate.of(2019, 1, 1)
        )

        assertEquals(
            mapOf(
                group1.id to Stats(minimum = 5.0, maximum = 5.0),
                group2.id to Stats(minimum = 3.5, maximum = 3.5)
            ),
            stats.groupCaretakers
        )
        assertEquals(Stats(minimum = 8.5, maximum = 8.5), stats.unitTotalCaretakers)
    }

    private fun getDaycare(daycareId: UUID): DaycareResponse {
        val (_, res, body) = http.get("/daycares/$daycareId")
            .asUser(staffMember)
            .responseObject<DaycareResponse>(objectMapper)

        assertEquals(200, res.statusCode)
        return body.get()
    }

    private fun getDaycareStats(daycareId: UUID, from: LocalDate, to: LocalDate): DaycareCapacityStats {
        val (_, res, body) = http.get("/daycares/$daycareId/stats?from=$from&to=$to")
            .asUser(staffMember)
            .responseObject<DaycareCapacityStats>(objectMapper)

        assertEquals(200, res.statusCode)
        return body.get()
    }

    private fun createDaycareGroup(
        daycareId: UUID,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double
    ): DaycareGroup {
        val (_, res, body) = http.post("/daycares/$daycareId/groups")
            .jsonBody(
                objectMapper.writeValueAsString(
                    DaycareController.CreateGroupRequest(
                        name = name,
                        startDate = startDate,
                        initialCaretakers = initialCaretakers
                    )
                )
            )
            .asUser(supervisor)
            .responseObject<DaycareGroup>(objectMapper)

        assertEquals(201, res.statusCode)
        return body.get()
    }

    private fun deleteDaycareGroup(daycareId: UUID, groupId: UUID, expectedStatus: Int = 204) {
        val (_, res) = http.delete("/daycares/$daycareId/groups/$groupId")
            .asUser(supervisor)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun groupHasMessageAccount(groupId: UUID): Boolean {
        // language=SQL
        val sql = """
            SELECT EXISTS(
                SELECT * FROM message_account WHERE daycare_group_id = :daycareGroupId AND active = true
            )
        """.trimIndent()
        return db.read {
            it.createQuery(sql)
                .bind("daycareGroupId", groupId)
                .mapTo<Boolean>().first()
        }
    }
}
