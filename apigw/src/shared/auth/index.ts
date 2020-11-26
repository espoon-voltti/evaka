// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { NextFunction, Request, Response } from 'express'
import { logAuditEvent } from '../logging'
import { gatewayRole } from '../config'
import { createJwt } from './jwt'
import { SamlUser } from '../routes/auth/saml/types'

const auditEventGatewayId =
  (gatewayRole === 'enduser' && 'eugw') ||
  (gatewayRole === 'internal' && 'ingw') ||
  (gatewayRole === undefined && 'devgw')

export function authenticate(req: Request, res: Response, next: NextFunction) {
  if (!req.user) {
    logAuditEvent(
      `evaka.${auditEventGatewayId}.auth.not_found`,
      req,
      'Could not find user'
    )
    res.sendStatus(401)
    return
  }
  if (!req.user.id) {
    logAuditEvent(
      `evaka.${auditEventGatewayId}.auth.not_found`,
      req,
      'Could not find id for user'
    )
    res.sendStatus(401)
    return
  }
  try {
    // FIXME do we need to verify session state
    return next()
  } catch (err) {
    logAuditEvent(
      `evaka.${auditEventGatewayId}.auth.jwt_verification_failed`,
      req,
      `JWT authentication error. Error: ${err}`
    )
    res.status(401).send(err)
  }
}

export function createAuthHeader(user: SamlUser): string {
  const token = createJwt({
    kind: gatewayRole === 'enduser' ? 'SuomiFI' : 'EspooAD',
    sub: user.id,
    scope: user.roles
      .map((role) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`))
      .join(' ')
  })
  return `Bearer ${token}`
}
