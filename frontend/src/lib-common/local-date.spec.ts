// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
  it('can be formatted to default format', () => {
    const date = LocalDate.of(2022, 2, 1)
    expect(date.format()).toStrictEqual('01.02.2022')
  })
  it('can be parsed from default format', () => {
    const expected = LocalDate.of(2022, 2, 1)
    expect(
      LocalDate.parseFiOrNull('01.02.2022')?.isEqual(expected)
    ).toBeTruthy()
    expect(LocalDate.parseFiOrNull('1.2.2022')?.isEqual(expected)).toBeTruthy()
  })
  it('considers non-existing dates invalid', () => {
    expect(LocalDate.tryParseIso('2020-00-01')).toBeUndefined()
    expect(() => LocalDate.of(2020, 0, 1)).toThrow()
    expect(LocalDate.parseFiOrNull('31.02.2020')).toBeNull()
  })
  it('supports comparisons', () => {
    const middle = LocalDate.of(2020, 7, 7)

    const before = LocalDate.of(2019, 1, 1)
    expect(before.isBefore(middle)).toBeTruthy()
    expect(before.isEqualOrBefore(middle)).toBeTruthy()
    expect(before.isEqual(middle)).toBeFalsy()
    expect(before.isEqualOrAfter(middle)).toBeFalsy()
    expect(before.isAfter(middle)).toBeFalsy()

    const after = LocalDate.of(2021, 9, 9)
    expect(after.isBefore(middle)).toBeFalsy()
    expect(after.isEqualOrBefore(middle)).toBeFalsy()
    expect(after.isEqual(middle)).toBeFalsy()
    expect(after.isEqualOrAfter(middle)).toBeTruthy()
    expect(after.isAfter(middle)).toBeTruthy()

    const duplicate = LocalDate.parseIso(middle.formatIso())
    expect(duplicate.isBefore(middle)).toBeFalsy()
    expect(duplicate.isEqualOrBefore(middle)).toBeTruthy()
    expect(duplicate.isEqual(middle)).toBeTruthy()
    expect(duplicate.isEqualOrAfter(middle)).toBeTruthy()
    expect(duplicate.isAfter(middle)).toBeFalsy()

    // Comparison operators work, because we override valueOf()
    expect(before < middle).toBeTruthy()
    expect(after > middle).toBeTruthy()
    expect(duplicate <= middle).toBeTruthy()
    expect(duplicate >= middle).toBeTruthy()

    // Unfortunately equals operators don't work
    expect(duplicate === middle).toBeFalsy()
  })
})
