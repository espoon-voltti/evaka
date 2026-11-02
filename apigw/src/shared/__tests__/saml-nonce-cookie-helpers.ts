// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { sign } from 'cookie-signature'
import type express from 'express'
import { describe, expect, test, vi } from 'vitest'

import type { NonceCookieConfig } from '../saml/nonce-cookie.ts'
import {
  checkNonceCookie,
  clearNonceCookie,
  nonceCookiePrefix
} from '../saml/nonce-cookie.ts'

const secret = 'nonce-cookie-test-secret'
const prefix = 'evaka.saml.nonce.suomifi.citizen'
const config: NonceCookieConfig = {
  namePrefix: prefix,
  path: '/api/application/auth/saml/login/callback',
  secret
}
const signed = (id: string, key = secret) => `s:${sign(id, key)}`

describe('SAML nonce cookie helpers', () => {
  test('nonceCookiePrefix', () => {
    expect(nonceCookiePrefix('suomifi', 'citizen')).toBe(
      'evaka.saml.nonce.suomifi.citizen'
    )
    expect(nonceCookiePrefix('suomifi', 'employee')).toBe(
      'evaka.saml.nonce.suomifi.employee'
    )
  })

  test.each([
    ['match', { [`${prefix}._abc`]: signed('_abc') }, '_abc', '_abc', 0],
    ['mismatch', { [`${prefix}._abc`]: signed('_other') }, '_abc', '_other', 0],
    [
      'mismatch',
      { [`${prefix}._abc`]: signed('_abc', 'wrong-secret') },
      '_abc',
      undefined,
      0
    ],
    ['mismatch', { [`${prefix}._abc`]: '_abc' }, '_abc', undefined, 0],
    ['missing', {}, '_abc', undefined, 0],
    [
      'missing',
      { [`${prefix}._other`]: signed('_other') },
      '_abc',
      undefined,
      1
    ],
    [
      'match',
      {
        [`${prefix}._abc`]: signed('_abc'),
        [`${prefix}._other`]: signed('_other')
      },
      '_abc',
      '_abc',
      1
    ],
    [
      'missing',
      { [`${prefix}._abc`]: signed('_abc') },
      undefined,
      undefined,
      1
    ],
    [
      'missing',
      { 'evaka.saml.nonce.suomifi.employee._abc': signed('_abc') },
      '_abc',
      undefined,
      0
    ]
  ] as const)(
    'checkNonceCookie → %s (case %#)',
    (outcome, cookies, inResponseTo, cookieId, otherCookies) => {
      expect(checkNonceCookie(cookies, config, inResponseTo)).toEqual({
        outcome,
        cookieId,
        otherCookies
      })
    }
  )

  test('clearNonceCookie clears only the cookie of the given request', () => {
    const clearCookie = vi.fn()
    clearNonceCookie(
      { clearCookie } as unknown as express.Response,
      config,
      '_abc'
    )
    expect(clearCookie).toHaveBeenCalledWith(`${prefix}._abc`, {
      httpOnly: true,
      secure: true,
      sameSite: 'none',
      path: config.path
    })
  })
})
