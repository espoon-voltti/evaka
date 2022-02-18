// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { stringToNumber, stringToInt } from './number'

describe('stringToNumber', () => {
  it('works', () => {
    expect(stringToNumber('123')).toEqual(123)
    expect(stringToNumber('123.45')).toEqual(123.45)
    expect(stringToNumber('0,45')).toEqual(0.45)

    expect(stringToNumber('')).toBeUndefined()
    expect(stringToNumber('lol')).toBeUndefined()
    expect(stringToNumber('123asdf')).toBeUndefined()
    expect(stringToNumber('a123')).toBeUndefined()
    expect(stringToNumber('111.ars')).toBeUndefined()
    expect(stringToNumber('1e2')).toBeUndefined()
    expect(stringToNumber('.123')).toBeUndefined()
    expect(stringToNumber('123.123.123')).toBeUndefined()
  })
})

describe('stringToInt', () => {
  it('works', () => {
    expect(stringToInt('123')).toEqual(123)
    expect(stringToInt('123.45')).toBeUndefined()
    expect(stringToInt('0,45')).toBeUndefined()
  })
})
