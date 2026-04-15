// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '../local-date'

export const isAutomatedTest = false

declare global {
  interface Window {
    evaka?: {
      automatedTest?: boolean
      mockedTime?: Date | undefined
    }
  }
}

/**
 * @deprecated use HelsinkiDateTime.now() instead
 */
export const mockNow = (): Date | undefined => undefined

/**
 * @deprecated use LocalDate.todayInHelsinkiTz() or LocalDate.todayInSystemTz() instead
 */
export function mockToday(): LocalDate | undefined {
  const mockedTime = mockNow()
  return mockedTime ? LocalDate.fromSystemTzDate(mockedTime) : undefined
}
