// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Platform =
  | { os: 'ios'; isSafari: boolean }
  | { os: 'android' }
  | { os: 'other' }

export function detectPlatform(
  userAgent: string = typeof navigator !== 'undefined'
    ? navigator.userAgent
    : ''
): Platform {
  // iOS detection: iPhone, iPad, iPod. iPadOS 13+ also matches "Macintosh"
  // with touch support, but we deliberately only treat the explicit
  // identifiers as iOS to avoid false positives on macOS Safari.
  if (/iPad|iPhone|iPod/.test(userAgent)) {
    // On iOS, the only browser engine is WebKit. "Real" Safari is the only
    // one that exposes the official "Add to Home Screen" UI; CriOS, FxiOS,
    // EdgiOS etc. are wrappers that don't.
    const isSafari = !/CriOS|FxiOS|EdgiOS|OPiOS/.test(userAgent)
    return { os: 'ios', isSafari }
  }

  if (userAgent.includes('Android')) {
    return { os: 'android' }
  }

  return { os: 'other' }
}

export type BrowserOs =
  | 'ios'
  | 'android'
  | 'macos'
  | 'windows'
  | 'linux'
  | 'other'

export type BrowserFamily =
  | 'safari'
  | 'chrome'
  | 'chrome-ios'
  | 'edge'
  | 'firefox'
  | 'samsung-internet'
  | 'other'

export interface BrowserInfo {
  os: BrowserOs
  family: BrowserFamily
  isStandalone: boolean
}

export function detectBrowser(
  userAgent: string = typeof navigator !== 'undefined'
    ? navigator.userAgent
    : '',
  mediaQueryMatch: boolean = typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function'
    ? window.matchMedia('(display-mode: standalone)').matches
    : false
): BrowserInfo {
  const os: BrowserOs = (() => {
    if (/iPad|iPhone|iPod/.test(userAgent)) return 'ios'
    if (userAgent.includes('Android')) return 'android'
    if (userAgent.includes('Macintosh') || userAgent.includes('Mac OS X'))
      return 'macos'
    if (userAgent.includes('Windows')) return 'windows'
    if (userAgent.includes('Linux')) return 'linux'
    return 'other'
  })()

  const family: BrowserFamily = (() => {
    if (os === 'ios') {
      if (userAgent.includes('CriOS')) return 'chrome-ios'
      if (userAgent.includes('FxiOS')) return 'other'
      if (userAgent.includes('EdgiOS')) return 'other'
      return 'safari'
    }
    if (userAgent.includes('SamsungBrowser')) return 'samsung-internet'
    if (userAgent.includes('Edg/')) return 'edge'
    if (userAgent.includes('Firefox')) return 'firefox'
    if (userAgent.includes('Chrome')) return 'chrome'
    if (userAgent.includes('Safari')) return 'safari'
    return 'other'
  })()

  return { os, family, isStandalone: mediaQueryMatch }
}
