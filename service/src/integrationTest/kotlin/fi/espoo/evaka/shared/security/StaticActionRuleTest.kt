// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class StaticActionRuleTest : AccessControlTest() {
    private val strongCitizen =
        AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
    private val weakCitizen =
        AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.WEAK)
    private val employee = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet())

    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @Test
    fun `IsCitizen permits only strongly authenticated citizen if allowWeakLogin = false`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = false).any())
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, strongCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, weakCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, employee, clock, action))
        }
    }

    @Test
    fun `IsCitizen permits any citizen if allowWeakLogin = true`() {
        val action = Action.Global.READ_HOLIDAY_PERIODS
        rules.add(action, IsCitizen(allowWeakLogin = true).any())
        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, strongCitizen, clock, action))
            assertTrue(accessControl.hasPermissionFor(tx, weakCitizen, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, employee, clock, action))
        }
    }

    @Test
    fun `HasGlobalRole permits if user has the right global role`() {
        val action = Action.Global.CREATE_UNIT
        rules.add(action, HasGlobalRole(UserRole.REPORT_VIEWER, UserRole.FINANCE_ADMIN))
        val permittedEmployee =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN),
            )
        val deniedEmployee =
            AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.DIRECTOR))

        db.read { tx ->
            assertTrue(accessControl.hasPermissionFor(tx, permittedEmployee, clock, action))
            assertFalse(accessControl.hasPermissionFor(tx, deniedEmployee, clock, action))
        }
    }
}
