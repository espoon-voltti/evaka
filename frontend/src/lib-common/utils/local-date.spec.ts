// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { getAge } from 'lib-common/utils/local-date'

describe('local-date utils', () => {
  describe('getAge', () => {
    it('returns 0 for a person born today', () => {
      expect(getAge(LocalDate.todayInSystemTz())).toEqual(0)
    })

    it('returns 0 for a person born 11 months ago', () => {
      expect(getAge(LocalDate.todayInSystemTz().subMonths(11))).toEqual(0)
    })

    it('returns 1 for a person born 20 year ago', () => {
      expect(getAge(LocalDate.todayInSystemTz().subYears(20))).toEqual(20)
    })
  })
})
