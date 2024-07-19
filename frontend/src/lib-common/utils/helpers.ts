// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '../local-date'

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

export const isProduction = (): boolean => getEnvironment() === 'prod'

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
    databaseId?: number
    mockedTime?: Date | undefined
  }
}

/**
 * @deprecated use HelsinkiDateTime.now() instead
 */
export const mockNow = (): Date | undefined =>
  typeof window !== 'undefined' ? window.evaka?.mockedTime : undefined

/**
 * @deprecated use LocalDate.todayInHelsinkiTz() or LocalDate.todayInSystemTz() instead
 */
export function mockToday(): LocalDate | undefined {
  const mockedTime = mockNow()
  return mockedTime ? LocalDate.fromSystemTzDate(mockedTime) : undefined
}
