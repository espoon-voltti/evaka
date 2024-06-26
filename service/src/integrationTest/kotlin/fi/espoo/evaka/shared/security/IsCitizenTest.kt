// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IsCitizenTest : AccessControlTest() {
    private val strongPerson = DevPerson()
    private val strongCitizen = strongPerson.user(CitizenAuthLevel.STRONG)
    private val weakPerson = DevPerson()
    private val weakCitizen = weakPerson.user(CitizenAuthLevel.WEAK)
    private val employee = DevEmployee()

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(strongPerson, DevPersonType.ADULT)
            tx.insert(weakPerson, DevPersonType.ADULT)
            tx.insert(employee)
        }
    }

    @Test
    fun `permits only strongly authenticated citizen if allowWeakLogin = false`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = false).any())
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, strongCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, weakCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, employee.user, clock, action))
        }
    }

    @Test
    fun `permits any citizen if allowWeakLogin = true`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = true).any())
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, strongCitizen, clock, action))
            assertTrue(accessControl.hasPermissionFor(tx, weakCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, employee.user, clock, action))
        }
    }

    @Test
    fun `self() permits only if the target is the same citizen user doing the action`() {
        val action = Action.Person.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).self())
        db.read { tx ->
            assertTrue(
                accessControl.hasPermissionFor(tx, strongCitizen, clock, action, strongCitizen.id)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, strongCitizen, clock, action, weakCitizen.id)
            )
            assertFalse(
                accessControl.hasPermissionFor(tx, weakCitizen, clock, action, weakCitizen.id)
            )
        }
    }

    @Test
    fun `IsCitizen and isPermittedForSomeTarget check`() {
        val action = Action.Child.READ
        fun isPermittedForSomeTarget(user: AuthenticatedUser) =
            db.read { accessControl.isPermittedForSomeTarget(it, user, clock, action) }

        rules.add(action, IsCitizen(allowWeakLogin = false).guardianOfChild())

        assertFalse(isPermittedForSomeTarget(weakCitizen))
        assertFalse(isPermittedForSomeTarget(strongCitizen))

        db.transaction { tx ->
            val child = DevPerson()
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = weakCitizen.id, childId = child.id))
            tx.insert(DevGuardian(guardianId = strongCitizen.id, childId = child.id))
        }

        assertFalse(isPermittedForSomeTarget(weakCitizen))
        assertTrue(isPermittedForSomeTarget(strongCitizen))
    }
}
