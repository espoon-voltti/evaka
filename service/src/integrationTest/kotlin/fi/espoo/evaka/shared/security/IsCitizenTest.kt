// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class IsCitizenTest : AccessControlTest() {
    private val strongCitizen =
        AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
    private val weakCitizen =
        AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.WEAK)
    private val employee = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet())

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `permits only strongly authenticated citizen if allowWeakLogin = false`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = false).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, clock, action))
        assertFalse(accessControl.hasPermissionFor(weakCitizen, clock, action))
        assertFalse(accessControl.hasPermissionFor(employee, clock, action))
    }

    @Test
    fun `permits any citizen if allowWeakLogin = true`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = true).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, clock, action))
        assertTrue(accessControl.hasPermissionFor(weakCitizen, clock, action))
        assertFalse(accessControl.hasPermissionFor(employee, clock, action))
    }

    @Test
    fun `self() permits only if the target is the same citizen user doing the action`() {
        val action = Action.Person.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).self())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, clock, action, strongCitizen.id))
        assertFalse(accessControl.hasPermissionFor(strongCitizen, clock, action, weakCitizen.id))
        assertFalse(accessControl.hasPermissionFor(weakCitizen, clock, action, weakCitizen.id))
    }
}
