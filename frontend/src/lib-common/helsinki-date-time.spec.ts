// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from './helsinki-date-time'
import LocalTime from './local-time'

describe('HelsinkiDateTime', () => {
  it('can be formatted into an ISO string with the TZ offset always based on Europe/Helsinki instead of local time zone', () => {
    const winter = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    expect(winter.formatIso()).toStrictEqual('2022-01-01T02:03:04.005+02:00')
    const summer = HelsinkiDateTime.of(2022, 6, 1, 2, 3, 4, 5)
    expect(summer.formatIso()).toStrictEqual('2022-06-01T02:03:04.005+03:00')
  })
  it('can be converted to LocalDate', () => {
    const winter = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    expect(winter.toLocalDate().formatIso()).toStrictEqual('2022-01-01')
    const summer = HelsinkiDateTime.of(2022, 6, 1, 2, 3, 4, 5)
    expect(summer.toLocalDate().formatIso()).toStrictEqual('2022-06-01')
  })
  it('can be converted to LocalTime', () => {
    const winter = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    expect(winter.toLocalTime().formatIso()).toStrictEqual('02:03:04.005000000')
    const summer = HelsinkiDateTime.of(2022, 6, 1, 2, 3, 4, 5)
    expect(summer.toLocalTime().formatIso()).toStrictEqual('02:03:04.005000000')
  })
  it('date can be changed', () => {
    const winter = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    const summer = HelsinkiDateTime.of(2022, 6, 1, 2, 3, 4, 5)
    expect(winter.withDate(summer.toLocalDate()).formatIso()).toStrictEqual(
      '2022-06-01T02:03:04.005+03:00'
    )
    expect(summer.withDate(winter.toLocalDate()).formatIso()).toStrictEqual(
      '2022-01-01T02:03:04.005+02:00'
    )
  })
  it('time can be changed', () => {
    const winter = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    const summer = HelsinkiDateTime.of(2022, 6, 1, 2, 3, 4, 5)
    const newTime = LocalTime.of(2, 1, 0, 1000000)
    expect(winter.withTime(newTime).formatIso()).toStrictEqual(
      '2022-01-01T02:01:00.001+02:00'
    )
    expect(summer.withTime(newTime).formatIso()).toStrictEqual(
      '2022-06-01T02:01:00.001+03:00'
    )
  })
  it('can be converted to/from JSON', () => {
    const timestamp = HelsinkiDateTime.of(2022, 1, 1, 2, 3, 4, 5)
    expect(
      HelsinkiDateTime.parseIso(timestamp.toJSON()).isEqual(timestamp)
    ).toBeTruthy()
  })
  it('rejects invalid dates', () => {
    expect(() => HelsinkiDateTime.of(2022, 2, 30)).toThrow(RangeError)
    expect(() => HelsinkiDateTime.of(2022, 1, 30, 25, 0)).toThrow(RangeError)
  })
  it('has property getters for each part of the timestamp', () => {
    const parts = [2022, 6, 1, 2, 3, 4, 5] as const
    const [year, month, date, hour, minute, second, millisecond] = parts
    const timestamp = HelsinkiDateTime.of(...parts)
    expect(timestamp.year).toStrictEqual(year)
    expect(timestamp.month).toStrictEqual(month)
    expect(timestamp.date).toStrictEqual(date)
    expect(timestamp.hour).toStrictEqual(hour)
    expect(timestamp.minute).toStrictEqual(minute)
    expect(timestamp.second).toStrictEqual(second)
    expect(timestamp.millisecond).toStrictEqual(millisecond)
  })
  it('supports comparisons', () => {
    const middle = HelsinkiDateTime.of(2022, 1, 1, 0, 0)

    const before = HelsinkiDateTime.of(2011, 1, 1, 0, 0)
    expect(before.isBefore(middle)).toBeTruthy()
    expect(before.isEqualOrBefore(middle)).toBeTruthy()
    expect(before.isEqual(middle)).toBeFalsy()
    expect(before.isEqualOrAfter(middle)).toBeFalsy()
    expect(before.isAfter(middle)).toBeFalsy()

    const after = HelsinkiDateTime.of(2033, 1, 1, 0, 0)
    expect(after.isBefore(middle)).toBeFalsy()
    expect(after.isEqualOrBefore(middle)).toBeFalsy()
    expect(after.isEqual(middle)).toBeFalsy()
    expect(after.isEqualOrAfter(middle)).toBeTruthy()
    expect(after.isAfter(middle)).toBeTruthy()

    const duplicate = HelsinkiDateTime.parseIso(middle.formatIso())
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
