// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type express from 'express'
import { describe, expect, test } from 'vitest'

import {
  buildRelayStateWithCorrelationToken,
  extractCorrelationToken,
  validateRelayStateUrl
} from '../saml/index.ts'

const reqWith = (relayState: string) =>
  ({
    body: { RelayState: relayState },
    query: {}
  }) as express.Request

describe('RelayState correlation token', () => {
  test('appends and extracts the token', () => {
    const built = buildRelayStateWithCorrelationToken('/citizen/foo', 'tok-abc')
    expect(built).toBe('/citizen/foo&sfiCorr=tok-abc')
    expect(extractCorrelationToken(reqWith(built))).toBe('tok-abc')
  })

  test('no token appended when undefined; extraction yields undefined', () => {
    const built = buildRelayStateWithCorrelationToken('/x', undefined)
    expect(built).toBe('/x')
    expect(extractCorrelationToken(reqWith(built))).toBeUndefined()
  })

  test('strips and reads only the appended token when the base URL already carries a query string with the token param', () => {
    const built = buildRelayStateWithCorrelationToken(
      '/c/foo?a=1&sfiCorr=decoy',
      'tok-1'
    )
    expect(extractCorrelationToken(reqWith(built))).toBe('tok-1')
    const url = validateRelayStateUrl(reqWith(built))
    expect(url?.pathname).toBe('/c/foo')
    expect(url?.search).toBe('?a=1&sfiCorr=decoy')
  })

  test('malformed token percent-encoding yields undefined instead of throwing', () => {
    expect(extractCorrelationToken(reqWith('/x&sfiCorr=%'))).toBeUndefined()
  })
})
