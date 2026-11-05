// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { deflateRawSync, inflateRawSync } from 'node:zlib'

import { SAML, ValidateInResponseTo } from '@node-saml/node-saml'
import { describe, expect, test } from 'vitest'

import type { EvakaSamlConfig } from '../config.ts'
import { createSamlConfig } from '../saml/index.ts'
import { authnRequestId } from '../saml/request-id.ts'
import {
  IDP_ENTRY_POINT_URL,
  SP_ISSUER,
  SP_LOGIN_CALLBACK_URL
} from '../test/saml-test-helpers.ts'

const testSamlConfig: EvakaSamlConfig = {
  callbackUrl: SP_LOGIN_CALLBACK_URL,
  entryPoint: IDP_ENTRY_POINT_URL,
  logoutUrl: IDP_ENTRY_POINT_URL,
  issuer: SP_ISSUER,
  publicCert: 'config/test-cert/slo-test-idp-cert.pem',
  privateCert: 'config/test-cert/saml-private.pem',
  validateInResponseTo: ValidateInResponseTo.never,
  decryptAssertions: false,
  acceptedClockSkewMs: 0
}

const urlWithSamlRequest = (xml: string) => {
  const url = new URL(IDP_ENTRY_POINT_URL)
  url.searchParams.set(
    'SAMLRequest',
    deflateRawSync(Buffer.from(xml)).toString('base64')
  )
  return url.toString()
}

describe('authnRequestId', () => {
  test('returns the ID of the AuthnRequest node-saml generates', async () => {
    const saml = new SAML(createSamlConfig(testSamlConfig))
    const url = await saml.getAuthorizeUrlAsync('', undefined, {})
    const samlRequest = new URL(url).searchParams.get('SAMLRequest') ?? ''
    const xml = inflateRawSync(Buffer.from(samlRequest, 'base64')).toString()
    const id = authnRequestId(url)
    expect(id).toMatch(/^_[0-9a-f]+$/)
    expect(xml).toContain(`ID="${id}"`)
  })

  test('returns a different ID for each request', async () => {
    const saml = new SAML(createSamlConfig(testSamlConfig))
    const a = authnRequestId(await saml.getAuthorizeUrlAsync('', undefined, {}))
    const b = authnRequestId(await saml.getAuthorizeUrlAsync('', undefined, {}))
    expect(a).toBeDefined()
    expect(a).not.toBe(b)
  })

  test('returns undefined when there is no SAMLRequest', () => {
    expect(authnRequestId(IDP_ENTRY_POINT_URL)).toBeUndefined()
  })

  test('returns undefined for a malformed SAMLRequest', () => {
    const url = new URL(IDP_ENTRY_POINT_URL)
    url.searchParams.set('SAMLRequest', 'not-deflated')
    expect(authnRequestId(url.toString())).toBeUndefined()
  })

  test('returns undefined for a request other than AuthnRequest', () => {
    expect(
      authnRequestId(
        urlWithSamlRequest(
          '<samlp:LogoutRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" ID="_abc"/>'
        )
      )
    ).toBeUndefined()
  })
})
