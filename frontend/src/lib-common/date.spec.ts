// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatDate, isValidTime } from './date'

describe('formatDate', () => {
  it('should format valid date correctly', () => {
    expect(formatDate(new Date('2019-01-01'))).toBe('01.01.2019')
  })

  it('should format undefined to empty string', () => {
    expect(formatDate(undefined)).toBe('')
  })
})

describe('isValidTime', () => {
  it.each(['23:59', '01:01', '00:01', '00:00'])(
    '%s should be a valid time',
    (s) => expect(isValidTime(s)).toBe(true)
  )
  it.each(['foo', '24:00', '1:1', '23:60', '02:65'])(
    '%s should not be a valid time',
    (s) => expect(isValidTime(s)).toBe(false)
  )
})
