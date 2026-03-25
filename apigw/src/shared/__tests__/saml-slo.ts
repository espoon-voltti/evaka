// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import zlib from 'node:zlib'

import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import { ValidateInResponseTo } from '@node-saml/node-saml'
import xmldom from '@xmldom/xmldom'
import type { AxiosResponse } from 'axios'
import type { Cookie } from 'tough-cookie'
import xml2js from 'xml2js'

import type { Config } from '../config.ts'
import { configFromEnv } from '../config.ts'
import type { DevCitizen } from '../dev-api.ts'
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

const mockUser: DevCitizen & CitizenUser = {
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
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
    await tester.setCookie(cookie as Cookie)
    const resPostLogout = await tester.client.get(SECURED_ENDPOINT, {
      validateStatus: () => true
    })
    expect(resPostLogout.status).toBe(200)
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
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
  expect(res.headers['location']).toMatch(
    new RegExp(`^${IDP_ENTRY_POINT_URL}\\?SAMLResponse=?`)
  )
  const logoutResponse = getSamlMessageFromRedirectResponse(res)
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const logoutResponseJson = await xml2js.parseStringPromise(logoutResponse)
  expect(
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    logoutResponseJson['samlp:LogoutResponse']['samlp:Status'][0][
      'samlp:StatusCode'
    ][0]['$'].Value
  ).toEqual('urn:oasis:names:tc:SAML:2.0:status:Success')
}

function getSamlMessageFromRedirectResponse(res: AxiosResponse) {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
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
