// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import connectRedis from 'connect-redis'
import {
  addMinutes,
  differenceInMinutes,
  differenceInSeconds,
  isDate
} from 'date-fns'
import express, { CookieOptions } from 'express'
import session from 'express-session'
import { RedisClient } from 'redis'
import AsyncRedisClient from './async-redis-client'
import { cookieSecret, sessionTimeoutMinutes, useSecureCookies } from './config'
import { LogoutToken, toMiddleware } from './express'
import { logDebug } from './logging'
import { fromCallback } from './promise-utils'

export type SessionType = 'enduser' | 'employee'

const RedisStore = connectRedis(session)

let asyncRedisClient: AsyncRedisClient | undefined

const sessionCookieOptions: CookieOptions = {
  path: '/',
  httpOnly: true,
  secure: useSecureCookies,
  sameSite: 'lax'
}

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

export function refreshLogoutToken() {
  return toMiddleware(async (req) => {
    if (!req.session) return
    if (!req.session.logoutToken) return
    if (!isDate(req.session.cookie.expires)) return
    const sessionExpires = req.session.cookie.expires as Date
    const logoutExpires = new Date(req.session.logoutToken.expiresAt)
    // Logout token should always expire at least 30 minutes later than the session
    if (differenceInMinutes(logoutExpires, sessionExpires) < 30) {
      await saveLogoutToken(req, req.session.idpProvider)
    }
  })
}

/**
 * Save a logout token for a user session to be consumed during logout.
 * Provide the same token during logout to logoutExpress to consume it.
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
export async function saveLogoutToken(
  req: express.Request,
  strategyName: string | null | undefined,
  logoutToken?: string
): Promise<void> {
  if (!req.session) return

  const token = logoutToken || req.session.logoutToken?.value
  if (!token) return

  // Persist in session to allow custom logic per strategy
  req.session.idpProvider = strategyName

  if (!asyncRedisClient) return
  const now = new Date()
  const expires = addMinutes(now, sessionTimeoutMinutes + 60)
  const newToken = {
    expiresAt: expires.valueOf(),
    value: token
  }
  req.session.logoutToken = newToken

  const key = logoutKey(token)
  const ttlSeconds = differenceInSeconds(expires, now)
  // https://redis.io/commands/expire - Set a timeout on key
  // Return value:
  //   1 if the timeout was set
  //   0 if key does not exist.
  const ret = await asyncRedisClient.expire(key, ttlSeconds)
  if (ret === 1) return
  // https://redis.io/commands/set - Set key to hold the string value.
  // Options:
  //   EX seconds -- Set the specified expire time, in seconds.
  await asyncRedisClient.set(key, req.session.id, 'EX', ttlSeconds)
}

async function consumeLogoutToken(
  req: express.Request,
  logoutToken?: LogoutToken['value']
): Promise<void> {
  // Prefer an explicit token from e.g. a SAMLRequest but fall back to the
  // logoutToken from the session in case
  // a) this wasn't a SAMLRequest
  // b) it's malformed
  // to ensure the logout token is deleted from the store even in non-SLO cases.
  const token = logoutToken || req.session?.logoutToken?.value
  if (!token) {
    logDebug("Can't consume logout request without a logout token, ignoring")
    return
  }

  if (!asyncRedisClient) return
  const key = logoutKey(token)
  const sid = await asyncRedisClient.get(key)
  if (sid) {
    // Ensure both session and logout keys are cleared in case no cookies were
    // available -> no req.session was available to be deleted.
    await asyncRedisClient.del(sessionKey(sid), key)
  }
}

export async function logoutExpress(
  req: express.Request,
  res: express.Response,
  sessionType: SessionType,
  logoutToken?: LogoutToken['value']
) {
  req.logout()
  await consumeLogoutToken(req, logoutToken)
  if (req.session) {
    const session = req.session
    await fromCallback((cb) => session.destroy(cb))
  }
  res.clearCookie(sessionCookie(sessionType))
}

export default (sessionType: SessionType, redisClient?: RedisClient) => {
  asyncRedisClient = redisClient && new AsyncRedisClient(redisClient)
  return session({
    cookie: {
      ...sessionCookieOptions,
      maxAge: sessionTimeoutMinutes * 60000
    },
    resave: false,
    rolling: true,
    saveUninitialized: false,
    secret: cookieSecret,
    name: sessionCookie(sessionType),
    store: redisClient
      ? new RedisStore({
          client: redisClient
        })
      : new session.MemoryStore()
  })
}
