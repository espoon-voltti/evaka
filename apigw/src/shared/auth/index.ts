// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { concat } from 'lodash'
import { NextFunction, Request, Response } from 'express'
import { logAuditEvent } from '../logging'
import { gatewayRole } from '../config'
import { createJwt } from './jwt'
import { Profile } from '@node-saml/passport-saml'
import { UserType } from '../service-client'

const auditEventGatewayId =
  (gatewayRole === 'enduser' && 'eugw') ||
  (gatewayRole === 'internal' && 'ingw') ||
  (gatewayRole === undefined && 'devgw')

export function requireAuthentication(
  req: Request,
  res: Response,
  next: NextFunction
) {
  if (!req.user || !req.user.id) {
    logAuditEvent(
      `evaka.${auditEventGatewayId}.auth.not_found`,
      req,
      'Could not find user'
    )
    res.sendStatus(401)
    return
  }
  return next()
}

export interface EvakaSessionUser {
  // eVaka id
  id?: string | undefined
  userType?: UserType | undefined
  // all are optional because of legacy sessions
  roles?: string[] | undefined
  globalRoles?: string[] | undefined
  allScopedRoles?: string[] | undefined
  mobileEmployeeId?: string | undefined
}

function createJwtToken(user: EvakaSessionUser): string {
  const type =
    user.userType ?? (gatewayRole === 'enduser' ? 'ENDUSER' : 'EMPLOYEE')

  if (!user.id) {
    throw new Error('User is missing an id')
  }

  const common = {
    sub: user.id
  } as const

  switch (type) {
    case 'ENDUSER':
    case 'CITIZEN_STRONG':
      return createJwt({ ...common, evaka_type: 'citizen' })
    case 'CITIZEN_WEAK':
      return createJwt({ ...common, evaka_type: 'citizen_weak' })
    case 'MOBILE':
      return createJwt({
        ...common,
        evaka_employee_id: user.mobileEmployeeId,
        evaka_type: 'mobile'
      })
    case 'SYSTEM':
      return createJwt({ ...common, evaka_type: 'system' })
    case 'EMPLOYEE': {
      const roles =
        user.roles ?? concat(user.globalRoles ?? [], user.allScopedRoles ?? [])
      return createJwt({
        ...common,
        evaka_type: 'employee',
        scope: roles
          .map((role) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`))
          .join(' ')
      })
    }
    default:
      throw new Error(`Unsupported user type ${type}`)
  }
}

export function createAuthHeader(user: EvakaSessionUser): string {
  return `Bearer ${createJwtToken(user)}`
}

export function createIntegrationAuthHeader(): string {
  return `Bearer ${createJwt({
    sub: '00000000-0000-0000-0000-000000000000',
    evaka_type: 'integration'
  })}`
}

export function createLogoutToken(
  nameID: Required<Profile>['nameID'],
  sessionIndex: Profile['sessionIndex']
) {
  return `${nameID}:::${sessionIndex}`
}
