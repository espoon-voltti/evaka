// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const getEnvironment = (): string => {
  if (
    window.location.host.startsWith('localhost') ||
    window.location.host.includes(':8080')
  ) {
    return 'local'
  }

  if (window.location.host === 'espoonvarhaiskasvatus.fi') {
    return 'prod'
  }

  if (
    window.location.host.includes('espoonvarhaiskasvatus.fi') ||
    window.location.host.includes('espoon-voltti.fi')
  ) {
    const splitDomains = window.location.host.split('.')
    return splitDomains[splitDomains.length - 3]
  }

  return ''
}

interface StoredTestConfig {
  automatedTest?: boolean
  forceStandalone?: boolean
  mockedTime?: string
}

const readStoredTestConfig = (): EvakaWindowConfig | undefined => {
  try {
    const stored = window.localStorage.getItem('evaka.testConfig')
    if (!stored) return undefined
    const { automatedTest, forceStandalone, mockedTime } = JSON.parse(
      stored
    ) as StoredTestConfig
    return {
      automatedTest,
      forceStandalone,
      mockedTime: mockedTime ? new Date(mockedTime) : undefined
    }
  } catch {
    return undefined
  }
}

// WebDriver based tests cannot inject window.evaka before the page loads like
// Playwright does with an init script, so they pass the config in storage
if (typeof window !== 'undefined' && !window.evaka) {
  const storedTestConfig = readStoredTestConfig()
  if (storedTestConfig) window.evaka = storedTestConfig
}

export const isAutomatedTest =
  (typeof window !== 'undefined' ? window.evaka?.automatedTest : undefined) ??
  false

export const isIOS = () =>
  ['iPad', 'iPhone', 'iPad Simulator', 'iPhone Simulator'].includes(
    navigator.platform
  ) ||
  (navigator.userAgent.includes('Mac') && 'ontouchend' in document)

declare global {
  interface Window {
    evaka?: EvakaWindowConfig
  }

  interface EvakaWindowConfig {
    automatedTest?: boolean
    mockedTime?: Date | undefined
    keepSessionAliveThrottleTime?: number
    forceStandalone?: boolean
  }
}
