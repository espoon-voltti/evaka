// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'
import _ from 'lodash'

import type { EmployeeSessionUser } from '../shared/auth/index.ts'
import type { Config } from '../shared/config.ts'
import { toMiddleware } from '../shared/express.ts'
import { logAuditEvent, logWarn } from '../shared/logging.ts'
import type { EmployeeDetailsResult } from '../shared/service-client.ts'
import { getEmployeeDetails } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

interface EmployeeRoles {
  globalRoles: string[]
  allScopedRoles: string[]
}

const rolesChanged = (a: string[] | undefined, b: string[] | undefined) =>
  !_.isEqual(new Set(a), new Set(b))

export async function updateEmployeeSessionRoles(
  req: express.Request,
  sessions: Sessions<'employee' | 'employee-mobile'>,
  sessionUser: EmployeeSessionUser,
  roles: EmployeeRoles
): Promise<void> {
  req.session.employeeRolesCheckedAt = Date.now()
  if (
    rolesChanged(sessionUser.globalRoles, roles.globalRoles) ||
    rolesChanged(sessionUser.allScopedRoles, roles.allScopedRoles)
  ) {
    await sessions.updateUser(req, {
      ...sessionUser,
      globalRoles: roles.globalRoles,
      allScopedRoles: roles.allScopedRoles
    })
  }
}

export function employeeSessionMaxAge(
  sessions: Sessions<'employee'>,
  config: Pick<Config['employee'], 'maxSessionAgeMinutes'>
): express.RequestHandler {
  return toMiddleware(async (req, res) => {
    const sessionUser = sessions.getUser(req)
    if (sessionUser?.userType !== 'EMPLOYEE') return
    const now = Date.now()
    req.session.employeeLoginAt ??= now

    if (
      config.maxSessionAgeMinutes !== undefined &&
      now - req.session.employeeLoginAt > config.maxSessionAgeMinutes * 60_000
    ) {
      logAuditEvent(
        'evaka.employee.session.max_age_reached',
        req,
        'Employee session ended after reaching its maximum age'
      )
      await sessions.destroy(req, res)
    }
  })
}

export function employeeRoleRefresh(
  sessions: Sessions<'employee'>,
  config: Pick<Config['employee'], 'roleRefreshIntervalSeconds'>
): express.RequestHandler {
  return toMiddleware(async (req, res) => {
    const sessionUser = sessions.getUser(req)
    if (sessionUser?.userType !== 'EMPLOYEE') return
    const session = req.session
    const now = Date.now()

    if (
      session.employeeRolesCheckedAt !== undefined &&
      now - session.employeeRolesCheckedAt <
        config.roleRefreshIntervalSeconds * 1000
    )
      return

    let result: EmployeeDetailsResult
    try {
      result = await getEmployeeDetails(req, sessionUser.id)
    } catch (err) {
      logWarn('Employee role refresh failed, retrying on next request', req, {
        error: err instanceof Error ? err.message : String(err)
      })
      return
    }
    if (result.status !== 'found') {
      if (result.status === 'inactive') {
        logAuditEvent(
          'evaka.employee.session.employee_inactive',
          req,
          'Employee session ended because the employee is deactivated'
        )
      } else {
        logAuditEvent(
          'evaka.employee.session.employee_not_found',
          req,
          'Employee session ended because the employee no longer exists'
        )
      }
      await sessions.destroy(req, res)
      return
    }
    await updateEmployeeSessionRoles(
      req,
      sessions,
      sessionUser,
      result.details.user
    )
  })
}
