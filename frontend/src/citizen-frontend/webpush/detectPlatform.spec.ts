// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { detectBrowser } from '../pwa/detectPlatform'

describe('detectBrowser', () => {
  it('detects Chrome on Android', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'chrome',
      isStandalone: false
    })
  })

  it('detects Samsung Internet on Android', () => {
    const ua =
      'Mozilla/5.0 (Linux; Android 13; SAMSUNG SM-G975U) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/22.0 Chrome/115.0.0.0 Mobile Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'samsung-internet'
    })
  })

  it('detects Firefox on Android', () => {
    const ua =
      'Mozilla/5.0 (Android 13; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'android',
      family: 'firefox'
    })
  })

  it('detects Safari on iOS', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'ios',
      family: 'safari'
    })
  })

  it('detects Chrome on iOS as non-Safari (CriOS)', () => {
    const ua =
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.0.0 Mobile/15E148 Safari/604.1'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'ios',
      family: 'chrome-ios'
    })
  })

  it('detects Edge on desktop Windows', () => {
    const ua =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'windows',
      family: 'edge'
    })
  })

  it('detects Chrome on desktop Linux', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'linux',
      family: 'chrome'
    })
  })

  it('detects Firefox on desktop', () => {
    const ua =
      'Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0'
    expect(detectBrowser(ua)).toMatchObject({
      family: 'firefox'
    })
  })

  it('detects Safari on macOS', () => {
    const ua =
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15'
    expect(detectBrowser(ua)).toMatchObject({
      os: 'macos',
      family: 'safari'
    })
  })

  it('falls back cleanly on unknown UA', () => {
    expect(detectBrowser('ReallyWeirdBot/1.0')).toMatchObject({
      os: 'other',
      family: 'other'
    })
  })
})
