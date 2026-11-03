// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { inflateRawSync } from 'node:zlib'

import { SAML, ValidateInResponseTo } from '@node-saml/node-saml'
import axios from 'axios'
import { sign, unsign } from 'cookie-signature'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import type { Config } from '../config.ts'
import { configFromEnv } from '../config.ts'
import { logAuditEvent, logError } from '../logging.ts'
import { checkNonceCookie, setNonceCookie } from '../saml/nonce-cookie.ts'
import { GatewayTester } from '../test/gateway-tester.ts'
import {
  buildLoginResponse,
  IDP_ENTRY_POINT_URL,
  SP_DOMAIN,
  SP_ISSUER,
  SP_LOGIN_CALLBACK_ENDPOINT,
  SP_LOGIN_CALLBACK_URL
} from '../test/saml-test-helpers.ts'

vi.mock('../logging.ts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../logging.ts')>()
  return {
    ...actual,
    logAuditEvent: vi.fn(actual.logAuditEvent),
    logError: vi.fn(actual.logError)
  }
})

vi.mock('../saml/nonce-cookie.ts', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../saml/nonce-cookie.ts')>()
  return {
    ...actual,
    checkNonceCookie: vi.fn(actual.checkNonceCookie),
    setNonceCookie: vi.fn(actual.setNonceCookie)
  }
})

const CITIZEN_COOKIE_SECRET = 'citizen-secret-for-testing-nonce-cookies'
const EMPLOYEE_COOKIE_SECRET = 'employee-secret-for-testing-nonce-cookies'
const CITIZEN_NONCE_COOKIE = 'evaka.saml.nonce.suomifi.citizen'
const AD_LOGIN_CALLBACK_ENDPOINT = '/api/employee/auth/ad/login/callback'

const mockUser = { id: '942b9cab-210d-4d49-b4c9-65f26390eed3' }

const samlConfig = (callbackUrl: string) => ({
  callbackUrl,
  entryPoint: IDP_ENTRY_POINT_URL,
  logoutUrl: IDP_ENTRY_POINT_URL,
  issuer: SP_ISSUER,
  publicCert: 'config/test-cert/slo-test-idp-cert.pem',
  privateCert: 'config/test-cert/saml-private.pem',
  validateInResponseTo: ValidateInResponseTo.never,
  decryptAssertions: false,
  acceptedClockSkewMs: 0
})

describe('SAML login nonce cookie', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    const baseConfig = configFromEnv()
    const config: Config = {
      ...baseConfig,
      citizen: { ...baseConfig.citizen, cookieSecret: CITIZEN_COOKIE_SECRET },
      employee: {
        ...baseConfig.employee,
        cookieSecret: EMPLOYEE_COOKIE_SECRET
      },
      ad: {
        type: 'saml',
        externalIdPrefix: 'test',
        userIdKey: 'userId',
        saml: samlConfig(`${SP_DOMAIN}${AD_LOGIN_CALLBACK_ENDPOINT}`)
      },
      sfi: { type: 'saml', saml: samlConfig(SP_LOGIN_CALLBACK_URL) }
    }
    tester = await GatewayTester.start(config, 'citizen')
  })
  afterEach(async () => {
    vi.mocked(logAuditEvent).mockClear()
    vi.mocked(logError).mockClear()
    vi.mocked(checkNonceCookie).mockClear()
    vi.mocked(setNonceCookie).mockClear()
    await tester?.afterEach()
    await tester?.stop()
  })

  const cookiePair = (setCookie: string) => setCookie.split(';')[0]
  const cookieName = (setCookie: string) => cookiePair(setCookie).split('=')[0]
  const cookieValue = (setCookie: string) =>
    decodeURIComponent(cookiePair(setCookie).split('=')[1])
  const escape = (s: string) => s.replaceAll('.', '\\.')

  async function startLogin(
    loginPath = '/api/citizen/auth/sfi/login',
    noncePrefix = CITIZEN_NONCE_COOKIE
  ): Promise<{ setCookie: string; requestId: string }> {
    const res = await tester.client.get(loginPath, {
      maxRedirects: 0,
      validateStatus: () => true
    })
    expect(res.status).toBe(302)
    const setCookie = (res.headers['set-cookie'] ?? []).find((h) =>
      h.startsWith(`${noncePrefix}.`)
    )
    if (!setCookie) throw new Error('Nonce cookie not set')
    const samlRequest =
      new URL(res.headers.location as string).searchParams.get('SAMLRequest') ??
      ''
    const xml = inflateRawSync(Buffer.from(samlRequest, 'base64')).toString()
    const requestId = /ID="([^"]+)"/.exec(xml)?.[1]
    if (!requestId) throw new Error('No ID in SAMLRequest')
    return { setCookie, requestId }
  }

  const forgedCookie = (cookieName: string, id: string, secret: string) =>
    `${cookieName}=${encodeURIComponent(`s:${sign(id, secret)}`)}`

  async function postCallback(requestId: string, cookieHeader?: string) {
    tester.nockScope.post('/system/citizen-login').reply(200, mockUser)
    const res = await axios.post(
      SP_LOGIN_CALLBACK_ENDPOINT,
      new URLSearchParams({
        SAMLResponse: buildLoginResponse('test@test.local', '_s', requestId)
      }),
      {
        baseURL: tester.client.defaults.baseURL,
        headers: cookieHeader ? { Cookie: cookieHeader } : {},
        maxRedirects: 0,
        validateStatus: () => true
      }
    )
    tester.nockScope.done()
    return res
  }

  function expectNonceAudit(pattern: RegExp) {
    expect(logAuditEvent).toHaveBeenCalledWith(
      'evaka.saml.suomifi.sign_in_nonce_check',
      expect.anything(),
      expect.stringMatching(pattern)
    )
  }

  const clearedNonceCookies = (setCookies: string[] | undefined) =>
    (setCookies ?? [])
      .filter((h) => h.includes('Expires=Thu, 01 Jan 1970'))
      .map(cookieName)
      .filter((name) => name.startsWith('evaka.saml.nonce.'))

  test('login sets a nonce cookie named after the AuthnRequest', async () => {
    const { setCookie, requestId } = await startLogin()
    expect(cookieName(setCookie)).toBe(`${CITIZEN_NONCE_COOKIE}.${requestId}`)
    expect(setCookie).toMatch(
      new RegExp(`;\\s*Path=${escape(SP_LOGIN_CALLBACK_ENDPOINT)}(;|$)`, 'i')
    )
    expect(setCookie).toMatch(/;\s*Max-Age=3600/i)
    expect(setCookie).toMatch(/;\s*HttpOnly/i)
    expect(setCookie).toMatch(/;\s*Secure/i)
    expect(setCookie).toMatch(/;\s*SameSite=None/i)
    expect(unsign(cookieValue(setCookie).slice(2), CITIZEN_COOKIE_SECRET)).toBe(
      requestId
    )
    expect(logAuditEvent).toHaveBeenCalledWith(
      'evaka.saml.suomifi.sign_in_started',
      expect.anything(),
      expect.stringMatching(
        new RegExp(
          `requestId=${requestId} sessionType=citizen host=localhost userAgent=`
        )
      )
    )
  })

  test.each([
    [
      'employee Suomi.fi',
      '/api/employee/auth/sfi/login',
      'evaka.saml.nonce.suomifi.employee',
      SP_LOGIN_CALLBACK_ENDPOINT
    ],
    [
      'Entra ID',
      '/api/employee/auth/ad/login',
      'evaka.saml.nonce.ead.employee',
      AD_LOGIN_CALLBACK_ENDPOINT
    ]
  ])(
    '%s login sets an employee nonce cookie on its callback path',
    async (_, loginPath, noncePrefix, callbackPath) => {
      const { setCookie, requestId } = await startLogin(loginPath, noncePrefix)
      expect(cookieName(setCookie)).toBe(`${noncePrefix}.${requestId}`)
      expect(setCookie).toMatch(
        new RegExp(`;\\s*Path=${escape(callbackPath)}(;|$)`, 'i')
      )
      expect(
        unsign(cookieValue(setCookie).slice(2), EMPLOYEE_COOKIE_SECRET)
      ).toBe(requestId)
    }
  )

  test('callback logs match, clears the cookie and logs the user in', async () => {
    const { setCookie, requestId } = await startLogin()
    const res = await postCallback(requestId, cookiePair(setCookie))
    expect(res.status).toBe(302)
    expect(res.headers.location).not.toContain('loginError')
    expectNonceAudit(
      new RegExp(
        `outcome=match sessionType=citizen host=localhost cookieId=${requestId} inResponseTo=${requestId} otherCookies=0 userAgent=`
      )
    )
    expect(clearedNonceCookies(res.headers['set-cookie'])).toEqual([
      `${CITIZEN_NONCE_COOKIE}.${requestId}`
    ])
  })

  test('callback logs mismatch for a tampered cookie and still logs the user in', async () => {
    const { requestId } = await startLogin()
    const res = await postCallback(
      requestId,
      forgedCookie(
        `${CITIZEN_NONCE_COOKIE}.${requestId}`,
        '_other',
        CITIZEN_COOKIE_SECRET
      )
    )
    expect(res.status).toBe(302)
    expect(res.headers.location).not.toContain('loginError')
    expectNonceAudit(/outcome=mismatch .* cookieId=_other /)
    expect(clearedNonceCookies(res.headers['set-cookie'])).toEqual([
      `${CITIZEN_NONCE_COOKIE}.${requestId}`
    ])
  })

  test('callback logs missing and still logs the user in', async () => {
    const { requestId } = await startLogin()
    const res = await postCallback(requestId)
    expect(res.status).toBe(302)
    expect(res.headers.location).not.toContain('loginError')
    expectNonceAudit(/outcome=missing .* cookieId= .* otherCookies=0 /)
    expect(clearedNonceCookies(res.headers['set-cookie'])).toEqual([])
  })

  test('missing reports how many other nonce cookies the browser sent', async () => {
    const { requestId } = await startLogin()
    const other = await startLogin()
    await postCallback(requestId, cookiePair(other.setCookie))
    expectNonceAudit(
      new RegExp(`outcome=missing .* inResponseTo=${requestId} otherCookies=1 `)
    )
  })

  test('parallel logins of the same kind both match', async () => {
    const first = await startLogin()
    const second = await startLogin()
    const bothCookies = `${cookiePair(first.setCookie)}; ${cookiePair(second.setCookie)}`

    const firstRes = await postCallback(first.requestId, bothCookies)
    expectNonceAudit(
      new RegExp(`outcome=match .* inResponseTo=${first.requestId} `)
    )
    expect(clearedNonceCookies(firstRes.headers['set-cookie'])).toEqual([
      `${CITIZEN_NONCE_COOKIE}.${first.requestId}`
    ])

    await postCallback(second.requestId, cookiePair(second.setCookie))
    expectNonceAudit(
      new RegExp(`outcome=match .* inResponseTo=${second.requestId} `)
    )
  })

  test('an employee Suomi.fi nonce cookie does not count for a citizen login', async () => {
    const { requestId } = await startLogin()
    const res = await postCallback(
      requestId,
      forgedCookie(
        `evaka.saml.nonce.suomifi.employee.${requestId}`,
        requestId,
        EMPLOYEE_COOKIE_SECRET
      )
    )
    expect(res.status).toBe(302)
    expectNonceAudit(/outcome=missing .* otherCookies=0 /)
  })

  test('a failing nonce check does not block the login', async () => {
    vi.mocked(checkNonceCookie).mockImplementationOnce(() => {
      throw new Error('boom')
    })
    const { setCookie, requestId } = await startLogin()
    const res = await postCallback(requestId, cookiePair(setCookie))
    expect(res.status).toBe(302)
    expect(res.headers.location).not.toContain('loginError')
    expect(logError).toHaveBeenCalledWith(
      'SAML nonce check failed',
      expect.anything(),
      undefined,
      expect.any(Error)
    )
  })

  test('a failing nonce cookie write does not block the login redirect', async () => {
    vi.mocked(setNonceCookie).mockImplementationOnce(() => {
      throw new Error('boom')
    })
    const res = await tester.client.get('/api/citizen/auth/sfi/login', {
      maxRedirects: 0,
      validateStatus: () => true
    })
    expect(res.status).toBe(302)
    expect(res.headers.location).toMatch(new RegExp(`^${IDP_ENTRY_POINT_URL}`))
    expect(logError).toHaveBeenCalledWith(
      'SAML nonce cookie could not be set',
      expect.anything(),
      undefined,
      expect.any(Error)
    )
  })

  test('a failing AuthnRequest logs sign_in_started once, without a request ID', async () => {
    const spy = vi
      .spyOn(SAML.prototype, 'getAuthorizeUrlAsync')
      .mockRejectedValueOnce(new Error('boom'))
    try {
      await tester.client.get('/api/citizen/auth/sfi/login', {
        maxRedirects: 0,
        validateStatus: () => true
      })
    } finally {
      spy.mockRestore()
    }
    const calls = vi.mocked(logAuditEvent).mock.calls
    const started = calls.filter(
      ([code]) => code === 'evaka.saml.suomifi.sign_in_started'
    )
    expect(started).toHaveLength(1)
    expect(started[0][2]).toMatch(/^Login endpoint called requestId= /)
    expect(calls.map(([code]) => code)).toContain(
      'evaka.saml.suomifi.sign_in_failed'
    )
    expect(setNonceCookie).not.toHaveBeenCalled()
  })
})
