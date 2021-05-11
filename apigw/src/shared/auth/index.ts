// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { concat } from 'lodash'
import { NextFunction, Request, Response } from 'express'
import { logAuditEvent } from '../logging'
import { gatewayRole } from '../config'
import { createJwt } from './jwt'
import { SamlUser } from '../routes/auth/saml/types'
import { Profile, SAML } from 'passport-saml'
import { fromCallback } from '../promise-utils'

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

export function createLogoutToken(
  nameID: Required<Profile>['nameID'],
  sessionIndex: Profile['sessionIndex']
) {
  return `${nameID}:::${sessionIndex}`
}

/**
 * If request is a SAMLRequest, parse, validate and return the Profile from it.
 * @param saml Config must match active strategy's config
 */
export async function tryParseProfile(
  req: Request,
  saml: SAML
): Promise<Profile | undefined> {
  let profile: Profile | null | undefined

  // NOTE: This duplicate parsing can be removed if passport-saml ever exposes
  // an alternative for passport.authenticate() that either lets us hook into
  // it before any redirects or separate XML parsing and authentication methods.
  if (req.query?.SAMLRequest) {
    // Redirects have signatures in the original query parameter
    const dummyOrigin = 'http://evaka'
    const originalQuery = new URL(req.url, dummyOrigin).search.replace(
      /^\?/,
      ''
    )
    profile = await fromCallback<Profile | null | undefined>((cb) =>
      saml.validateRedirect(req.query, originalQuery, cb)
    )
  } else if (req.body?.SAMLRequest) {
    // POST logout callbacks have the signature in the message body directly
    profile = await fromCallback<Profile | null | undefined>((cb) =>
      saml.validatePostRequest(req.body, cb)
    )
  }

  return profile || undefined
}
