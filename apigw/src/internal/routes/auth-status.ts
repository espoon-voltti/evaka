// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import _ from 'lodash'

import { EvakaSessionUser } from '../../shared/auth/index.js'
import { appCommit } from '../../shared/config.js'
import { toRequestHandler } from '../../shared/express.js'
import {
  authenticateMobileDevice,
  getEmployeeDetails,
  UUID
} from '../../shared/service-client.js'
import { Sessions } from '../../shared/session.js'

export interface AuthStatus {
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
  pushApplicationServerKey: string | undefined
}

interface ValidatedUser {
  user: EmployeeUser | MobileUser
  globalRoles: string[]
  allScopedRoles: string[]
}

async function validateUser(
  req: express.Request,
  user: EvakaSessionUser
): Promise<ValidatedUser | undefined> {
  if (!user || !user.id) return undefined
  switch (user.userType) {
    case 'MOBILE': {
      const device = await authenticateMobileDevice(req, user.id)
      if (!device) {
        return undefined
      }
      const employeeId = device.employeeId ?? user.mobileEmployeeId ?? null
      const pinLoginActive = !!user.mobileEmployeeId
      const { id, name, unitIds, personalDevice, pushApplicationServerKey } =
        device
      return {
        user: {
          id,
          name,
          userType: 'MOBILE',
          unitIds,
          employeeId,
          pinLoginActive,
          personalDevice,
          pushApplicationServerKey
        },
        globalRoles: [],
        allScopedRoles: []
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

const rolesChanged = (a: string[] | undefined, b: string[] | undefined) =>
  !_.isEqual(new Set(a), new Set(b))

const userChanged = (sessionUser: Express.User, user: ValidatedUser): boolean =>
  rolesChanged(sessionUser.allScopedRoles, user.allScopedRoles) ||
  rolesChanged(sessionUser.globalRoles, user.globalRoles)

export default (sessions: Sessions) =>
  toRequestHandler(async (req, res) => {
    const sessionUser = sessions.getUser(req)
    const validUser = sessionUser && (await validateUser(req, sessionUser))
    let status: AuthStatus
    if (validUser) {
      const { user, globalRoles, allScopedRoles } = validUser
      // Refresh roles if necessary
      if (userChanged(sessionUser, validUser)) {
        await sessions.updateUser(req, {
          ...sessionUser,
          globalRoles,
          allScopedRoles
        })
      }
      status = {
        loggedIn: true,
        user,
        globalRoles,
        allScopedRoles,
        roles: [...globalRoles, ...allScopedRoles],
        apiVersion: appCommit
      }
    } else {
      await sessions.destroy(req, res)
      status = {
        loggedIn: false,
        apiVersion: appCommit
      }
    }
    res.status(200).json(status)
  })
