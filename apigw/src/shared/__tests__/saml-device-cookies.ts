// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'

import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import { ValidateInResponseTo } from '@node-saml/node-saml'
import type { AxiosResponse } from 'axios'
import { unsign } from 'cookie-signature'
import { Cookie } from 'tough-cookie'

import type { Config } from '../config.ts'
import { configFromEnv } from '../config.ts'
import { AUTH_HISTORY_COOKIE_PREFIX } from '../device-cookies.ts'
import { GatewayTester } from '../test/gateway-tester.ts'
import {
  buildLoginResponse,
  IDP_ENTRY_POINT_URL,
  SP_CALLBACK_URL,
  SP_ISSUER,
  SP_LOGIN_CALLBACK_ENDPOINT
} from '../test/saml-test-helpers.ts'

const mockUser = {
  id: '942b9cab-210d-4d49-b4c9-65f26390eed3'
}

// Use different secrets to verify the device cookie is signed with the
// citizen secret and not the employee secret
const CITIZEN_COOKIE_SECRET = 'citizen-secret-for-testing-device-cookies'
const EMPLOYEE_COOKIE_SECRET = 'employee-secret-for-testing-device-cookies'

function expectedUserHash() {
  return createHash('sha256').update(mockUser.id).digest('hex')
}

function extractDeviceCookieHeader(res: AxiosResponse): string {
  const setCookieHeaders: string[] = res.headers['set-cookie'] ?? []
  const header = setCookieHeaders.find((h) =>
    h.startsWith(AUTH_HISTORY_COOKIE_PREFIX)
  )
  if (!header) throw new Error('Device cookie header not found in response')
  return header
}

function extractSignedPayload(deviceCookieHeader: string): string {
  const cookie = Cookie.parse(deviceCookieHeader)
  if (!cookie) throw new Error('Failed to parse device cookie header')
  const decoded = decodeURIComponent(cookie.value)
  if (!decoded.startsWith('s:'))
    throw new Error('Cookie value is not signed (missing s: prefix)')
  return decoded.slice(2)
}

describe('SAML device cookies', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    const baseConfig = configFromEnv()
    const config: Config = {
      ...baseConfig,
      citizen: {
        ...baseConfig.citizen,
        cookieSecret: CITIZEN_COOKIE_SECRET
      },
      employee: {
        ...baseConfig.employee,
        cookieSecret: EMPLOYEE_COOKIE_SECRET
      },
      sfi: {
        type: 'saml',
        saml: {
          callbackUrl: SP_CALLBACK_URL,
          entryPoint: IDP_ENTRY_POINT_URL,
          logoutUrl: IDP_ENTRY_POINT_URL,
          issuer: SP_ISSUER,
          publicCert: 'config/test-cert/slo-test-idp-cert.pem',
          privateCert: 'config/test-cert/saml-private.pem',
          validateInResponseTo: ValidateInResponseTo.never,
          decryptAssertions: false,
          acceptedClockSkewMs: 0
        }
      }
    }
    tester = await GatewayTester.start(config, 'citizen')
  })
  afterEach(async () => {
    await tester?.afterEach()
    await tester?.stop()
  })

  async function performSamlLogin(): Promise<AxiosResponse> {
    const loginResponse = buildLoginResponse(
      'test@test.local',
      '_test_session',
      'testAuthnRequest'
    )
    tester.nockScope.post('/system/citizen-login').reply(200, mockUser)
    const res = await tester.client.post(
      SP_LOGIN_CALLBACK_ENDPOINT,
      new URLSearchParams({ SAMLResponse: loginResponse }),
      {
        maxRedirects: 0,
        validateStatus: () => true
      }
    )
    tester.nockScope.done()
    return res
  }

  test('citizen SFI login sets device cookie signed with citizen secret', async () => {
    const res = await performSamlLogin()
    expect(res.status).toBe(302)

    const signedPayload = extractSignedPayload(extractDeviceCookieHeader(res))
    expect(unsign(signedPayload, CITIZEN_COOKIE_SECRET)).toBe(
      expectedUserHash()
    )
    expect(unsign(signedPayload, EMPLOYEE_COOKIE_SECRET)).toBe(false)
  })

  test('device cookie name contains SHA256 hash of user ID', async () => {
    const res = await performSamlLogin()
    expect(res.status).toBe(302)

    const header = extractDeviceCookieHeader(res)
    const expectedName = `${AUTH_HISTORY_COOKIE_PREFIX}${expectedUserHash()}`
    expect(header).toMatch(new RegExp(`^${expectedName}=`))
  })

  test('device cookie has correct security attributes', async () => {
    const res = await performSamlLogin()
    expect(res.status).toBe(302)

    const header = extractDeviceCookieHeader(res)
    expect(header).toMatch(/;\s*Secure/i)
    expect(header).toMatch(/;\s*HttpOnly/i)
    expect(header).toMatch(/;\s*SameSite=Strict/i)

    const expiresMatch = header.match(/Expires=([^;]+)/)
    expect(expiresMatch).not.toBeNull()
    const expiresDate = new Date(expiresMatch![1])
    const ninetyDaysMs = 90 * 24 * 60 * 60 * 1000
    const diff = expiresDate.getTime() - Date.now()
    expect(diff).toBeGreaterThan(ninetyDaysMs - 60_000)
    expect(diff).toBeLessThan(ninetyDaysMs + 60_000)
  })

  test('weak login receives device history from previously set device cookie', async () => {
    // Step 1: SAML login sets the device cookie
    const samlRes = await performSamlLogin()
    expect(samlRes.status).toBe(302)

    // Step 2: Add the device cookie to the jar for subsequent requests.
    // Secure cookies aren't stored by tough-cookie over HTTP, so we strip
    // the flag to simulate a browser that already has the cookie.
    const deviceCookieHeader = extractDeviceCookieHeader(samlRes)
    const cookie = Cookie.parse(deviceCookieHeader)!
    cookie.secure = false
    await tester.setCookie(cookie)

    // Step 3: Weak login - capture the request body sent to the backend
    let capturedDeviceHistory: string[] | undefined
    tester.nockScope
      .post('/system/citizen-weak-login', (body: Record<string, unknown>) => {
        capturedDeviceHistory = body.deviceAuthHistory as string[]
        return true
      })
      .reply(200, mockUser)

    tester.setCsrfHeader = true
    const weakLoginRes = await tester.client.post(
      '/api/citizen/auth/weak-login',
      { username: 'test@example.com', password: 'password123' },
      { validateStatus: () => true }
    )
    tester.nockScope.done()
    expect(weakLoginRes.status).toBe(200)

    // The backend should receive the device history containing the user hash
    // from the cookie that was set during the SAML login
    expect(capturedDeviceHistory).toEqual([expectedUserHash()])
  })
})
