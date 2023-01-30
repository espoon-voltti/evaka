// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

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
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
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
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DaycareControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var daycareController: DaycareController

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
                DevEmployee(id = supervisorId, firstName = "Elina", lastName = "Esimies")
            )
            tx.insertTestEmployee(DevEmployee(id = staffId))
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

        assertThrows<Conflict> { deleteDaycareGroup(daycareId, group.id) }

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
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
                )
            )
        }

        assertThrows<Conflict> { deleteDaycareGroup(daycareId, group.id) }

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

        assertThrows<Conflict> { deleteDaycareGroup(daycareId, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    private fun getDaycare(daycareId: DaycareId): DaycareController.DaycareResponse {
        return daycareController.getDaycare(dbInstance(), staffMember, RealEvakaClock(), daycareId)
    }

    private fun createDaycareGroup(
        daycareId: DaycareId,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double
    ): DaycareGroup {
        return daycareController.createGroup(
            dbInstance(),
            supervisor,
            RealEvakaClock(),
            daycareId,
            DaycareController.CreateGroupRequest(name, startDate, initialCaretakers)
        )
    }

    private fun deleteDaycareGroup(
        daycareId: DaycareId,
        groupId: GroupId,
    ) {
        daycareController.deleteGroup(
            dbInstance(),
            supervisor,
            RealEvakaClock(),
            daycareId,
            groupId
        )
    }

    private fun groupHasMessageAccount(groupId: GroupId): Boolean {
        // language=SQL
        val sql =
            """
            SELECT EXISTS(
                SELECT * FROM message_account WHERE daycare_group_id = :daycareGroupId AND active = true
            )
        """
                .trimIndent()
        return db.read {
            it.createQuery(sql).bind("daycareGroupId", groupId).mapTo<Boolean>().first()
        }
    }
}
