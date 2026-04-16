// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { detectPlatform } from './detectPlatform'

describe('detectPlatform', () => {
  it('detects iOS Safari (iPhone)', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua)).toEqual({ os: 'ios', isSafari: true })
  })

  it('detects iOS Chrome (CriOS) as iOS, not Safari', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua)).toEqual({ os: 'ios', isSafari: false })
  })

  it('detects iPad as iOS', () => {
    const ua =
      'Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    expect(detectPlatform(ua).os).toBe('ios')
  })

  it('detects Android Chrome', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36'
    expect(detectPlatform(ua)).toEqual({ os: 'android' })
  })

  it('detects Android Firefox', () => {
    const ua =
      'Mozilla/5.0 (Android 14; Mobile; rv:121.0) Gecko/121.0 Firefox/121.0'
    expect(detectPlatform(ua)).toEqual({ os: 'android' })
  })

  it('returns "other" for an unknown UA', () => {
    expect(detectPlatform('SomeBot/1.0')).toEqual({ os: 'other' })
  })

  it('returns "other" for desktop Chrome', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    expect(detectPlatform(ua)).toEqual({ os: 'other' })
  })
})
