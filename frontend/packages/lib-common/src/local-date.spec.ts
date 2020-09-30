// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from './local-date'

describe('LocalDate', () => {
  it('can be parsed from an ISO string', () => {
    const value = LocalDate.parseIso('2020-01-20')
    expect(value.year).toStrictEqual(2020)
    expect(value.month).toStrictEqual(1)
    expect(value.date).toStrictEqual(20)
  })
  it('cannot be parsed from an ISO timestamp string', () => {
    expect(() => LocalDate.parseIso('2020-12-12T12:12:12+02:00')).toThrow()
  })
  it('uses 1-based month indexing', () => {
    const inJanuary = LocalDate.of(2020, 1, 1)
    const inDecember = inJanuary.subMonths(1)
    expect(inDecember.year).toStrictEqual(2019)
    expect(inDecember.month).toStrictEqual(12)
    expect(inDecember.date).toStrictEqual(1)
  })
  it('becomes an ISO string when JSONified', () => {
    const json = JSON.stringify({ date: LocalDate.of(2020, 2, 1) })
    expect(JSON.parse(json)).toStrictEqual({
      date: '2020-02-01'
    })
  })
})
