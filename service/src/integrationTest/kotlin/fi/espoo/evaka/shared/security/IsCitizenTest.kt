// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsCitizenTest : AccessControlTest() {
    private val strongCitizen = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
    private val weakCitizen = AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.WEAK)
    private val employee = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet())

    @Test
    fun `permits only strongly authenticated citizen if allowWeakLogin = false`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = false).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, action))
        assertFalse(accessControl.hasPermissionFor(weakCitizen, action))
        assertFalse(accessControl.hasPermissionFor(employee, action))
    }

    @Test
    fun `permits any citizen if allowWeakLogin = true`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = true).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, action))
        assertTrue(accessControl.hasPermissionFor(weakCitizen, action))
        assertFalse(accessControl.hasPermissionFor(employee, action))
    }

    @Test
    fun `self() permits only if the target is the same citizen user doing the action`() {
        val action = Action.Person.READ
        rules.add(action, IsCitizen(allowWeakLogin = false).self())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, action, strongCitizen.id))
        assertFalse(accessControl.hasPermissionFor(strongCitizen, action, weakCitizen.id))
        assertFalse(accessControl.hasPermissionFor(weakCitizen, action, weakCitizen.id))
    }
}
