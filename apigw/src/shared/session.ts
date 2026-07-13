// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RedisStore } from 'connect-redis'
import { signedCookie } from 'cookie-parser'
import { addMinutes } from 'date-fns/addMinutes'
import { differenceInMinutes } from 'date-fns/differenceInMinutes'
import { differenceInSeconds } from 'date-fns/differenceInSeconds'
import { isDate } from 'date-fns/isDate'
import type express from 'express'
import session from 'express-session'

import type { EvakaSessionUser } from './auth/index.ts'
import { createLogoutToken, createUserHeader } from './auth/index.ts'
import type { SessionConfig } from './config.ts'
import { createSha256Hash } from './crypto.ts'
import type { LogoutToken } from './express.ts'
import { toMiddleware } from './express.ts'
import { logAuditEvent, logDebug } from './logging.ts'
import { fromCallback } from './promise-utils.ts'
import type { RedisClient } from './redis-client.ts'
import type { SamlSession } from './saml/index.ts'

export type SessionType = 'citizen' | 'employee' | 'employee-mobile'

export type LoginCorrelation = { ownSid?: string; secondarySid?: string }

function cookiePrefix(sessionType: SessionType) {
  switch (sessionType) {
    case 'citizen':
      return 'evaka.eugw'
    case 'employee':
      return 'evaka.employee'
    case 'employee-mobile':
      return 'evaka.employee'
  }
}

function sessionKey(id: string) {
  return `sess:${id}`
}

function logoutKey(token: LogoutToken['value']) {
  return `slo:${token}`
}

function userSessionsKey(userIdHash: string) {
  return `usess:${userIdHash}`
}

function secondarySfiSessionKey(sfiSessionId: string) {
  return `sfisess:${sfiSessionId}`
}

function sfiLoginKey(sfiSessionId: string) {
  return `sfilogin:${sfiSessionId}`
}

function sfiSloKey(logoutToken: LogoutToken['value']) {
  return `sfislo:${logoutToken}`
}

function loginCorrelationKey(token: string) {
  return `sficorr:${token}`
}

export function sessionCookie(sessionType: SessionType) {
  return `${cookiePrefix(sessionType)}.session`
}

export interface Sessions<T extends SessionType> {
  sessionType: T
  cookieName: string
  middleware: express.RequestHandler
  requireAuthentication: express.RequestHandler

  save(req: express.Request): Promise<void>

  saveLogoutToken(
    req: express.Request,
    logoutToken?: LogoutToken['value']
  ): Promise<void>
  saveRejectedSfiLogin(
    linkedSessionIds: string[],
    samlSession: SamlSession
  ): Promise<void>
  saveLoginCorrelation(
    token: string,
    correlation: LoginCorrelation
  ): Promise<void>
  consumeLoginCorrelation(token: string): Promise<LoginCorrelation>
  logoutWithToken(token: LogoutToken['value']): Promise<unknown>

  login(
    req: express.Request,
    user: EvakaSessionUser,
    secondarySessionId?: string
  ): Promise<void>
  destroy(req: express.Request, res: express.Response): Promise<void>

  updateUser(req: express.Request, user: EvakaSessionUser): Promise<void>
  getUser(req: express.Request): EvakaSessionUser | undefined
  getSecondaryUserIfNewer(
    req: express.Request
  ): Promise<EvakaSessionUser | SamlSession | undefined>
  getUserHeader(req: express.Request): string | undefined
  isAuthenticated(req: express.Request): boolean
  sessionIdFromCookie(rawCookie: string | undefined): string | undefined
}

// Only needs to bridge login-start -> login-callback while a human completes the
// IdP login; independent of the 32-min rejected-login TTL.
const LOGIN_CORRELATION_TTL_SECONDS = 15 * 60

export function sessionSupport<T extends SessionType>(
  sessionType: T,
  redisClient: RedisClient,
  config: SessionConfig,
  maxSessionTimeoutMinutes?: number
): Sessions<T> {
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
    req.user = getUser(req)

    await refreshLogoutToken(req)
  })

  const middleware: express.RequestHandler = (req, res, next) => {
    baseMiddleware(req, res, (errOrDefer) => {
      if (errOrDefer) next(errOrDefer)
      else extraMiddleware(req, res, next)
    })
  }

  const requireAuthentication: express.RequestHandler = (req, res, next) => {
    const user = getUser(req)
    if (!user || !user.id) {
      logAuditEvent(`evaka.apigw.auth.not_found`, req, 'Could not find user')
      res.sendStatus(401)
      return
    }
    return next()
  }

  async function save(req: express.Request) {
    const session = req.session
    if (session) {
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

  async function saveRejectedSfiLogin(
    linkedSessionIds: string[],
    samlSession: SamlSession
  ): Promise<void> {
    if (!maxSessionTimeoutMinutes) return
    const ids: string[] = []
    for (const id of linkedSessionIds) {
      if (id && (await isSfiSession(id))) ids.push(id)
    }
    if (ids.length === 0) return

    const ttlSeconds = maxSessionTimeoutMinutes * 60
    await redisClient.set(
      sfiSloKey(createLogoutToken(samlSession)),
      JSON.stringify({ samlSession, sessionIds: ids }),
      { EX: ttlSeconds }
    )
    const record = JSON.stringify({ samlSession, createdAt: Date.now() })
    for (const id of ids) {
      await redisClient.set(sfiLoginKey(id), record, {
        EX: ttlSeconds
      })
    }
  }

  async function saveLoginCorrelation(
    token: string,
    correlation: LoginCorrelation
  ): Promise<void> {
    await redisClient.set(
      loginCorrelationKey(token),
      JSON.stringify(correlation),
      { EX: LOGIN_CORRELATION_TTL_SECONDS }
    )
  }

  async function consumeLoginCorrelation(
    token: string
  ): Promise<LoginCorrelation> {
    const key = loginCorrelationKey(token)
    const raw = await redisClient.get(key)
    await redisClient.del(key)
    if (!raw) return {}
    // oxlint-disable-next-line typescript/no-unsafe-assignment
    const parsed = JSON.parse(raw)
    return {
      // oxlint-disable-next-line typescript/no-unsafe-assignment,typescript/no-unsafe-member-access
      ownSid: typeof parsed?.ownSid === 'string' ? parsed.ownSid : undefined,
      // oxlint-disable-next-line typescript/no-unsafe-assignment
      secondarySid:
        // oxlint-disable-next-line typescript/no-unsafe-member-access
        typeof parsed?.secondarySid === 'string'
          ? // oxlint-disable-next-line typescript/no-unsafe-member-access
            parsed.secondarySid
          : undefined
    }
  }

  function storedLogoutToken(rawSession: string): string | undefined {
    // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
    const value = JSON.parse(rawSession)?.logoutToken?.value
    return typeof value === 'string' ? value : undefined
  }

  function storedUser(rawSession: string): unknown {
    // oxlint-disable-next-line typescript/no-unsafe-member-access
    return JSON.parse(rawSession)?.passport?.user ?? undefined
  }

  function authTypeOf(user: unknown): string | undefined {
    if (typeof user !== 'object' || user === null) return undefined
    const authType = (user as Record<string, unknown>).authType
    return typeof authType === 'string' ? authType : undefined
  }

  async function isSfiSession(sessionId: string): Promise<boolean> {
    const session = await redisClient.get(sessionKey(sessionId))
    return !!session && authTypeOf(storedUser(session)) === 'sfi'
  }

  async function deleteSessionById(sessionId: string): Promise<void> {
    const session = await redisClient.get(sessionKey(sessionId))
    const keys = [sessionKey(sessionId), sfiLoginKey(sessionId)]
    const token = session ? storedLogoutToken(session) : undefined
    if (token) keys.push(logoutKey(token))
    await redisClient.del(keys)
  }

  async function destroyLinkedSfiSession(sessionId: string): Promise<void> {
    const secondarySessionId = await redisClient.get(
      secondarySfiSessionKey(sessionId)
    )
    if (!secondarySessionId) return
    await deleteSessionById(secondarySessionId)
    await redisClient.del([
      secondarySfiSessionKey(sessionId),
      secondarySfiSessionKey(secondarySessionId)
    ])
  }

  async function logoutWithRejectedSfiLogin(
    logoutToken: LogoutToken['value']
  ): Promise<unknown> {
    const key = sfiSloKey(logoutToken)
    const raw = await redisClient.get(key)
    if (!raw) return
    // oxlint-disable-next-line typescript/no-unsafe-assignment
    const rejectedLogin = JSON.parse(raw)
    // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
    const samlSession = rejectedLogin?.samlSession
    // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
    const sessionIds = rejectedLogin?.sessionIds
    if (!samlSession || !Array.isArray(sessionIds)) {
      await redisClient.del(key)
      return
    }

    let loggedOut = false
    for (const sessionId of sessionIds) {
      if (typeof sessionId !== 'string') continue
      const alive = !!(await redisClient.get(sessionKey(sessionId)))
      await deleteSessionById(sessionId)
      if (!alive) continue
      await destroyLinkedSfiSession(sessionId)
      loggedOut = true
    }
    await redisClient.del(key)
    return loggedOut ? samlSession : undefined
  }

  async function logoutWithToken(
    logoutToken: LogoutToken['value']
  ): Promise<unknown> {
    if (!logoutToken) return
    const sid = await redisClient.get(logoutKey(logoutToken))
    if (!sid) return await logoutWithRejectedSfiLogin(logoutToken)
    const session = await redisClient.get(sessionKey(sid))
    if (!session) {
      await redisClient.del(logoutKey(logoutToken))
      return
    }
    const user = storedUser(session)
    await deleteSessionById(sid)
    if (!user) return

    if (authTypeOf(user) === 'sfi') await destroyLinkedSfiSession(sid)

    return user
  }

  async function login(
    req: express.Request,
    user: EvakaSessionUser,
    secondarySessionId?: string
  ): Promise<void> {
    await fromCallback<void>((cb) => req.session.regenerate(cb))
    // express-session has now regenerated the active session, so the ID has changed, and it's empty
    await saveUser(
      req,
      {
        ...user,
        // spread saml session fields for backwards compatibility
        ...(user.authType === 'sfi' || user.authType === 'ad'
          ? user.samlSession
          : undefined)
      },
      secondarySessionId
    )
  }

  async function destroy(req: express.Request, res: express.Response) {
    logDebug('Destroying session', req)
    const logoutToken = req.session?.logoutToken?.value
    const sessionId = req.session?.id

    if (req.session) {
      await fromCallback((cb) => req.session.destroy(cb))
      res.clearCookie(cookieName)
    }

    if (logoutToken) {
      // TODO: use Redis getdel operation once the client supports it
      const sid = await redisClient.get(logoutKey(logoutToken))
      if (sid) {
        // Ensure both session and logout keys are cleared in case no cookies were
        // available -> no req.session was available to be deleted.
        await redisClient.del([sessionKey(sid), logoutKey(logoutToken)])
      }
    }

    if (req.user?.authType === 'sfi' && sessionId) {
      await redisClient.del(sfiLoginKey(sessionId))
      await destroyLinkedSfiSession(sessionId)
    }
  }

  async function updateUser(
    req: express.Request,
    user: EvakaSessionUser
  ): Promise<void> {
    if (!req.session)
      throw new Error("Can't update user without an existing session")
    await saveUser(req, user)
  }

  async function saveUser(
    req: express.Request,
    user: EvakaSessionUser,
    secondarySessionId?: string
  ) {
    const userIdHash = createSha256Hash(user.id)

    req.session.passport = { user }
    req.session.evaka = {
      user,
      userIdHash: userIdHash
    }
    await save(req)
    req.user = user

    if (req.session.id && user.authType === 'citizen-weak') {
      const key = userSessionsKey(userIdHash)
      await redisClient
        .multi()
        .sAdd(key, req.session.id)
        .expire(key, config.sessionTimeoutMinutes * 60)
        .exec()
    }

    if (
      req.session.id &&
      user.authType === 'sfi' &&
      maxSessionTimeoutMinutes &&
      secondarySessionId &&
      (await isSfiSession(secondarySessionId))
    ) {
      await redisClient.set(
        secondarySfiSessionKey(req.session.id),
        secondarySessionId,
        {
          EX: maxSessionTimeoutMinutes * 60
        }
      )
      await redisClient.set(
        secondarySfiSessionKey(secondarySessionId),
        req.session.id,
        {
          EX: maxSessionTimeoutMinutes * 60
        }
      )
    }
  }

  function getUser(req: express.Request): EvakaSessionUser | undefined {
    return req.session?.passport?.user ?? undefined
  }

  async function getSecondaryUserIfNewer(
    req: express.Request
  ): Promise<EvakaSessionUser | SamlSession | undefined> {
    const primaryUser = req.session?.evaka?.user
    if (!primaryUser || primaryUser.authType !== 'sfi') return undefined

    const primarySessionId = req.session.id
    const primaryUserCreatedAt = primaryUser.createdAt

    let newest:
      | { value: EvakaSessionUser | SamlSession; createdAt: number }
      | undefined

    // Candidate 1: the real other-side session via the bidirectional sfisess: link
    // written by a successful co-resident login.
    const secondarySessionId = await redisClient.get(
      secondarySfiSessionKey(primarySessionId)
    )
    if (secondarySessionId) {
      const secondarySession = await redisClient.get(
        sessionKey(secondarySessionId)
      )
      if (secondarySession) {
        // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
        const user = JSON.parse(secondarySession)?.evaka?.user
        // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
        const createdAt = user?.createdAt
        if (typeof createdAt === 'number')
          // oxlint-disable-next-line typescript/no-unsafe-assignment
          newest = { value: user, createdAt }
      }
    }

    // Candidate 2: a validated-but-rejected co-resident login, which has no
    // eVaka session of its own.
    const raw = await redisClient.get(sfiLoginKey(primarySessionId))
    if (raw) {
      // oxlint-disable-next-line typescript/no-unsafe-assignment
      const rejectedLogin = JSON.parse(raw)
      // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
      const createdAt = rejectedLogin?.createdAt
      // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
      const samlSession = rejectedLogin?.samlSession
      if (
        typeof createdAt === 'number' &&
        samlSession &&
        (!newest || createdAt > newest.createdAt)
      )
        // oxlint-disable-next-line typescript/no-unsafe-assignment
        newest = { value: samlSession, createdAt }
    }

    if (!newest) return undefined
    return newest.createdAt > primaryUserCreatedAt ? newest.value : undefined
  }

  function getUserHeader(req: express.Request): string | undefined {
    const user = getUser(req)
    return user ? createUserHeader(user) : undefined
  }

  function isAuthenticated(req: express.Request): boolean {
    return !!req.session?.passport?.user
  }

  function sessionIdFromCookie(
    rawCookie: string | undefined
  ): string | undefined {
    if (!rawCookie) return undefined
    return signedCookie(rawCookie, config.cookieSecret) || undefined
  }

  return {
    sessionType,
    cookieName,
    middleware,
    requireAuthentication,
    save,
    saveLogoutToken,
    saveRejectedSfiLogin,
    saveLoginCorrelation,
    consumeLoginCorrelation,
    logoutWithToken,
    login,
    destroy,
    updateUser,
    getUser,
    getSecondaryUserIfNewer,
    getUserHeader,
    isAuthenticated,
    sessionIdFromCookie
  }
}
