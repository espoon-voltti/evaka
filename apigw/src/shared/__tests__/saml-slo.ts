// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosResponse } from 'axios'
import fs from 'fs'
import path from 'path'
import { RedisClient } from 'redis'
import { Cookie } from 'tough-cookie'
import { SignedXml } from 'xml-crypto'
import xml2js from 'xml2js'
import xmldom from 'xmldom'
import zlib from 'zlib'
import * as config from '../config'
import { sfiConfig } from '../config'
import { fromCallback } from '../promise-utils'
import type { AuthenticatedUser } from '../service-client'
import { sessionCookie } from '../session'
import { GatewayTester } from '../test/gateway-tester'

const mockUser: AuthenticatedUser = {
  id: '942b9cab-210d-4d49-b4c9-65f26390eed3',
  roles: ['ENDUSER']
}

// Explicitly use separate domains for the simulated SP and IdP to replicate
// 3rd party cookie and SAML message parsing issues only present in those
// conditions. SP must be in a domain that, from a browser's cookie handling
// point of view, is a third party site to the IdP managing SSO / Single Logout.
//
// See also:
// https://wiki.shibboleth.net/confluence/display/IDP30/LogoutConfiguration#LogoutConfiguration-Overview
// https://simplesamlphp.org/docs/stable/simplesamlphp-idp-more#section_1
const SAML_SP_DOMAIN = new URL(sfiConfig.callbackUrl).origin
const IDP_ENTRY_POINT_URL = sfiConfig.entryPoint

// Helper constants to ensure correct endpoints in all cases
const SP_LOGIN_CALLBACK_ENDPOINT = '/api/application/auth/saml/login/callback'
const SP_LOGOUT_CALLBACK_ENDPOINT = '/api/application/auth/saml/logout/callback'
const SP_LOGIN_CALLBACK_URL = `${SAML_SP_DOMAIN}${SP_LOGIN_CALLBACK_ENDPOINT}`
const SP_LOGOUT_CALLBACK_URL = `${SAML_SP_DOMAIN}${SP_LOGOUT_CALLBACK_ENDPOINT}`
const SECURED_ENDPOINT = `/api/application/auth/status`

// Use test certificates to validate actual SAML message parsing while not using
// any real certificates/domains.
const SP_ISSUER = sfiConfig.issuer
const IDP_ISSUER = 'evaka-slo-test'
const IDP_PVK = fs
  .readFileSync(
    path.resolve(__dirname, '../../../config/test-cert/slo-test-idp-key.pem'),
    'utf8'
  )
  .toString()

describe('SAML Single Logout', () => {
  let tester: GatewayTester
  let redisClient: RedisClient
  const sfiMock = config.sfiMock

  beforeEach(async () => {
    // In order to enable the REAL Suomi.fi passport-saml Strategy only for
    // these tests, config.sfiMock should be true in every other case but this
    // test suite + as Strategy vs. DummyStrategy selection is done at app
    // import-time -> override the config and re-import app for all of these
    // tests to prevent affecting other test suites. Theoretically tests from
    // other test suites could be run in-between, so must be done beforeEach
    // instead of beforeAll.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(config as any).sfiMock = false
    const { default: app, _TEST_ONLY_redisClient } = await import(
      '../../enduser/app'
    )
    redisClient = _TEST_ONLY_redisClient
    tester = await GatewayTester.start(app, 'enduser')
  })
  afterEach(async () => {
    await tester?.afterEach()
    await tester?.stop()
    await fromCallback((cb) => redisClient?.flushall(cb))
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(config as any).sfiMock = sfiMock
  })

  test('reference case (3rd party cookies available)', async () => {
    const resPreAuth = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPreAuth.status).toBe(200)
    expect(resPreAuth.data?.loggedIn).toBeFalsy()

    // Do an IdP-initiated login (skips calling the SP /login endpoint and jumps
    // directly to the SAMLResponse phase)
    const nameId = 'aaaaaaaaa@aaaaaaaa.local'
    const sessionIndex = '_1111111111111111111111'
    const inResponseTo = 'firstAuthnRequest'
    const loginResponse = buildLoginResponse(nameId, sessionIndex, inResponseTo)
    await tester.login(mockUser, { SAMLResponse: loginResponse })

    tester.nockScope.get(`/persondetails/uuid/${mockUser.id}`).reply(200, {
      id: mockUser.id
    })
    // Secured endpoint should now be accessible with session cookies
    const res = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(res.status).toBe(200)
    expect(res.data?.loggedIn).toBe(true)
    tester.nockScope.done()

    // Next the user uses another service participating to the same IdP SSO and
    // initiates the SLO process from that other service.
    await callSLOEndpointAndAssertResult(tester, nameId, sessionIndex)

    // Logout propagation at the IdP indicated to the user that SLO to our
    // service was succesful and enduser thinks they no longer have any open
    // sessions.
    //
    // After few moments someone with the access to the computer writes opens
    // our service which must not be available without authentication.
    const resPostLogout = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPostLogout.status).toBe(200)
    expect(resPostLogout.data?.loggedIn).toBeFalsy()
  })

  test('IdP-initiated logout works without (3rd party) cookies', async () => {
    // This is otherwise identical to the reference case BUT simulates 3rd party
    // cookies being blocked (even SameSite: None) which is starting to be the
    // default on many browsers (Safari; Chrome & Firefox at some point).
    //
    // If the service only relies on a session or logout cookie being available
    // to logout, the user will not actually be logged out and in the worst case
    // think they _have_ been and risk exposing their data to other entities
    // that have access to the same browser/machine.

    // Baseline
    const resPreAuth = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPreAuth.status).toBe(200)
    expect(resPreAuth.data?.loggedIn).toBeFalsy()

    // Do an IdP-initiated login (skips calling the SP /login endpoint and jumps
    // directly to the SAMLResponse phase)
    const nameId = 'aaaaaaaaa@aaaaaaaa.local'
    const sessionIndex = '_1111111111111111111111'
    const inResponseTo = 'firstAuthnRequest'
    const loginResponse = buildLoginResponse(nameId, sessionIndex, inResponseTo)
    await tester.login(mockUser, { SAMLResponse: loginResponse })

    // Secured endpoint should now be accessible with session cookies
    tester.nockScope.get(`/persondetails/uuid/${mockUser.id}`).reply(200, {
      id: mockUser.id
    })
    const res = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(res.data?.loggedIn).toBe(true)
    tester.nockScope.done()

    // Proceeding to SLO...
    //
    // This is similar situation with "reference case" but because third party cookies are blocked
    // by end user's browser following request is executed without express-session's session cookie.
    // HTTP calls from within iframe do not contain cookies when third party cookies are blocked.
    //
    // This situation is simulated by temporarily clearing the session cookies.
    // Store current cookies so that they can be restored after SLO.
    const cookie = await tester.getCookie(sessionCookie('enduser'))
    expect(cookie).toBeTruthy()
    await tester.expireSession()

    // Next the user uses another service participating to the same IdP SSO and
    // initiates the SLO process from that other service.
    await callSLOEndpointAndAssertResult(tester, nameId, sessionIndex)

    // Logout propagation at the IdP indicated to the user that SLO to our
    // service was succesful and enduser thinks they no longer have any open
    // sessions.
    //
    // After few moments someone with the access to the computer writes opens
    // our service which must not be available without authentication.
    //
    // Restore cookies to simulate returning our service
    await tester.setCookie(cookie as Cookie)
    const resPostLogout = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPostLogout.status).toBe(200)
    expect(resPostLogout.data?.loggedIn).toBeFalsy()
  })
})

async function callSLOEndpointAndAssertResult(
  tester: GatewayTester,
  nameId: string,
  sessionIndex: string
) {
  const idpInitiatedLogoutRequest = buildIdPInitiatedLogoutRequest(
    nameId,
    sessionIndex
  )
  const res = await tester.client.post(
    SP_LOGOUT_CALLBACK_ENDPOINT,
    { SAMLRequest: idpInitiatedLogoutRequest },
    {
      maxRedirects: 0,
      validateStatus: () => true
    }
  )
  expect(res.status).toBe(302)
  expect(res.headers['location']).toMatch(
    new RegExp(`^${IDP_ENTRY_POINT_URL}\\?SAMLResponse=?`)
  )
  const logoutResponse = getSamlMessageFromRedirectResponse(res)
  const logoutResponseJson = await xml2js.parseStringPromise(logoutResponse)
  expect(
    logoutResponseJson['samlp:LogoutResponse']['samlp:Status'][0][
      'samlp:StatusCode'
    ][0]['$'].Value
  ).toEqual('urn:oasis:names:tc:SAML:2.0:status:Success')
}

function getSamlMessageFromRedirectResponse(res: AxiosResponse) {
  const location = new URL(res.headers['location'])
  const msg =
    location.searchParams.get('SAMLRequest') ??
    location.searchParams.get('SAMLResponse')
  if (!msg)
    throw new Error(
      'Response must have a SAMLRequest or SAMLResponse search parameter'
    )

  const decoded = Buffer.from(msg, 'base64')
  const inflated = zlib.inflateRawSync(decoded)
  return new xmldom.DOMParser({}).parseFromString(inflated.toString())
}

function buildLoginResponse(
  nameId: string,
  sessionIndex: string,
  inResponseTo: string
) {
  const notBefore = '1980-01-01T01:00:00Z'
  const issueInstant = '1980-01-01T01:01:00Z'
  const notOnOrAfter = '4980-01-01T01:01:00Z'

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
  <saml:Assertion
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
     <saml:AuthnStatement
          AuthnInstant="${issueInstant}"
          SessionNotOnOrAfter="${notOnOrAfter}"
          SessionIndex="${sessionIndex}">
          <saml:AuthnContext>
              <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef>
          </saml:AuthnContext>
      </saml:AuthnStatement>
  </saml:Assertion>
</samlp:Response>`
  return Buffer.from(signXml(loginResponse)).toString('base64')
}

function buildIdPInitiatedLogoutRequest(nameId: string, sessionIndex: string) {
  const idpInitiatedLogoutRequest = `<samlp:LogoutRequest
  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
  xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
  ID="_adcdabcd"
  Version="2.0"
  IssueInstant="2020-01-01T01:01:00Z"
  Destination="${SP_LOGOUT_CALLBACK_URL}">
  <saml:Issuer>${IDP_ISSUER}</saml:Issuer>
  <saml:NameID
      SPNameQualifier="${SP_ISSUER}"
      Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">${nameId}</saml:NameID>
  <samlp:SessionIndex>${sessionIndex}</samlp:SessionIndex>
</samlp:LogoutRequest>`
  return Buffer.from(signXml(idpInitiatedLogoutRequest)).toString('base64')
}

function signXml(xml: string) {
  const sig = new SignedXml()
  sig.addReference(
    '/*',
    [
      'http://www.w3.org/2000/09/xmldsig#enveloped-signature',
      'http://www.w3.org/2001/10/xml-exc-c14n#'
    ],
    'http://www.w3.org/2001/04/xmlenc#sha256',
    '',
    '',
    '',
    false
  )
  sig.signatureAlgorithm = 'http://www.w3.org/2001/04/xmldsig-more#rsa-sha256'
  sig.signingKey = IDP_PVK
  sig.computeSignature(xml)
  return sig.getSignedXml()
}
