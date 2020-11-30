// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

class DaycareControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var daycareController: DaycareController

    val daycareId: UUID = UUID.fromString("68851e10-eb86-443e-b28d-0f6ee9642a3c")
    val groupName = "foo"
    val groupFounded: LocalDate = LocalDate.of(2019, 5, 20)
    val initialCaretakers = 4.5

    @Test
    fun `smoke test for groups`() {
        val user = AuthenticatedUser(UUID.randomUUID(), setOf(UserRole.ADMIN))
        val existingGroups = daycareController.getGroups(db, user, daycareId).body!!
        assertEquals(2, existingGroups.size)

        val created = daycareController.createGroup(
            db,
            user,
            daycareId,
            DaycareController.CreateGroupRequest(
                name = groupName,
                startDate = groupFounded,
                initialCaretakers = initialCaretakers
            )
        ).body!!

        val groups = daycareController.getGroups(db, user, daycareId).body!!
        assertEquals(3, groups.size)

        val group = groups.find { it.id == created.id }!!
        assertEquals(daycareId, group.daycareId)
        assertEquals(groupName, group.name)
        assertEquals(groupFounded, group.startDate)
        assertNull(group.endDate)

        val stats = daycareController.getStats(db, user, daycareId, LocalDate.now(), LocalDate.now().plusDays(50)).body!!
        val groupStats = stats.groupCaretakers[group.id]!!
        assertEquals(initialCaretakers, groupStats.minimum)
        assertEquals(initialCaretakers, groupStats.maximum)

        val deleteStatus = daycareController.deleteGroup(db, user, daycareId, group.id).statusCode
        assertEquals(HttpStatus.NO_CONTENT, deleteStatus)
        val groupsAfterDelete = daycareController.getGroups(db, user, daycareId).body!!
        assertEquals(2, groupsAfterDelete.size)
    }
}
