// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.DaycareController
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestBackupCare
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DaycareGroupIntegrationTest : FullApplicationTest() {
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))

    @BeforeEach
    protected fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
            tx.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare.id, name = testDaycare.name))
        }
    }

    @Test
    fun testCreate() {
        val name = "Ryh mä"
        val startDate = LocalDate.of(2019, 1, 1)
        val caretakers = 42.0
        val (_, res, body) = http.post("/daycares/${testDaycare.id}/groups")
            .jsonBody(
                objectMapper.writeValueAsString(
                    DaycareController.CreateGroupRequest(
                        name = name,
                        startDate = startDate,
                        initialCaretakers = caretakers
                    )
                )
            )
            .asUser(admin)
            .responseObject<DaycareGroup>(objectMapper)
        assertTrue(res.isSuccessful)

        val group = body.get()

        assertEquals(testDaycare.id, group.daycareId)
        assertEquals(name, group.name)
        assertEquals(startDate, group.startDate)
        assertNull(group.endDate)
        assertTrue(group.deletable)

        getAndAssertGroup(group)
    }

    @Test
    fun testDeletable() {
        val group = DaycareGroup(
            id = UUID.randomUUID(),
            daycareId = testDaycare.id,
            name = "Lap set",
            startDate = LocalDate.of(2019, 1, 1),
            endDate = null,
            deletable = true
        )
        db.transaction {
            it.insertTestDaycareGroup(
                DevDaycareGroup(
                    id = group.id,
                    daycareId = group.daycareId,
                    name = group.name,
                    startDate = group.startDate
                )
            )
        }
        getAndAssertGroup(group)

        db.transaction { tx ->
            tx.insertTestPerson(DevPerson(testChild_1.id))
            tx.insertTestChild(DevChild(testChild_1.id))
            tx.insertTestBackupCare(
                DevBackupCare(
                    childId = testChild_1.id,
                    groupId = group.id,
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
                    unitId = group.daycareId
                )
            )
        }
        getAndAssertGroup(group.copy(deletable = false))
    }

    private fun getAndAssertGroup(expected: DaycareGroup) {
        val (_, res, body) = http.get("/daycares/${expected.daycareId}/groups")
            .asUser(admin)
            .responseObject<List<DaycareGroup>>(objectMapper)
        assertTrue(res.isSuccessful)

        val groups = body.get()
        assertEquals(1, groups.size)
        val group = groups[0]
        assertEquals(expected.id, group.id)
        assertEquals(expected.daycareId, group.daycareId)
        assertEquals(expected.name, group.name)
        assertEquals(expected.startDate, group.startDate)
        assertEquals(expected.endDate, group.endDate)
        assertEquals(expected.deletable, group.deletable)
    }
}
