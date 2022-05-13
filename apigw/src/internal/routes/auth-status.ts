// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { eq } from 'lodash'
import { toRequestHandler } from '../../shared/express'
import {
  getEmployeeDetails,
  getMobileDevice,
  UUID
} from '../../shared/service-client'
import { logoutExpress, saveSession } from '../../shared/session'
import { fromCallback } from '../../shared/promise-utils'
import { appCommit } from '../../shared/config'

interface AuthStatus {
  loggedIn: boolean
  user?: EmployeeUser | MobileUser
  roles?: string[]
  globalRoles?: string[]
  allScopedRoles?: string[]
  apiVersion: string
}

interface EmployeeUser {
  userType: 'EMPLOYEE'
  id: UUID
  name: string
  accessibleFeatures: object
  permittedGlobalActions: string[]
}

interface MobileUser {
  userType: 'MOBILE'
  employeeId: UUID | null
  id: UUID
  name: string
  personalDevice: boolean
  unitIds: UUID[]
  pinLoginActive: boolean
}

interface ValidatedUser {
  user: EmployeeUser | MobileUser
  globalRoles: string[]
  allScopedRoles: string[]
}

async function validateUser(
  req: express.Request
): Promise<ValidatedUser | undefined> {
  const user = req.user
  if (!user || !user.id) return undefined
  switch (user.userType) {
    case 'MOBILE': {
      const device = await getMobileDevice(req, user.id)
      if (!device) {
        return undefined
      }
      const employeeId = device.employeeId ?? user.mobileEmployeeId ?? null
      const pinLoginActive = !!user.mobileEmployeeId
      const { id, name, unitIds, personalDevice } = device
      return {
        user: {
          id,
          name,
          userType: 'MOBILE',
          unitIds,
          employeeId,
          pinLoginActive,
          personalDevice
        },
        globalRoles: [],
        allScopedRoles: ['MOBILE']
      }
    }
    case 'EMPLOYEE': {
      const employee = await getEmployeeDetails(req, user.id)
      if (!employee) {
        return undefined
      }
      const {
        id,
        firstName,
        lastName,
        globalRoles,
        allScopedRoles,
        accessibleFeatures,
        permittedGlobalActions
      } = employee
      const name = [firstName, lastName].filter((x) => !!x).join(' ')
      return {
        user: {
          userType: 'EMPLOYEE',
          id,
          name,
          accessibleFeatures: accessibleFeatures ?? {},
          permittedGlobalActions: permittedGlobalActions ?? []
        },
        globalRoles,
        allScopedRoles
      }
    }
    default:
      return undefined
  }
}

export default toRequestHandler(async (req, res) => {
  const sessionUser = req.user
  const validUser = sessionUser && (await validateUser(req))
  let status: AuthStatus
  if (validUser) {
    const { user, globalRoles, allScopedRoles } = validUser
    // Refresh roles if necessary
    if (
      !eq(sessionUser.globalRoles, globalRoles) ||
      !eq(sessionUser.allScopedRoles, allScopedRoles)
    ) {
      await fromCallback((cb) =>
        req.logIn({ ...sessionUser, globalRoles, allScopedRoles }, cb)
      )
    }
    await saveSession(req)
    status = {
      loggedIn: true,
      user,
      globalRoles,
      allScopedRoles,
      roles: [...globalRoles, ...allScopedRoles],
      apiVersion: appCommit
    }
  } else {
    if (sessionUser) {
      await logoutExpress(req, res, 'employee')
    }
    status = {
      loggedIn: false,
      apiVersion: appCommit
    }
  }
  res.status(200).json(status)
})
