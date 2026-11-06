// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import { appCommit } from '../../shared/config.ts'
import { toRequestHandler } from '../../shared/express.ts'
import type { UUID } from '../../shared/service-client.ts'
import {
  authenticateMobileDevice,
  getEmployeeDetails
} from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'
import { updateEmployeeSessionRoles } from '../employee-session-refresh.ts'

export interface AuthStatus {
  loggedIn: boolean
  user?: EmployeeUser | MobileUser
  featureConfig?: object
  /** @deprecated removed in a follow-up PR; frontend gates on permitted actions / startPage */
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
  startPage: string
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
  featureConfig?: object
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
        featureConfig: undefined,
        globalRoles: [],
        allScopedRoles: []
      }
    }
    case 'EMPLOYEE': {
      const response = await getEmployeeDetails(req, user.id)
      if (response.status !== 'found') {
        return undefined
      }
      const { user: employee, featureConfig } = response.details
      const {
        id,
        firstName,
        lastName,
        globalRoles,
        allScopedRoles,
        accessibleFeatures,
        permittedGlobalActions,
        startPage
      } = employee
      const name = [firstName, lastName].filter((x) => !!x).join(' ')
      return {
        user: {
          userType: 'EMPLOYEE',
          id,
          name,
          accessibleFeatures,
          permittedGlobalActions,
          startPage
        },
        featureConfig,
        globalRoles,
        allScopedRoles
      }
    }
    default:
      return undefined
  }
}

export const internalAuthStatus = (
  sessions: Sessions<'employee' | 'employee-mobile'>
) =>
  toRequestHandler(async (req, res) => {
    const sessionUser = sessions.getUser(req)
    const validUser = sessionUser && (await validateUser(req, sessionUser))
    let status: AuthStatus
    if (validUser) {
      const { user, featureConfig, globalRoles, allScopedRoles } = validUser
      if (sessionUser?.userType === 'EMPLOYEE') {
        await updateEmployeeSessionRoles(req, sessions, sessionUser, validUser)
      }
      status = {
        loggedIn: true,
        user,
        featureConfig,
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
