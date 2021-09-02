// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatDate } from './date'

describe('formatDate', () => {
  it('should format valid date correctly', () => {
    expect(formatDate(new Date('2019-01-01'))).toBe('01.01.2019')
  })

  it('should format undefined to empty string', () => {
    expect(formatDate(undefined)).toBe('')
  })
})
