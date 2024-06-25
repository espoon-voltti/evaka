// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import passportSaml from '@node-saml/passport-saml'
import type express from 'express'
import { BaseError } from 'make-error-cause'

import { EvakaSessionUser } from './auth/index.js'

export interface LogoutToken {
  // milliseconds value of a Date. Not an actual Date because it will be JSONified
  expiresAt: number
  value: string
}

export type AsyncRequestHandler = (
  req: express.Request,
  res: express.Response
) => Promise<void>

// A middleware calls next() on success, and next(err) on failure
export function toMiddleware(f: AsyncRequestHandler): express.RequestHandler {
  return (req, res, next) => {
    f(req, res)
      .then(() => next())
      .catch(next)
  }
}

// A request handler calls nothing on success, and next(err) on failure
export function toRequestHandler(
  f: AsyncRequestHandler
): express.RequestHandler {
  return (req, res, next) => {
    f(req, res).catch(next)
  }
}

export function assertStringProp<T, K extends keyof T>(
  object: T,
  property: K
): string {
  const value = object[property]
  if (typeof value !== 'string') {
    throw new InvalidRequest(
      `Expected '${String(property)}' to be string, but it is ${String(value)}`
    )
  }
  return value
}

export class InvalidRequest extends BaseError {}

// Use TS interface merging to add fields to express req.session
declare module 'express-session' {
  interface SessionData {
    antiCsrfToken?: string
    idpProvider?: string | null
    logoutToken?: LogoutToken
    employeeIdToken?: string
  }
}

// Use TS interface merging to add fields to express req and req.user.
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      traceId?: string
      spanId?: string
      samlLogoutRequest: passportSaml.Profile
    }
    // eslint-disable-next-line @typescript-eslint/no-empty-interface
    interface User extends EvakaSessionUser {}
  }
}
