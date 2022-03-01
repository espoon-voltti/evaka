// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.holidayperiod

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.StaticPermittedRoleActions
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HolidayPeriodAclIntegrationTest : PureJdbiTest() {
    private lateinit var accessControl: AccessControl

    private val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId.raw, roles = setOf(UserRole.ADMIN))
    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val supervisor = AuthenticatedUser.Employee(supervisorId.raw, roles = setOf(UserRole.UNIT_SUPERVISOR))

    @BeforeAll
    fun before() {
        accessControl = AccessControl(StaticPermittedRoleActions(), DefaultActionRuleMapping(), AccessControlList(jdbi), jdbi)
    }

    private val holidayPeriodActions = listOf(
        Action.Global.CREATE_HOLIDAY_PERIOD,
        Action.Global.READ_HOLIDAY_PERIOD,
        Action.Global.READ_HOLIDAY_PERIODS,
        Action.Global.DELETE_HOLIDAY_PERIOD,
        Action.Global.UPDATE_HOLIDAY_PERIOD
    )

    @Test
    fun `unit supervisor cannot create, update and delete holiday periods`() {
        holidayPeriodActions.forEach {
            assertThrows<Forbidden> { accessControl.requirePermissionFor(supervisor, it) }
        }
    }

    @Test
    fun `admin can create, update and delete holiday periods`() {
        holidayPeriodActions.forEach {
            assertDoesNotThrow { accessControl.requirePermissionFor(admin, it) }
        }
    }
}
