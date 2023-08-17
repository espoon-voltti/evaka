// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteTimeRange from './finite-time-range'
import LocalTime from './local-time'

describe('FiniteTimeRange', () => {
  it('can be created', () => {
    const value = FiniteTimeRange.tryCreate(
      LocalTime.of(12, 15),
      LocalTime.of(23, 59)
    )
    expect(value!.start.isEqual(LocalTime.of(12, 15, 0, 0)))
    expect(value!.end.isEqual(LocalTime.of(23, 59, 0, 0)))
  })

  it('with start/end', () => {
    const value = new FiniteTimeRange(
      LocalTime.of(12, 15),
      LocalTime.of(23, 59)
    )
    expect(value.withStart(LocalTime.of(9, 0))).toStrictEqual(
      new FiniteTimeRange(LocalTime.of(9, 0), LocalTime.of(23, 59))
    )
    expect(value.withEnd(LocalTime.of(22, 0))).toStrictEqual(
      new FiniteTimeRange(LocalTime.of(12, 15), LocalTime.of(22, 0))
    )

    try {
      new FiniteTimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)).withStart(
        LocalTime.of(12, 1)
      )
      expect(false).toBeTruthy()
      /* eslint-disable  */
    } catch (e: any) {
      expect(e.message.toString()).toStrictEqual(
        'Attempting to initialize invalid finite time range with start: 12:01:00, end: 12:00:00'
      )
    }
    /* eslint-enable */

    try {
      value.withEnd(LocalTime.of(12, 14))
      expect(false).toBeTruthy()
      /* eslint-disable  */
    } catch (e: any) {
      expect(e.message.toString()).toStrictEqual(
        'Attempting to initialize invalid finite time range with start: 12:15:00, end: 12:14:00'
      )
    }
    /* eslint-enable */
  })

  it('contains', () => {
    const container = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    expect(container.contains(container)).toBeTruthy()
    expect(
      container.contains(
        new FiniteTimeRange(LocalTime.of(10, 0), LocalTime.of(16, 0))
      )
    ).toBeTruthy()
    expect(
      container.contains(
        new FiniteTimeRange(LocalTime.of(8, 59), LocalTime.of(9, 1))
      )
    ).toBeFalsy()
  })

  it('includes', () => {
    const container = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    expect(
      container.includes(container.start) && container.includes(container.end)
    ).toBeTruthy()
    expect(container.includes(LocalTime.of(12, 0))).toBeTruthy()
    expect(container.includes(LocalTime.of(17, 1))).toBeFalsy()
  })

  it('overlaps', () => {
    const nineToFive = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    expect(nineToFive.overlaps(nineToFive)).toBeTruthy()
    expect(
      nineToFive.overlaps(
        new FiniteTimeRange(LocalTime.of(8, 59), LocalTime.of(17, 1))
      )
    ).toBeTruthy()
    expect(
      nineToFive.overlaps(
        new FiniteTimeRange(LocalTime.of(8, 59), LocalTime.of(9, 0))
      )
    ).toBeTruthy()
    expect(
      nineToFive.overlaps(
        new FiniteTimeRange(LocalTime.of(17, 0), LocalTime.of(17, 1))
      )
    ).toBeTruthy()
    expect(
      nineToFive.overlaps(
        new FiniteTimeRange(LocalTime.of(8, 58), LocalTime.of(8, 59))
      )
    ).toBeFalsy()
    expect(
      nineToFive.overlaps(
        new FiniteTimeRange(LocalTime.of(17, 1), LocalTime.of(17, 2))
      )
    ).toBeFalsy()
  })

  it('intersection', () => {
    const nineToFive = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    expect(nineToFive.intersection(nineToFive)).toStrictEqual(nineToFive)

    expect(
      nineToFive.intersection(
        new FiniteTimeRange(LocalTime.of(8, 58), LocalTime.of(9, 1))
      )
    ).toStrictEqual(new FiniteTimeRange(LocalTime.of(9, 0), LocalTime.of(9, 1)))

    expect(
      nineToFive.intersection(
        new FiniteTimeRange(LocalTime.of(16, 59), LocalTime.of(17, 1))
      )
    ).toStrictEqual(
      new FiniteTimeRange(LocalTime.of(16, 59), LocalTime.of(17, 0))
    )

    expect(
      nineToFive.intersection(
        new FiniteTimeRange(LocalTime.of(8, 58), LocalTime.of(8, 59))
      )
    ).toBeFalsy()
    expect(
      nineToFive.intersection(
        new FiniteTimeRange(LocalTime.of(17, 1), LocalTime.of(17, 2))
      )
    ).toBeFalsy()
  })

  it('hours', () => {
    expect(
      Array.from(
        new FiniteTimeRange(LocalTime.of(9, 0), LocalTime.of(9, 0)).hours()
      )
    ).toStrictEqual([LocalTime.of(9, 0)])
    expect(
      Array.from(
        new FiniteTimeRange(LocalTime.of(9, 59), LocalTime.of(10, 59)).hours()
      )
    ).toStrictEqual([LocalTime.of(9, 59), LocalTime.of(10, 59)])
  })

  it('toString and format', () => {
    const nineToFive = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(17, 0)
    )
    expect(nineToFive.toString()).toStrictEqual('[09:00:00, 17:00:00]')
    expect(nineToFive.format('H:MM')).toStrictEqual('9:01 - 17:01')
  })

  it('', () => {
    const nineToTen = new FiniteTimeRange(
      LocalTime.of(9, 0),
      LocalTime.of(10, 0)
    )
    expect(
      nineToTen.isBeforeMaybeConnected(
        new FiniteTimeRange(LocalTime.of(10, 1), LocalTime.of(11, 0))
      )
    ).toBeTruthy()
    expect(
      nineToTen.isBeforeMaybeConnected(
        new FiniteTimeRange(LocalTime.of(10, 0), LocalTime.of(11, 0))
      )
    ).toBeTruthy()
    expect(
      nineToTen.isBeforeMaybeConnected(
        new FiniteTimeRange(LocalTime.of(9, 59), LocalTime.of(11, 0))
      )
    ).toBeFalsy()

    expect(
      nineToTen.isAfterMaybeConnected(
        new FiniteTimeRange(LocalTime.of(8, 0), LocalTime.of(8, 59))
      )
    ).toBeTruthy()
    expect(
      nineToTen.isAfterMaybeConnected(
        new FiniteTimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0))
      )
    ).toBeTruthy()
    expect(
      nineToTen.isAfterMaybeConnected(
        new FiniteTimeRange(LocalTime.of(8, 0), LocalTime.of(9, 1))
      )
    ).toBeFalsy()
  })
})
