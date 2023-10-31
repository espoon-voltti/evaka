// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
import fi.espoo.evaka.note.child.daily.createChildDailyNote
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ChildDailyNoteAccessControlTest : AccessControlTest() {
    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)
    private val dummyNote =
        ChildDailyNoteBody(
            "test",
            feedingNote = null,
            sleepingNote = null,
            sleepingMinutes = null,
            reminders = emptyList(),
            reminderNote = "",
        )

    @Test
    fun `HasUnitRole inPlacementUnitOfChildOfChildDailyNote`() {
        val area = db.transaction { it.insert(DevCareArea()) }
        val placementUnit = db.transaction { it.insert(DevDaycare(areaId = area)) }
        val otherUnit = db.transaction { it.insert(DevDaycare(areaId = area)) }

        val placedChild = db.transaction { it.insert(DevPerson(), DevPersonType.CHILD) }
        db.transaction {
            it.insert(
                DevPlacement(
                    unitId = placementUnit,
                    childId = placedChild,
                    startDate = clock.today(),
                    endDate = clock.today().plusDays(30)
                )
            )
        }
        val placedChildNote = db.transaction { it.createChildDailyNote(placedChild, dummyNote) }
        val otherChild = db.transaction { it.insert(DevPerson(), DevPersonType.CHILD) }
        val otherChildNote = db.transaction { it.createChildDailyNote(otherChild, dummyNote) }

        val unitSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(placementUnit to UserRole.UNIT_SUPERVISOR)
            )
        val otherSupervisor =
            createTestEmployee(
                globalRoles = emptySet(),
                unitRoles = mapOf(otherUnit to UserRole.UNIT_SUPERVISOR)
            )

        val action = Action.ChildDailyNote.UPDATE
        rules.add(
            action,
            HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChildOfChildDailyNote()
        )

        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, placedChildNote)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, unitSupervisor, clock, action, otherChildNote)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, otherSupervisor, clock, action, placedChildNote)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, otherSupervisor, clock, action, otherChildNote)
            )
        }
    }
}
