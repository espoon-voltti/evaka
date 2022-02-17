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

export const isProduction = (): boolean => {
  return getEnvironment() === 'prod'
}

export const isAutomatedTest =
  typeof window !== 'undefined' && 'evakaAutomatedTest' in window

export const isIOS = () =>
  ['iPad', 'iPhone', 'iPad Simulator', 'iPhone Simulator'].includes(
    navigator.platform
  ) ||
  (navigator.userAgent.includes('Mac') && 'ontouchend' in document)

declare global {
  interface Window {
    evakaMockedTime: string | undefined
  }
}

export const mockNow = (): string | undefined =>
  typeof window !== 'undefined' && 'evakaMockedTime' in window
    ? (window.evakaMockedTime as string)
    : undefined
