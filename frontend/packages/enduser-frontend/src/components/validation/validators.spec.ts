// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isValidTimeString } from './validators'

describe('isValidTimeString', () => {
  it('accepts correct ones', () => {
    expect(isValidTimeString('15:00')).toEqual(true)
    expect(isValidTimeString('00:00')).toEqual(true)
    expect(isValidTimeString('23:59')).toEqual(true)
    expect(isValidTimeString('8:00')).toEqual(true)
  })
  it('fails incorrect ones', () => {
    expect(isValidTimeString('8')).toEqual(false)
    expect(isValidTimeString('24:00')).toEqual(false)
    expect(isValidTimeString('69:00')).toEqual(false)
    expect(isValidTimeString('12:60')).toEqual(false)
    expect(isValidTimeString(' ')).toEqual(false)
    expect(isValidTimeString('foobar')).toEqual(false)
    expect(isValidTimeString('foo:bar')).toEqual(false)
  })
})
