// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from './local-time'

describe('LocalTime', () => {
  it('can be formatted into an ISO local time string with nanosecond precision', () => {
    const time = LocalTime.of(1, 2, 3, 123456789)
    expect(time.formatIso()).toStrictEqual('01:02:03.123456789')
  })
  it('can be converted to/from JSON', () => {
    const time = LocalTime.of(1, 2, 3, 123456789)
    expect(LocalTime.parseIso(time.toJSON()).isEqual(time)).toBeTruthy()
  })
  it('has property getters for each part of the time', () => {
    const parts = [1, 2, 3, 123456789] as const
    const [hour, minute, second, nanosecond] = parts
    const timestamp = LocalTime.of(...parts)
    expect(timestamp.hour).toStrictEqual(hour)
    expect(timestamp.minute).toStrictEqual(minute)
    expect(timestamp.second).toStrictEqual(second)
    expect(timestamp.nanosecond).toStrictEqual(nanosecond)
  })
  it('supports comparisons', () => {
    const middle = LocalTime.of(15, 0, 1, 150)

    const before = LocalTime.of(15, 0, 1, 149)
    expect(before.isBefore(middle)).toBeTruthy()
    expect(before.isEqualOrBefore(middle)).toBeTruthy()
    expect(before.isEqual(middle)).toBeFalsy()
    expect(before.isEqualOrAfter(middle)).toBeFalsy()
    expect(before.isAfter(middle)).toBeFalsy()

    const after = LocalTime.of(15, 0, 1, 151)
    expect(after.isBefore(middle)).toBeFalsy()
    expect(after.isEqualOrBefore(middle)).toBeFalsy()
    expect(after.isEqual(middle)).toBeFalsy()
    expect(after.isEqualOrAfter(middle)).toBeTruthy()
    expect(after.isAfter(middle)).toBeTruthy()

    const duplicate = LocalTime.parseIso(middle.formatIso())
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
    expect(duplicate == middle).toBeFalsy()
    expect(duplicate === middle).toBeFalsy()
  })
})
