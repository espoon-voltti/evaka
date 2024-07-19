// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { CookieOptions } from 'express'
import { v4 as uuid } from 'uuid'

import { login } from '../shared/auth/index.js'
import { pinSessionTimeoutSeconds, useSecureCookies } from '../shared/config.js'
import { getDatabaseId } from '../shared/dev-api.js'
import {
  assertStringProp,
  toMiddleware,
  toRequestHandler
} from '../shared/express.js'
import { RedisClient } from '../shared/redis-client.js'
import {
  employeePinLogin,
  identifyMobileDevice,
  MobileDeviceIdentity,
  validatePairing
} from '../shared/service-client.js'

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
  req: express.Request,
  res: express.Response,
  device: MobileDeviceIdentity
) {
  await login(req, {
    id: device.id,
    globalRoles: [],
    allScopedRoles: ['MOBILE'],
    userType: 'MOBILE'
  })
  // Unconditionally refresh long-term cookie on each login to refresh expiry
  // time and make it a "rolling" cookie
  res.cookie(mobileLongTermCookieName, device.longTermToken, {
    ...mobileLongTermCookieOptions,
    maxAge: daysToMillis(90)
  })
}

export const refreshMobileSession = toMiddleware(async (req, res) => {
  if (!req.user) {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
    const token = req.signedCookies[mobileLongTermCookieName]
    if (token) {
      const deviceIdentity = await identifyMobileDevice(
        req,
        token,
        getDatabaseId(req)
      )
      if (deviceIdentity) {
        await mobileLogin(req, res, deviceIdentity)
      } else {
        // device has been removed or token has been rotated
        res.clearCookie(mobileLongTermCookieName, mobileLongTermCookieOptions)
      }
    }
  }
})

export default toRequestHandler(async (req, res) => {
  const id = assertStringProp(req.body, 'id')
  const challengeKey = assertStringProp(req.body, 'challengeKey')
  const responseKey = assertStringProp(req.body, 'responseKey')
  const deviceIdentity = await validatePairing(
    req,
    id,
    {
      challengeKey,
      responseKey
    },
    getDatabaseId(req)
  )
  await mobileLogin(req, res, deviceIdentity)
  res.sendStatus(204)
})

export const devApiE2ESignup = toRequestHandler(async (req, res) => {
  const token = assertStringProp(req.query, 'token')
  const deviceIdentity = await identifyMobileDevice(
    req,
    token,
    getDatabaseId(req)
  )
  if (deviceIdentity) {
    await mobileLogin(req, res, deviceIdentity)
    res.redirect('/employee/mobile')
  } else {
    res.sendStatus(404)
  }
})

const toMobileEmployeeIdKey = (token: string) => `mobile-employee-id:${token}`

export const pinLoginRequestHandler = (redisClient: RedisClient) =>
  toRequestHandler(async (req, res) => {
    if (req.user?.userType !== 'MOBILE') return

    const employeeId = assertStringProp(req.body, 'employeeId')
    const response = await employeePinLogin(req, getDatabaseId(req))

    const token = uuid()
    await redisClient.set(toMobileEmployeeIdKey(token), employeeId, {
      EX: pinSessionTimeoutSeconds
    })

    req.session.employeeIdToken = token
    res.status(200).send(response)
  })

export const pinLogoutRequestHandler = (redisClient: RedisClient) =>
  toRequestHandler(async (req, res) => {
    const token = req.session.employeeIdToken
    if (token) {
      await redisClient.del(toMobileEmployeeIdKey(token))
      req.session.employeeIdToken = undefined
      if (req.user) req.user.mobileEmployeeId = undefined
    }

    res.sendStatus(204)
  })

export const checkMobileEmployeeIdToken = (redisClient: RedisClient) =>
  toMiddleware(async (req) => {
    if (req.user?.userType !== 'MOBILE') return

    if (!req.session.employeeIdToken) {
      req.user.mobileEmployeeId = undefined
      return
    }

    const tokenKey = toMobileEmployeeIdKey(req.session.employeeIdToken)
    const employeeId = await redisClient.get(tokenKey)
    if (employeeId) {
      // refresh session
      await redisClient.expire(tokenKey, pinSessionTimeoutSeconds)
      req.user.mobileEmployeeId = employeeId
    } else {
      req.user.mobileEmployeeId = undefined
      req.session.employeeIdToken = undefined
    }
  })
