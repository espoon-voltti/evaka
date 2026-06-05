// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

import evaka.core.shared.auth.UserRole
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class EmployeeStartPageTest {
    @Test
    fun `service worker lands on applications`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(setOf(UserRole.SERVICE_WORKER), emptySet()),
        )
    }

    @Test
    fun `scoped special education teacher lands on applications`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(emptySet(), setOf(UserRole.SPECIAL_EDUCATION_TEACHER)),
        )
    }

    @Test
    fun `unit supervisor and staff land on units`() {
        assertEquals(
            EmployeeStartPage.UNITS,
            employeeStartPage(emptySet(), setOf(UserRole.UNIT_SUPERVISOR)),
        )
        assertEquals(
            EmployeeStartPage.UNITS,
            employeeStartPage(emptySet(), setOf(UserRole.STAFF)),
        )
    }

    @Test
    fun `director and report viewer land on reports`() {
        assertEquals(
            EmployeeStartPage.REPORTS,
            employeeStartPage(setOf(UserRole.DIRECTOR), emptySet()),
        )
        assertEquals(
            EmployeeStartPage.REPORTS,
            employeeStartPage(setOf(UserRole.REPORT_VIEWER), emptySet()),
        )
    }

    @Test
    fun `messaging lands on messages`() {
        assertEquals(
            EmployeeStartPage.MESSAGES,
            employeeStartPage(setOf(UserRole.MESSAGING), emptySet()),
        )
    }

    @Test
    fun `no roles lands on welcome`() {
        assertEquals(EmployeeStartPage.WELCOME, employeeStartPage(emptySet(), emptySet()))
    }

    @Test
    fun `other roles land on search`() {
        assertEquals(
            EmployeeStartPage.SEARCH,
            employeeStartPage(setOf(UserRole.FINANCE_ADMIN), emptySet()),
        )
    }

    @Test
    fun `applications takes priority over units`() {
        assertEquals(
            EmployeeStartPage.APPLICATIONS,
            employeeStartPage(setOf(UserRole.SERVICE_WORKER), setOf(UserRole.UNIT_SUPERVISOR)),
        )
    }
}
