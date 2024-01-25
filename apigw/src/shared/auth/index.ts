// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import express, { NextFunction, Request, Response } from 'express'
import { logAuditEvent } from '../logging.js'
import { gatewayRole } from '../config.js'
import { createJwt } from './jwt.js'
import { Profile } from '@node-saml/passport-saml'
import { UserType } from '../service-client.js'
import passport, { AuthenticateCallback } from 'passport'
import { fromCallback } from '../promise-utils.js'
import { Sessions } from '../session.js'
import { randomBytes } from 'node:crypto'

const auditEventGatewayId =
  (gatewayRole === 'enduser' && 'eugw') ||
  (gatewayRole === 'internal' && 'ingw') ||
  (gatewayRole === undefined && 'apigw')

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
        user.roles ??
        _.concat(user.globalRoles ?? [], user.allScopedRoles ?? [])
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

export const authenticate = async (
  strategyName: string,
  req: express.Request,
  res: express.Response
): Promise<Express.User | undefined> =>
  await new Promise<Express.User | undefined>((resolve, reject) => {
    const cb: AuthenticateCallback = (err, user) =>
      err ? reject(err) : resolve(user || undefined)
    const next: express.NextFunction = (err) =>
      err ? reject(err) : resolve(undefined)
    passport.authenticate(strategyName, cb)(req, res, next)
  })

export const login = async (
  req: express.Request,
  user: Express.User
): Promise<void> => {
  await fromCallback<void>((cb) => req.logIn(user, cb))
  // Passport has now regenerated the active session and saved it, so we have a
  // guarantee that the session ID has changed and Redis has stored the new session data

  req.session.antiCsrfToken = (
    await fromCallback<Buffer>((cb) => randomBytes(32, cb))
  ).toString('base64')
}

export const logout = async (
  sessions: Sessions,
  req: express.Request,
  res: express.Response
): Promise<void> => {
  // Pre-emptively clear the cookie, so even if something fails later, we
  // will end up clearing the cookie in the response
  res.clearCookie(sessions.cookieName)

  const logoutToken = req.session?.logoutToken?.value

  await fromCallback<void>((cb) => req.logOut(cb))
  // Passport has now saved the previous session with null user and regenerated
  // the active session, so we have a guarantee that the ID has changed and
  // the old session data in Redis no longer includes the user

  if (logoutToken) {
    await sessions.consumeLogoutToken(logoutToken)
  }
  await fromCallback((cb) =>
    req.session ? req.session.destroy(cb) : cb(undefined)
  )
}
