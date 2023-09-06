// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '../local-date'

export const isAutomatedTest =
  (typeof window !== 'undefined' ? window.evaka?.automatedTest : undefined) ??
  false

declare global {
  interface Window {
    evaka?: EvakaWindowConfig
  }

  interface EvakaWindowConfig {
    automatedTest?: boolean
    mockedTime?: Date | undefined
  }
}

export const mockNow = (): Date | undefined =>
  typeof window !== 'undefined' ? window.evaka?.mockedTime : undefined

export function mockToday(): LocalDate | undefined {
  const mockedTime = mockNow()
  return mockedTime ? LocalDate.fromSystemTzDate(mockedTime) : undefined
}
