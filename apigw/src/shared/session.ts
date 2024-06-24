// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import RedisStore from 'connect-redis'
import { addMinutes } from 'date-fns/addMinutes'
import { differenceInMinutes } from 'date-fns/differenceInMinutes'
import { differenceInSeconds } from 'date-fns/differenceInSeconds'
import { isDate } from 'date-fns/isDate'
import express from 'express'
import session from 'express-session'

import { SessionConfig } from './config.js'
import { LogoutToken, toMiddleware } from './express.js'
import { fromCallback } from './promise-utils.js'
import { RedisClient } from './redis-client.js'

export type SessionType = 'enduser' | 'employee'

function cookiePrefix(sessionType: SessionType) {
  return sessionType === 'enduser' ? 'evaka.eugw' : 'evaka.employee'
}

function sessionKey(id: string) {
  return `sess:${id}`
}

function logoutKey(token: LogoutToken['value']) {
  return `slo:${token}`
}

export function sessionCookie(sessionType: SessionType) {
  return `${cookiePrefix(sessionType)}.session`
}

export interface Sessions {
  cookieName: string
  middleware: express.RequestHandler

  save(req: express.Request): Promise<void>

  saveLogoutToken(
    req: express.Request,
    logoutToken?: LogoutToken['value']
  ): Promise<void>
  logoutWithToken(token: LogoutToken['value']): Promise<unknown>
  consumeLogoutToken(token: LogoutToken['value']): Promise<void>
}

export function sessionSupport(
  sessionType: SessionType,
  redisClient: RedisClient,
  config: SessionConfig
): Sessions {
  const cookieName = sessionCookie(sessionType)

  // Base session support middleware from express-session
  const baseMiddleware = session({
    cookie: {
      path: '/',
      httpOnly: true,
      secure: config.useSecureCookies,
      sameSite: 'lax',
      maxAge: config.sessionTimeoutMinutes * 60000
    },
    resave: false,
    rolling: true,
    saveUninitialized: false,
    secret: config.cookieSecret,
    name: cookieName,
    store: new RedisStore({ client: redisClient })
  })

  const extraMiddleware = toMiddleware(async (req) => {
    // Touch maxAge to guarantee session is rolling (= doesn't expire as long as you are active)
    req.session?.touch()

    await refreshLogoutToken(req)
  })

  const middleware: express.RequestHandler = (req, res, next) => {
    baseMiddleware(req, res, (errOrDefer) => {
      if (errOrDefer) next(errOrDefer)
      else extraMiddleware(req, res, next)
    })
  }

  async function save(req: express.Request) {
    if (req.session) {
      const session = req.session
      await fromCallback((cb) => session.save(cb))
    }
  }

  /**
   * Save a logout token for a user session to be consumed during logout.
   * Provide the same token during logout to consume it.
   *
   * The token must be a value generated from values available in a logout request
   * without any cookies, as many browsers will disable 3rd party cookies by
   * default, breaking Single Logout. For example, SAML nameID and sessionIndex.
   *
   * The token is used as an effective secondary index in Redis for the session,
   * which would normally be recognized from the session cookie.
   *
   * This token can be removed if this passport-saml issue is ever fixed and this
   * logic is directly implemented in the library:
   * https://github.com/node-saml/passport-saml/issues/419
   */
  async function saveLogoutToken(
    req: express.Request,
    logoutToken?: LogoutToken['value']
  ): Promise<void> {
    if (!req.session) return

    const token = logoutToken || req.session.logoutToken?.value
    if (!token) return

    const now = new Date()
    const expires = addMinutes(now, config.sessionTimeoutMinutes + 60)
    const newToken = {
      expiresAt: expires.valueOf(),
      value: token
    }
    req.session.logoutToken = newToken

    const key = logoutKey(token)
    const ttlSeconds = differenceInSeconds(expires, now)
    // https://redis.io/commands/set - Set key to hold the string value.
    // Options:
    //   EX seconds -- Set the specified expire time, in seconds.
    await redisClient.set(key, req.session.id, { EX: ttlSeconds })
  }

  async function refreshLogoutToken(req: express.Request): Promise<void> {
    if (!req.session) return
    if (!req.session.logoutToken) return
    if (!isDate(req.session.cookie.expires)) return
    const sessionExpires = req.session.cookie.expires
    const logoutExpires = new Date(req.session.logoutToken.expiresAt)
    // Logout token should always expire at least 30 minutes later than the session
    if (differenceInMinutes(logoutExpires, sessionExpires) < 30) {
      await saveLogoutToken(req)
    }
  }

  async function logoutWithToken(
    logoutToken: LogoutToken['value']
  ): Promise<unknown> {
    if (!logoutToken) return
    const sid = await redisClient.get(logoutKey(logoutToken))
    if (!sid) return
    const session = await redisClient.get(sessionKey(sid))
    await redisClient.del([sessionKey(sid), logoutKey(logoutToken)])
    if (!session) return
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
    const user = JSON.parse(session)?.passport?.user
    if (!user) return
    // eslint-disable-next-line @typescript-eslint/no-unsafe-return
    return user
  }

  async function consumeLogoutToken(token: LogoutToken['value']) {
    // TODO: use Redis getdel operation once the client supports it
    const sid = await redisClient.get(logoutKey(token))
    if (sid) {
      // Ensure both session and logout keys are cleared in case no cookies were
      // available -> no req.session was available to be deleted.
      await redisClient.del([sessionKey(sid), logoutKey(token)])
    }
  }

  return {
    cookieName,
    middleware,
    save,
    saveLogoutToken,
    logoutWithToken,
    consumeLogoutToken
  }
}
