// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

import evaka.core.shared.auth.UserRole

/** The page an employee is routed to immediately after login. */
enum class EmployeeStartPage {
    APPLICATIONS,
    UNITS,
    REPORTS,
    MESSAGES,
    WELCOME,
    SEARCH,
}

/**
 * Computes the post-login landing page from the employee's roles. Ported verbatim from the
 * previous frontend logic in App.tsx (RedirectToMainPage) so behavior is preserved.
 */
fun employeeStartPage(
    globalRoles: Set<UserRole>,
    allScopedRoles: Set<UserRole>,
): EmployeeStartPage {
    val roles = globalRoles + allScopedRoles
    return when {
        UserRole.SERVICE_WORKER in roles || UserRole.SPECIAL_EDUCATION_TEACHER in roles ->
            EmployeeStartPage.APPLICATIONS
        UserRole.UNIT_SUPERVISOR in roles || UserRole.STAFF in roles -> EmployeeStartPage.UNITS
        UserRole.DIRECTOR in roles || UserRole.REPORT_VIEWER in roles -> EmployeeStartPage.REPORTS
        UserRole.MESSAGING in roles -> EmployeeStartPage.MESSAGES
        roles.isEmpty() -> EmployeeStartPage.WELCOME
        else -> EmployeeStartPage.SEARCH
    }
}
