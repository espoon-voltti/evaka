// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { concat } from 'lodash'
import { NextFunction, Request, Response } from 'express'
import { logAuditEvent } from '../logging'
import { gatewayRole } from '../config'
import { createJwt } from './jwt'
import { SamlUser } from '../routes/auth/saml/types'

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

function createJwtToken(user: SamlUser): string {
  const type =
    user.userType ?? (gatewayRole === 'enduser' ? 'ENDUSER' : 'EMPLOYEE')

  const common = {
    kind: gatewayRole === 'enduser' ? 'SuomiFI' : 'AD',
    sub: user.id
  } as const

  switch (type) {
    case 'ENDUSER':
    case 'CITIZEN_STRONG':
      return createJwt({ ...common, evaka_type: 'citizen' })
    case 'CITIZEN_WEAK':
      return createJwt({ ...common, evaka_type: 'citizen_weak' })
    case 'MOBILE':
      return createJwt({ ...common, evaka_type: 'mobile' })
    case 'SYSTEM':
      return createJwt({ ...common, evaka_type: 'system' })
    case 'EMPLOYEE':
      const roles =
        user.roles ?? concat(user.globalRoles ?? [], user.allScopedRoles ?? [])
      return createJwt({
        ...common,
        evaka_type: 'employee',
        scope: roles
          .map((role) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`))
          .join(' ')
      })
    default:
      throw new Error(`Unsupported user type ${type}`)
  }
}

export function createAuthHeader(user: SamlUser): string {
  return `Bearer ${createJwtToken(user)}`
}
