// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from './local-time'
import TimeInterval from './time-interval'
import TimeRange from './time-range'

const testTime = (hour: number) => LocalTime.of(hour, 0)
const testInterval = (a: number, b: number | null) =>
  new TimeInterval(testTime(a), b ? testTime(b) : null)
const testRange = (a: number, b: number) =>
  new TimeRange(testTime(a), testTime(b))

describe('TimeInterval', () => {
  test('can have no end', () => {
    testInterval(16, null)
  })
  test('can end at midnight', () => {
    testInterval(20, 0)
  })

  test('start cannot be after end', () => {
    expect(() => testInterval(16, 8)).toThrow()
  })

  test('end is exclusive so start and end cannot be equal', () => {
    expect(() => testInterval(8, 8)).toThrow()
  })

  test('intersection and overlaps return undefined and false if there is no overlap', () => {
    //   01 02 03 04 05
    // A ------_
    // B       ------_
    const a = testInterval(1, 3)
    const a_ = testRange(1, 3)
    const b = testInterval(3, 5)
    const b_ = testRange(3, 5)
    expect(a.intersection(b)).toBeUndefined()
    expect(a.overlaps(b)).toBe(false)
    expect(a.intersection(b_)).toBeUndefined()
    expect(a.overlaps(b_)).toBe(false)
    expect(b.intersection(a)).toBeUndefined()
    expect(b.overlaps(a)).toBe(false)
    expect(b.intersection(a_)).toBeUndefined()
    expect(b.overlaps(a_)).toBe(false)

    //   01 02 03 04 05 06
    // C ------_
    // D          ------_
    const c = testInterval(1, 3)
    const c_ = testRange(1, 3)
    const d = testInterval(4, 6)
    const d_ = testRange(4, 6)
    expect(c.intersection(d)).toBeUndefined()
    expect(c.overlaps(d)).toBe(false)
    expect(c.intersection(d_)).toBeUndefined()
    expect(c.overlaps(d_)).toBe(false)
    expect(d.intersection(c)).toBeUndefined()
    expect(d.overlaps(c)).toBe(false)
    expect(d.intersection(c_)).toBeUndefined()
    expect(d.overlaps(c_)).toBe(false)

    //   00 01 02 ...  23 00
    // E ------_
    // F               ---_
    const e = testInterval(0, 2)
    const e_ = testInterval(0, 2)
    const f = testInterval(23, 0)
    const f_ = testInterval(23, 0)
    expect(e.intersection(f)).toBeUndefined()
    expect(e.overlaps(f)).toBe(false)
    expect(e.intersection(f_)).toBeUndefined()
    expect(e.overlaps(f_)).toBe(false)
    expect(f.intersection(e)).toBeUndefined()
    expect(f.overlaps(e)).toBe(false)
    expect(f.intersection(e_)).toBeUndefined()
    expect(f.overlaps(e_)).toBe(false)
  })

  test('intersection and overlaps work when there is overlap', () => {
    //   01 02 03 04 05
    // A ---------_
    // B    ---------_
    // X    ---------_
    const a = testInterval(1, 4)
    const a_ = testRange(1, 4)
    const b = testInterval(2, 5)
    const b_ = testRange(2, 5)
    const x = testInterval(2, 4)
    expect(a.intersection(b)?.isEqual(x)).toBe(true)
    expect(a.overlaps(b)).toBe(true)
    expect(a.intersection(b_)?.isEqual(x)).toBe(true)
    expect(a.overlaps(b_)).toBe(true)
    expect(b.intersection(a)?.isEqual(x)).toBe(true)
    expect(b.overlaps(a)).toBe(true)
    expect(b.intersection(a_)?.isEqual(x)).toBe(true)
    expect(b.overlaps(a_)).toBe(true)

    //   01 02 03 04 05 06
    // C ---------_
    // D       ---------_
    // Y       ---_
    const c = testInterval(1, 4)
    const c_ = testRange(1, 4)
    const d = testInterval(3, 6)
    const d_ = testRange(3, 6)
    const y = testInterval(3, 4)
    expect(c.intersection(d)?.isEqual(y)).toBe(true)
    expect(c.overlaps(d)).toBe(true)
    expect(c.intersection(d_)?.isEqual(y)).toBe(true)
    expect(c.overlaps(d_)).toBe(true)
    expect(d.intersection(c)?.isEqual(y)).toBe(true)
    expect(d.overlaps(c)).toBe(true)
    expect(d.intersection(c_)?.isEqual(y)).toBe(true)
    expect(d.overlaps(c_)).toBe(true)

    //   01 02 03 04 05 06
    // E ---------------_
    // F    ---------_
    // Z    ---------_
    const e = testInterval(1, 6)
    const e_ = testRange(1, 6)
    const f = testInterval(2, 5)
    const f_ = testRange(2, 5)
    const z = testInterval(2, 5)
    expect(e.intersection(f)?.isEqual(z)).toBe(true)
    expect(e.overlaps(f)).toBe(true)
    expect(e.intersection(f_)?.isEqual(z)).toBe(true)
    expect(e.overlaps(f_)).toBe(true)
    expect(f.intersection(e)?.isEqual(z)).toBe(true)
    expect(f.overlaps(e)).toBe(true)
    expect(f.intersection(e_)?.isEqual(z)).toBe(true)
    expect(f.overlaps(e_)).toBe(true)
  })

  test('a range intersects fully with itself', () => {
    //   01 02 03 04 05 06
    // X ---------------_
    // X ---------------_
    const x = testInterval(1, 6)
    expect(x.intersection(x)).toEqual(x)
  })
})
