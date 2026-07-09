// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import zlib from 'node:zlib'

import { ValidateInResponseTo } from '@node-saml/node-saml'
import xmldom from '@xmldom/xmldom'
import type { AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'
import xml2js from 'xml2js'

import type { Config } from '../config.ts'
import { configFromEnv } from '../config.ts'
import type { CitizenUser } from '../service-client.ts'
import { sessionCookie } from '../session.ts'
import { GatewayTester } from '../test/gateway-tester.ts'
import {
  buildIdPInitiatedLogoutRequest,
  buildLoginResponse,
  IDP_ENTRY_POINT_URL,
  SP_CALLBACK_URL,
  SP_ISSUER,
  SP_LOGOUT_CALLBACK_ENDPOINT
} from '../test/saml-test-helpers.ts'

const mockUser: CitizenUser & {
  firstName: string
  lastName: string
  ssn: string
  dependantCount: number
} = {
  id: '942b9cab-210d-4d49-b4c9-65f26390eed3',
  firstName: 'dummy',
  lastName: 'dummy',
  ssn: '010101-999X',
  dependantCount: 0
}

const SECURED_ENDPOINT = `/api/citizen/auth/status`

describe('SAML Single Logout', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    const config: Config = {
      ...configFromEnv(),
      sfi: {
        type: 'saml',
        // Explicitly use separate domains for the simulated SP and IdP to replicate
        // 3rd party cookie and SAML message parsing issues only present in those
        // conditions. SP must be in a domain that, from a browser's cookie handling
        // point of view, is a third party site to the IdP managing SSO / Single Logout.
        //
        // See also:
        // https://wiki.shibboleth.net/confluence/display/IDP30/LogoutConfiguration#LogoutConfiguration-Overview
        // https://simplesamlphp.org/docs/stable/simplesamlphp-idp-more#section_1
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

  test('reference case (3rd party cookies available)', async () => {
    const resPreAuth = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPreAuth.status).toBe(200)
    // oxlint-disable-next-line typescript/no-unsafe-member-access
    expect(resPreAuth.data?.loggedIn).toBeFalsy()

    // Do an IdP-initiated login (skips calling the SP /login endpoint and jumps
    // directly to the SAMLResponse phase)
    const nameId = 'aaaaaaaaa@aaaaaaaa.local'
    const sessionIndex = '_1111111111111111111111'
    const inResponseTo = 'firstAuthnRequest'
    const loginResponse = buildLoginResponse(nameId, sessionIndex, inResponseTo)
    await tester.login(mockUser, { SAMLResponse: loginResponse })

    tester.nockScope.get(`/system/citizen/${mockUser.id}`).reply(200, {
      id: mockUser.id
    })
    // Secured endpoint should now be accessible with session cookies
    const res = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(res.status).toBe(200)
    // oxlint-disable-next-line typescript/no-unsafe-member-access
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
    // oxlint-disable-next-line typescript/no-unsafe-member-access
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
    // oxlint-disable-next-line typescript/no-unsafe-member-access
    expect(resPreAuth.data?.loggedIn).toBeFalsy()

    // Do an IdP-initiated login (skips calling the SP /login endpoint and jumps
    // directly to the SAMLResponse phase)
    const nameId = 'aaaaaaaaa@aaaaaaaa.local'
    const sessionIndex = '_1111111111111111111111'
    const inResponseTo = 'firstAuthnRequest'
    const loginResponse = buildLoginResponse(nameId, sessionIndex, inResponseTo)
    await tester.login(mockUser, { SAMLResponse: loginResponse })

    // Secured endpoint should now be accessible with session cookies
    tester.nockScope.get(`/system/citizen/${mockUser.id}`).reply(200, {
      id: mockUser.id
    })
    const res = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    // oxlint-disable-next-line typescript/no-unsafe-member-access
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
    const cookie = await tester.getCookie(sessionCookie('citizen'))
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
    await tester.setCookie(cookie!)
    const resPostLogout = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPostLogout.status).toBe(200)
    // oxlint-disable-next-line typescript/no-unsafe-member-access
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
    new URLSearchParams({ SAMLRequest: idpInitiatedLogoutRequest }),
    {
      maxRedirects: 0,
      validateStatus: () => true
    }
  )
  expect(res.status).toBe(302)
  expect(res.headers.location).toMatch(
    new RegExp(`^${IDP_ENTRY_POINT_URL}\\?SAMLResponse=?`)
  )
  const logoutResponse = getSamlMessageFromRedirectResponse(res)
  // oxlint-disable-next-line typescript/no-unsafe-assignment
  const logoutResponseJson = await xml2js.parseStringPromise(logoutResponse)
  expect(
    // oxlint-disable-next-line typescript/no-unsafe-member-access
    logoutResponseJson['samlp:LogoutResponse']['samlp:Status'][0][
      'samlp:StatusCode'
    ][0].$.Value
  ).toEqual('urn:oasis:names:tc:SAML:2.0:status:Success')
}

function getSamlMessageFromRedirectResponse(res: AxiosResponse) {
  // oxlint-disable-next-line typescript/no-unsafe-argument
  const location = new URL(res.headers.location)
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

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Reads the SAML LogoutRequest that our /logout endpoint redirects the browser
// to, and returns its SessionIndex.
async function logoutRequestSessionIndex(
  tester: GatewayTester,
  logoutPath: string
): Promise<string> {
  const res = await tester.client.get(logoutPath, {
    maxRedirects: 0,
    validateStatus: () => true
  })
  expect(res.status).toBe(302)
  const dom = getSamlMessageFromRedirectResponse(res)
  // oxlint-disable-next-line typescript/no-unsafe-assignment
  const json = await xml2js.parseStringPromise(dom)
  // node-saml emits the SessionIndex element with a different (but
  // equivalent) namespace prefix than the rest of the LogoutRequest, and
  // since it declares that namespace inline, xml2js parses it as a node
  // with attributes (`$`) rather than a plain string.
  // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
  const sessionIndexNode = json['samlp:LogoutRequest']['saml2p:SessionIndex'][0]
  return typeof sessionIndexNode === 'string'
    ? sessionIndexNode
    : // oxlint-disable-next-line typescript/no-unsafe-member-access
      (sessionIndexNode._ as string)
}

// The SP registers a single SLO callback for both audiences; an IdP-initiated
// logout carries no eVaka RelayState, so it always dispatches to the citizen
// integration regardless of which session it names.
const SHARED_SLO_ENDPOINT = '/api/application/auth/saml/logout/callback'

async function idpInitiatedSlo(
  tester: GatewayTester,
  nameId: string,
  sessionIndex: string,
  { withCookies }: { withCookies: boolean }
): Promise<string> {
  const cookies = withCookies
    ? []
    : [
        await tester.getCookie(sessionCookie('citizen')),
        await tester.getCookie(sessionCookie('employee'))
      ]
  // The IdP's logout request is cross-site, so SameSite=lax withholds our
  // session cookies from it.
  if (!withCookies) await tester.cookies.removeAllCookies()

  const res = await tester.client.post(
    SHARED_SLO_ENDPOINT,
    new URLSearchParams({
      SAMLRequest: buildIdPInitiatedLogoutRequest(nameId, sessionIndex)
    }),
    { maxRedirects: 0, validateStatus: () => true }
  )
  expect(res.status).toBe(302)

  for (const cookie of cookies) if (cookie) await tester.setCookie(cookie)

  const logoutResponse = getSamlMessageFromRedirectResponse(res)
  // oxlint-disable-next-line typescript/no-unsafe-assignment
  const json = await xml2js.parseStringPromise(logoutResponse)
  // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-return
  return json['samlp:LogoutResponse']['samlp:Status'][0]['samlp:StatusCode'][0]
    .$.Value
}

const SAML_SUCCESS = 'urn:oasis:names:tc:SAML:2.0:status:Success'

async function isCitizenLoggedIn(tester: GatewayTester): Promise<boolean> {
  tester.nockScope
    .get(`/system/citizen/${mockUser.id}`)
    .reply(200, { id: mockUser.id })
  const res = await tester.client.get(SECURED_ENDPOINT, {
    validateStatus: () => true
  })
  // oxlint-disable-next-line typescript/no-unsafe-member-access
  return res.data?.loggedIn === true
}

async function isEmployeeLoggedIn(tester: GatewayTester): Promise<boolean> {
  tester.nockScope.get(`/system/employee/${mockUser.id}`).reply(200, {
    user: {
      id: mockUser.id,
      firstName: '',
      lastName: '',
      globalRoles: [],
      allScopedRoles: [],
      accessibleFeatures: {},
      permittedGlobalActions: [],
      startPage: '/'
    },
    featureConfig: {}
  })
  const res = await tester.client.get('/api/employee/auth/status', {
    validateStatus: () => true
  })
  // oxlint-disable-next-line typescript/no-unsafe-member-access
  return res.data?.loggedIn === true
}

// Starts a sfi login through /login so the co-resident session is captured in
// the sfiCorr correlation record, then completes it at the shared callback.
async function employeeSfiLogin(
  tester: GatewayTester,
  nameId: string,
  sessionIndex: string,
  { rejected }: { rejected: boolean }
): Promise<void> {
  const startRes = await tester.client.get(
    '/api/employee/auth/sfi/login?RelayState=%2Femployee',
    { maxRedirects: 0, validateStatus: () => true }
  )
  const relayState =
    // oxlint-disable-next-line typescript/no-unsafe-argument
    new URL(startRes.headers.location).searchParams.get('RelayState') ?? ''

  const scope = tester.nockScope.post('/system/employee-sfi-login')
  if (rejected) scope.reply(403)
  else
    scope.reply(200, { id: mockUser.id, globalRoles: [], allScopedRoles: [] })

  await tester.client.post(
    '/api/application/auth/saml/login/callback',
    new URLSearchParams({
      SAMLResponse: buildLoginResponse(nameId, sessionIndex, 'authnEmployee'),
      RelayState: relayState
    }),
    { maxRedirects: 0, validateStatus: () => true }
  )
  tester.nockScope.done()
}

describe('SLO session tracking on rejected sfi login', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    const config: Config = {
      ...configFromEnv(),
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

  test('citizen logout uses the rejected employee login sessionIndex', async () => {
    // 1. Citizen logs in via sfi.
    const citizenSessionIndex = '_citizen-session-index'
    await tester.login(mockUser, {
      SAMLResponse: buildLoginResponse(
        'citizen@local',
        citizenSessionIndex,
        'authnCitizen'
      )
    })

    await delay(5) // guarantee the rejected login's createdAt is strictly newer

    // 2. Employee sfi login: SAML validates, but eVaka rejects it.
    //    Go through /login so the citizen session is captured in the sfiCorr
    //    correlation record referenced by RelayState.
    const startRes = await tester.client.get('/api/employee/auth/sfi/login', {
      maxRedirects: 0,
      validateStatus: () => true
    })
    const relayState =
      // oxlint-disable-next-line typescript/no-unsafe-argument
      new URL(startRes.headers.location).searchParams.get('RelayState') ?? ''
    expect(relayState).toContain('sfiCorr=')

    const employeeSessionIndex = '_employee-session-index'
    tester.nockScope.post('/system/employee-sfi-login').reply(403)
    await tester.client.post(
      '/api/employee/auth/sfi/login/callback',
      new URLSearchParams({
        SAMLResponse: buildLoginResponse(
          'employee@local',
          employeeSessionIndex,
          'authnEmployee'
        ),
        RelayState: relayState
      }),
      { maxRedirects: 0, validateStatus: () => true }
    )
    tester.nockScope.done()

    // 3. Citizen logout must SLO with the employee (IdP-valid) sessionIndex.
    expect(
      await logoutRequestSessionIndex(tester, '/api/citizen/auth/logout')
    ).toBe(employeeSessionIndex)
  })

  test('same-side: employee logout uses the rejected re-login sessionIndex', async () => {
    // 1. Employee logs in via sfi (backend accepts).
    const firstIndex = '_employee-first-index'
    const startRes1 = await tester.client.get('/api/employee/auth/sfi/login', {
      maxRedirects: 0,
      validateStatus: () => true
    })
    const relayState1 =
      // oxlint-disable-next-line typescript/no-unsafe-argument
      new URL(startRes1.headers.location).searchParams.get('RelayState') ?? ''
    tester.nockScope
      .post('/system/employee-sfi-login')
      .reply(200, { id: mockUser.id, globalRoles: [], allScopedRoles: [] })
    await tester.client.post(
      '/api/employee/auth/sfi/login/callback',
      new URLSearchParams({
        SAMLResponse: buildLoginResponse('emp@local', firstIndex, 'authn1'),
        RelayState: relayState1
      }),
      { maxRedirects: 0, validateStatus: () => true }
    )
    tester.nockScope.done()

    await delay(5)

    // 2. Employee re-login on the same side: SAML validates, eVaka rejects.
    //    The existing employee session is captured as the own-side session
    //    via the correlation record.
    const secondIndex = '_employee-second-index'
    const startRes2 = await tester.client.get('/api/employee/auth/sfi/login', {
      maxRedirects: 0,
      validateStatus: () => true
    })
    const relayState2 =
      // oxlint-disable-next-line typescript/no-unsafe-argument
      new URL(startRes2.headers.location).searchParams.get('RelayState') ?? ''
    expect(relayState2).toContain('sfiCorr=')
    tester.nockScope.post('/system/employee-sfi-login').reply(403)
    await tester.client.post(
      '/api/employee/auth/sfi/login/callback',
      new URLSearchParams({
        SAMLResponse: buildLoginResponse('emp@local', secondIndex, 'authn2'),
        RelayState: relayState2
      }),
      { maxRedirects: 0, validateStatus: () => true }
    )
    tester.nockScope.done()

    // 3. Employee logout must SLO with the newer (rejected re-login) index.
    expect(
      await logoutRequestSessionIndex(tester, '/api/employee/auth/logout')
    ).toBe(secondIndex)
  })

  test('IdP-initiated SLO without cookies logs out the session linked to a rejected login', async () => {
    await tester.login(mockUser, {
      SAMLResponse: buildLoginResponse('citizen@local', '_c-idx', 'authnC')
    })
    await delay(5) // guarantee the rejected login's createdAt is strictly newer
    await employeeSfiLogin(tester, 'employee@local', '_e-idx', {
      rejected: true
    })
    expect(await isCitizenLoggedIn(tester)).toBe(true)

    const status = await idpInitiatedSlo(tester, 'employee@local', '_e-idx', {
      withCookies: false
    })

    expect(status).toBe(SAML_SUCCESS)
    expect(await isCitizenLoggedIn(tester)).toBe(false)
  })

  test('IdP-initiated SLO without cookies logs out both co-resident sessions', async () => {
    await tester.login(mockUser, {
      SAMLResponse: buildLoginResponse('citizen@local', '_c-idx', 'authnC')
    })
    await employeeSfiLogin(tester, 'employee@local', '_e-idx', {
      rejected: false
    })
    expect(await isCitizenLoggedIn(tester)).toBe(true)
    expect(await isEmployeeLoggedIn(tester)).toBe(true)

    // Suomi.fi only knows the newest login, and expects it to log out both.
    const status = await idpInitiatedSlo(tester, 'employee@local', '_e-idx', {
      withCookies: false
    })

    expect(status).toBe(SAML_SUCCESS)
    expect(await isCitizenLoggedIn(tester)).toBe(false)
    expect(await isEmployeeLoggedIn(tester)).toBe(false)
  })

  test('no pre-existing session: rejected login writes nothing', async () => {
    // Employee sfi login with no citizen or employee session present.
    const startRes = await tester.client.get('/api/employee/auth/sfi/login', {
      maxRedirects: 0,
      validateStatus: () => true
    })
    const relayState =
      // oxlint-disable-next-line typescript/no-unsafe-argument
      new URL(startRes.headers.location).searchParams.get('RelayState') ?? ''
    expect(relayState).not.toContain('sfiCorr=')

    tester.nockScope.post('/system/employee-sfi-login').reply(403)
    const res = await tester.client.post(
      '/api/employee/auth/sfi/login/callback',
      new URLSearchParams({
        SAMLResponse: buildLoginResponse('nobody@local', '_x', 'authnX'),
        RelayState: relayState
      }),
      { maxRedirects: 0, validateStatus: () => true }
    )
    // Login still fails (redirect to the employee error page); nothing persisted.
    expect(res.status).toBe(302)
    expect(res.headers.location).toContain('loginError')
    tester.nockScope.done()
  })

  test('the latest rejected attempt replaces the previous one', async () => {
    const citizenIndex = '_citizen-idx'
    await tester.login(mockUser, {
      SAMLResponse: buildLoginResponse('citizen@local', citizenIndex, 'authnC')
    })

    const attempt = async (employeeIndex: string) => {
      await delay(5)
      const startRes = await tester.client.get('/api/employee/auth/sfi/login', {
        maxRedirects: 0,
        validateStatus: () => true
      })
      const relayState =
        // oxlint-disable-next-line typescript/no-unsafe-argument
        new URL(startRes.headers.location).searchParams.get('RelayState') ?? ''
      tester.nockScope.post('/system/employee-sfi-login').reply(403)
      await tester.client.post(
        '/api/employee/auth/sfi/login/callback',
        new URLSearchParams({
          SAMLResponse: buildLoginResponse(
            'employee@local',
            employeeIndex,
            'authnE'
          ),
          RelayState: relayState
        }),
        { maxRedirects: 0, validateStatus: () => true }
      )
      tester.nockScope.done()
    }

    await attempt('_employee-idx-1')
    await attempt('_employee-idx-2')

    expect(
      await logoutRequestSessionIndex(tester, '/api/citizen/auth/logout')
    ).toBe('_employee-idx-2')
  })
})
