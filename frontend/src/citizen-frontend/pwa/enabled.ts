// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { featureFlags } from 'lib-customizations/citizen'

// ?pwa=1 opts in to PWA
function readOptIn(): boolean {
  const url = new URL(window.location.href)
  const param = url.searchParams.get('pwa')
  if (param !== null) {
    url.searchParams.delete('pwa')
    window.history.replaceState(window.history.state, '', url)
  }
  try {
    const storageKey = 'evaka-citizen.pwa'
    if (param === '1') {
      window.localStorage?.setItem(storageKey, '1')
    } else if (param !== null) {
      window.localStorage?.removeItem(storageKey)
    }
    return window.localStorage?.getItem(storageKey) === '1'
  } catch {
    return param === '1'
  }
}

export const pwaEnabled: boolean = featureFlags.citizenPwa || readOptIn()
export const optIn = featureFlags.citizenPwa ? '' : '?pwa=1'
