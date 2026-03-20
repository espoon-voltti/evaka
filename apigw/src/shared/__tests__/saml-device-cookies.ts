// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import { ValidateInResponseTo } from '@node-saml/node-saml'
import type { AxiosResponse } from 'axios'
import { unsign } from 'cookie-signature'
import { Cookie } from 'tough-cookie'
import { SignedXml } from 'xml-crypto'

import type { Config } from '../config.ts'
import { configFromEnv } from '../config.ts'
import { AUTH_HISTORY_COOKIE_PREFIX } from '../device-cookies.ts'
import { GatewayTester } from '../test/gateway-tester.ts'

const mockUser = {
  id: '942b9cab-210d-4d49-b4c9-65f26390eed3'
}

const SP_CALLBACK_URL =
  'https://saml-sp.qwerty.local/api/citizen/auth/sfi/logout/callback'
const IDP_ENTRY_POINT_URL = 'https://identity-provider.asdf.local/idp'

const SP_LOGIN_CALLBACK_ENDPOINT = '/api/citizen/auth/sfi/login/callback'
const SP_LOGIN_CALLBACK_URL = `${new URL(SP_CALLBACK_URL).origin}${SP_LOGIN_CALLBACK_ENDPOINT}`

const SP_ISSUER = 'evaka-local'
const IDP_ISSUER = 'evaka-slo-test'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const IDP_PVK = fs
  .readFileSync(
    path.resolve(__dirname, '../../../config/test-cert/slo-test-idp-key.pem'),
    'utf8'
  )
  .toString()

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
    const loginResponse = buildLoginResponse()
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

function buildLoginResponse() {
  const notBefore = '1980-01-01T01:00:00Z'
  const issueInstant = '1980-01-01T01:01:00Z'
  const notOnOrAfter = '4980-01-01T01:01:00Z'
  const nameId = 'test@test.local'
  const sessionIndex = '_test_session'
  const inResponseTo = 'testAuthnRequest'

  const assertion = `<saml:Assertion
      xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:xs="http://www.w3.org/2001/XMLSchema"
      ID="_bbbbbbbbbbbbbbbbbbbbbbbb"
      Version="2.0" IssueInstant="${issueInstant}">
      <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
      <saml:Subject>
          <saml:NameID
              SPNameQualifier="${SP_ISSUER}"
              Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">${nameId}</saml:NameID>
          <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
              <saml:SubjectConfirmationData
                  NotOnOrAfter="${notOnOrAfter}"
                  Recipient="${SP_LOGIN_CALLBACK_URL}"
                  InResponseTo="${inResponseTo}"/>
          </saml:SubjectConfirmation>
      </saml:Subject>
      <saml:Conditions
          NotBefore="${notBefore}"
          NotOnOrAfter="${notOnOrAfter}">
          <saml:AudienceRestriction>
              <saml:Audience>${SP_ISSUER}</saml:Audience>
          </saml:AudienceRestriction>
      </saml:Conditions>
      <saml:AttributeStatement>
        <saml:Attribute Name="urn:oid:1.2.246.21">
          <saml:AttributeValue>010101-999X</saml:AttributeValue>
        </saml:Attribute>
        <saml:Attribute Name="urn:oid:2.5.4.42">
          <saml:AttributeValue>Etunimi</saml:AttributeValue>
        </saml:Attribute>
        <saml:Attribute Name="urn:oid:2.5.4.4">
          <saml:AttributeValue>Sukunimi</saml:AttributeValue>
        </saml:Attribute>
      </saml:AttributeStatement>
      <saml:AuthnStatement
          AuthnInstant="${issueInstant}"
          SessionNotOnOrAfter="${notOnOrAfter}"
          SessionIndex="${sessionIndex}">
          <saml:AuthnContext>
              <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef>
          </saml:AuthnContext>
      </saml:AuthnStatement>
  </saml:Assertion>`

  const loginResponse = `<samlp:Response
  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
  xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
  ID="_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  Version="2.0"
  IssueInstant="${issueInstant}"
  Destination="${SP_LOGIN_CALLBACK_URL}"
  InResponseTo="${inResponseTo}">
  <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
  <samlp:Status>
      <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
  </samlp:Status>
  ${signXml(assertion)}
</samlp:Response>`
  return Buffer.from(signXml(loginResponse)).toString('base64')
}

function signXml(xml: string) {
  const sig = new SignedXml()
  sig.addReference({
    xpath: '/*',
    transforms: [
      'http://www.w3.org/2000/09/xmldsig#enveloped-signature',
      'http://www.w3.org/2001/10/xml-exc-c14n#'
    ],
    digestAlgorithm: 'http://www.w3.org/2001/04/xmlenc#sha256'
  })
  sig.signatureAlgorithm = 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256'
  sig.canonicalizationAlgorithm = 'http://www.w3.org/2001/10/xml-exc-c14n#'
  sig.privateKey = IDP_PVK
  sig.computeSignature(xml)
  return sig.getSignedXml()
}
