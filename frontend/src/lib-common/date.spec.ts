// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isValidTime } from './date'

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
