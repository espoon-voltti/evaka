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
