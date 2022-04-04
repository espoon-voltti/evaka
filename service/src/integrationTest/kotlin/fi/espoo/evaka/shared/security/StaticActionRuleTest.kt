// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticActionRuleTest : AccessControlTest() {
    private val strongCitizen = AuthenticatedUser.Citizen(UUID.randomUUID(), CitizenAuthLevel.STRONG)
    private val weakCitizen = AuthenticatedUser.Citizen(UUID.randomUUID(), CitizenAuthLevel.WEAK)
    private val employee = AuthenticatedUser.Employee(UUID.randomUUID(), emptySet())

    @Test
    fun `IsCitizen permits only strongly authenticated citizen if allowWeakLogin = false`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = false).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, action))
        assertFalse(accessControl.hasPermissionFor(weakCitizen, action))
        assertFalse(accessControl.hasPermissionFor(employee, action))
    }

    @Test
    fun `IsCitizen permits any citizen if allowWeakLogin = true`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = true).any())
        assertTrue(accessControl.hasPermissionFor(strongCitizen, action))
        assertTrue(accessControl.hasPermissionFor(weakCitizen, action))
        assertFalse(accessControl.hasPermissionFor(employee, action))
    }

    @Test
    fun `HasGlobalRole permits if user has the right global role`() {
        val action = Action.Global.CREATE_UNIT
        rules.add(action, HasGlobalRole(UserRole.REPORT_VIEWER, UserRole.FINANCE_ADMIN))
        val permittedEmployee = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN))
        val deniedEmployee = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.DIRECTOR))

        assertTrue(accessControl.hasPermissionFor(permittedEmployee, action))
        assertFalse(accessControl.hasPermissionFor(deniedEmployee, action))
    }
}
