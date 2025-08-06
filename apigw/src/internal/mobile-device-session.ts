// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CookieOptions } from 'express'
import type express from 'express'
import { v4 as uuid } from 'uuid'

import { pinSessionTimeoutSeconds, useSecureCookies } from '../shared/config.ts'
import {
  assertStringProp,
  toMiddleware,
  toRequestHandler
} from '../shared/express.ts'
import type { RedisClient } from '../shared/redis-client.ts'
import type { MobileDeviceIdentity } from '../shared/service-client.ts'
import {
  employeePinLogin,
  identifyMobileDevice,
  validatePairing
} from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

export const mobileLongTermCookieName = 'evaka.employee.mobile'
const mobileLongTermCookieOptions: CookieOptions = {
  path: '/',
  httpOnly: true,
  secure: useSecureCookies,
  sameSite: 'lax',
  signed: true
}

function daysToMillis(days: number): number {
  return days * 24 * 60 * 60_000
}

async function mobileLogin(
  sessions: Sessions<'employee-mobile'>,
  req: express.Request,
  res: express.Response,
  device: MobileDeviceIdentity
) {
  await sessions.login(req, {
    id: device.id,
    authType: 'employee-mobile',
    userType: 'MOBILE'
  })
  // Unconditionally refresh long-term cookie on each login to refresh expiry
  // time and make it a "rolling" cookie
  res.cookie(mobileLongTermCookieName, device.longTermToken, {
    ...mobileLongTermCookieOptions,
    maxAge: daysToMillis(90)
  })
}

export const refreshMobileSession = (sessions: Sessions<'employee-mobile'>) =>
  toMiddleware(async (req, res) => {
    const user = sessions.getUser(req)
    if (!user) {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      const token = req.signedCookies[mobileLongTermCookieName]
      if (token) {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
        const deviceIdentity = await identifyMobileDevice(req, token)
        if (deviceIdentity) {
          await mobileLogin(sessions, req, res, deviceIdentity)
        } else {
          // device has been removed or token has been rotated
          res.clearCookie(mobileLongTermCookieName, mobileLongTermCookieOptions)
        }
      }
    }
  })

export const finishPairing = (sessions: Sessions<'employee-mobile'>) =>
  toRequestHandler(async (req, res) => {
    const id = assertStringProp(req.body, 'id')
    const challengeKey = assertStringProp(req.body, 'challengeKey')
    const responseKey = assertStringProp(req.body, 'responseKey')
    const deviceIdentity = await validatePairing(req, id, {
      challengeKey,
      responseKey
    })
    await mobileLogin(sessions, req, res, deviceIdentity)
    res.sendStatus(204)
  })

export const devApiE2ESignup = (sessions: Sessions<'employee-mobile'>) =>
  toRequestHandler(async (req, res) => {
    const token = assertStringProp(req.query, 'token')
    const deviceIdentity = await identifyMobileDevice(req, token)
    if (deviceIdentity) {
      await mobileLogin(sessions, req, res, deviceIdentity)
      res.redirect('/employee/mobile')
    } else {
      res.sendStatus(404)
    }
  })

const toMobileEmployeeIdKey = (token: string) => `mobile-employee-id:${token}`

export const pinLoginRequestHandler = (
  sessions: Sessions<'employee-mobile'>,
  redisClient: RedisClient
) =>
  toRequestHandler(async (req, res) => {
    const user = sessions.getUser(req)
    if (user?.userType !== 'MOBILE') return

    const employeeId = assertStringProp(req.body, 'employeeId')
    const response = await employeePinLogin(req)

    if (response.status === 'SUCCESS') {
      const token = uuid()
      await redisClient.set(toMobileEmployeeIdKey(token), employeeId, {
        EX: pinSessionTimeoutSeconds
      })
      req.session.employeeIdToken = token
    }

    res.status(200).send(response)
  })

export const pinLogoutRequestHandler = (
  sessions: Sessions<'employee-mobile'>,
  redisClient: RedisClient
) =>
  toRequestHandler(async (req, res) => {
    const token = req.session.employeeIdToken
    const user = sessions.getUser(req)
    if (token) {
      await redisClient.del(toMobileEmployeeIdKey(token))
      req.session.employeeIdToken = undefined
      if (user && user.userType === 'MOBILE') user.mobileEmployeeId = undefined
    }

    res.sendStatus(204)
  })

export const checkMobileEmployeeIdToken = (
  sessions: Sessions<'employee-mobile'>,
  redisClient: RedisClient
) =>
  toMiddleware(async (req) => {
    const user = sessions.getUser(req)
    if (user?.userType !== 'MOBILE') return

    if (!req.session.employeeIdToken) {
      user.mobileEmployeeId = undefined
      return
    }

    const tokenKey = toMobileEmployeeIdKey(req.session.employeeIdToken)
    const employeeId = await redisClient.get(tokenKey)
    if (employeeId) {
      // refresh session
      await redisClient.expire(tokenKey, pinSessionTimeoutSeconds)
      user.mobileEmployeeId = employeeId
    } else {
      user.mobileEmployeeId = undefined
      req.session.employeeIdToken = undefined
    }
  })
