// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from './local-time'
import TimeRange from './time-range'

const testTime = (hour: number) => LocalTime.of(hour, 0)
const testRange = (a: number, b: number) =>
  TimeRange.of(testTime(a), testTime(b))

describe('TimeRange', () => {
  test('can end at midnight', () => {
    testRange(20, 0)
  })

  test('start cannot be after end', () => {
    expect(() => testRange(16, 8)).toThrow()
  })

  test('end is exclusive so start and end cannot be equal', () => {
    expect(() => testRange(8, 8)).toThrow()
  })

  test('intersection returns undefined if there is no overlap', () => {
    //   01 02 03 04 05
    // A ------_
    // B       ------_
    const a = testRange(1, 3)
    const b = testRange(3, 5)
    expect(a.intersection(b)).toBeUndefined()
    expect(b.intersection(a)).toBeUndefined()
    expect(TimeRange.intersection([a, b])).toBeUndefined()

    //   01 02 03 04 05 06
    // C ------_
    // D          ------_
    const c = testRange(1, 3)
    const d = testRange(4, 6)
    expect(c.intersection(d)).toBeUndefined()
    expect(d.intersection(c)).toBeUndefined()
    expect(TimeRange.intersection([c, d])).toBeUndefined()

    //   00 01 02 ...  23 00
    // E ------_
    // F               ---_
    const e = testRange(0, 2)
    const f = testRange(23, 0)
    expect(e.intersection(f)).toBeUndefined()
    expect(f.intersection(e)).toBeUndefined()
    expect(TimeRange.intersection([e, f])).toBeUndefined()
  })

  test('intersection works when there is overlap', () => {
    //   01 02 03 04 05
    // A ---------_
    // B    ---------_
    // X    ---------_
    const a = testRange(1, 4)
    const b = testRange(2, 5)
    const x = testRange(2, 4)
    expect(a.intersection(b)?.isEqual(x)).toBe(true)
    expect(b.intersection(a)?.isEqual(x)).toBe(true)
    expect(TimeRange.intersection([a, b])?.isEqual(x)).toBe(true)

    //   01 02 03 04 05 06
    // C ---------_
    // D       ---------_
    // Y       ---_
    const c = testRange(1, 4)
    const d = testRange(3, 6)
    const y = testRange(3, 4)
    expect(c.intersection(d)?.isEqual(y)).toBe(true)
    expect(d.intersection(c)?.isEqual(y)).toBe(true)
    expect(TimeRange.intersection([c, d])?.isEqual(y)).toBe(true)

    //   01 02 03 04 05 06
    // E ---------------_
    // F    ---------_
    // Z    ---------_
    const e = testRange(1, 6)
    const f = testRange(2, 5)
    const z = testRange(2, 5)
    expect(e.intersection(f)?.isEqual(z)).toBe(true)
    expect(f.intersection(e)?.isEqual(z)).toBe(true)
    expect(TimeRange.intersection([e, f])?.isEqual(z)).toBe(true)
  })

  test('a range intersects fully with itself', () => {
    //   01 02 03 04 05 06
    // X ---------------_
    // X ---------------_
    const x = testRange(1, 6)
    expect(x.intersection(x)).toEqual(x)
    expect(TimeRange.intersection([x])?.isEqual(x)).toBe(true)
  })

  test('includes', () => {
    const range = testRange(8, 16)
    expect(range.includes(LocalTime.of(7, 59, 59, 999999999))).toBe(false)
    expect(range.includes(LocalTime.of(8, 0))).toBe(true)
    expect(range.includes(LocalTime.of(15, 59, 59, 999999999))).toBe(true)
    expect(range.includes(LocalTime.of(16, 0))).toBe(false)
  })

  test('includesStartOf and includesEndOf work', () => {
    //   01 02 03 04 05 06
    // A ---------_
    // B       ---------_
    const a = testRange(1, 4)
    const b = testRange(3, 6)
    expect(a.includesStartOf(b)).toBe(true)
    expect(b.includesStartOf(a)).toBe(false)
    expect(a.includesEndOf(b)).toBe(false)
    expect(b.includesEndOf(a)).toBe(true)

    //   01 02 03 04 05 06
    // C ------_
    // D       -------- _
    const c = testRange(1, 3)
    const d = testRange(3, 6)
    expect(c.includesStartOf(d)).toBe(false)
    expect(d.includesStartOf(c)).toBe(false)
    expect(c.includesEndOf(d)).toBe(false)
    expect(d.includesEndOf(c)).toBe(false)
  })
})
