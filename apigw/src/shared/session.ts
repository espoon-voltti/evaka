// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { CookieOptions } from 'express'
import session from 'express-session'
import connectRedis from 'connect-redis'
import {
  cookieSecret,
  nodeEnv,
  redisDisableSecurity,
  redisHost,
  redisPassword,
  redisPort,
  redisTlsServerName,
  sessionTimeoutMinutes,
  useSecureCookies
} from './config'
import redis from 'redis'
import { v4 as uuidv4 } from 'uuid'
import { logError } from './logging'
import {
  addMinutes,
  differenceInMinutes,
  differenceInSeconds,
  isDate
} from 'date-fns'
import { toMiddleware } from './express'
import AsyncRedisClient from './async-redis-client'
import { fromCallback } from './promise-utils'

export type SessionType = 'enduser' | 'employee'

const RedisStore = connectRedis(session)

const redisClient =
  nodeEnv !== 'test'
    ? redis.createClient({
        host: redisHost,
        port: redisPort,
        ...(!redisDisableSecurity && {
          tls: { servername: redisTlsServerName },
          password: redisPassword
        })
      })
    : undefined

if (redisClient) {
  redisClient.on('error', (err) =>
    logError('Redis error', undefined, undefined, err)
  )

  // don't prevent the app from exiting if a redis connection is alive
  redisClient.unref()
}

const asyncRedisClient = redisClient && new AsyncRedisClient(redisClient)

const sessionCookieOptions: CookieOptions = {
  path: '/',
  httpOnly: true,
  secure: useSecureCookies,
  sameSite: 'lax'
}

const logoutCookieOptions: express.CookieOptions = {
  path: '/',
  httpOnly: true,
  secure: useSecureCookies,
  sameSite: useSecureCookies ? 'none' : undefined
}

function cookiePrefix(sessionType: SessionType) {
  return sessionType === 'enduser' ? 'evaka.eugw' : 'evaka.employee'
}

function logoutCookie(sessionType: SessionType) {
  return `${cookiePrefix(sessionType)}.logout`
}

export function sessionCookie(sessionType: SessionType) {
  return `${cookiePrefix(sessionType)}.session`
}

export function refreshLogoutToken(sessionType: SessionType) {
  return toMiddleware(async (req, res) => {
    if (!req.session) return
    if (!req.session.logoutToken) return
    if (!isDate(req.session.cookie.expires)) return
    const sessionExpires = req.session.cookie.expires as Date
    const logoutExpires = new Date(req.session.logoutToken.expiresAt)
    const cookieToken = req.cookies && req.cookies[logoutCookie(sessionType)]
    // Logout token should always expire at least 30 minutes later than the session
    if (
      differenceInMinutes(logoutExpires, sessionExpires) < 30 ||
      cookieToken !== req.session.logoutToken.value
    ) {
      await saveLogoutToken(req, res, sessionType)
    }
  })
}

export async function saveLogoutToken(
  req: express.Request,
  res: express.Response,
  sessionType: SessionType
): Promise<void> {
  if (!req.session) return
  const now = new Date()
  const expires = addMinutes(now, sessionTimeoutMinutes + 60)
  const logoutToken = {
    expiresAt: expires.valueOf(),
    value: req.session.logoutToken ? req.session.logoutToken.value : uuidv4()
  }
  req.session.logoutToken = logoutToken
  res.cookie(logoutCookie(sessionType), logoutToken.value, {
    ...logoutCookieOptions,
    expires
  })
  if (!asyncRedisClient) return
  const key = `logout:${logoutToken.value}`
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

// If a logout token is available, delete it and delete the session it points to
export async function consumeLogoutToken(
  req: express.Request,
  res: express.Response,
  sessionType: SessionType
): Promise<void> {
  const token = req.cookies && req.cookies[logoutCookie(sessionType)]
  if (!token || !asyncRedisClient) return
  res.clearCookie(logoutCookie(sessionType), logoutCookieOptions)
  const sid = await asyncRedisClient.get(`logout:${token}`)
  if (sid) {
    await asyncRedisClient.del(`sess:${sid}`, `logout:${token}`)
  }
}

export async function logoutExpress(
  req: express.Request,
  res: express.Response,
  sessionType: SessionType
) {
  req.logout()
  if (req.session) {
    const session = req.session
    await fromCallback((cb) => session.destroy(cb))
  }
  res.clearCookie(sessionCookie(sessionType))
  await consumeLogoutToken(req, res, sessionType)
}

export default (sessionType: SessionType) =>
  session({
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
