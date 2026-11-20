// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import DateTimeRange from './date-time-range'
import FiniteDateTimeRange from './finite-date-time-range'
import HelsinkiDateTime from './helsinki-date-time'

const start = HelsinkiDateTime.of(2020, 5, 7, 13, 59)
const end = HelsinkiDateTime.of(2020, 5, 8, 8, 0)

const at = (hour: number) => HelsinkiDateTime.of(2019, 1, 1, hour, 0)
const range = (from: number, to: number | null) =>
  new DateTimeRange(at(from), to === null ? null : at(to))
const finiteRange = (from: number, to: number) =>
  new FiniteDateTimeRange(at(from), at(to))

describe('DateTimeRange', () => {
  it('cannot end before it starts', () => {
    expect(() => new DateTimeRange(end, start)).toThrow()
  })

  it('can be parsed from JSON', () => {
    const value = DateTimeRange.parseJson({
      start: '2020-05-07T13:59:00+03:00',
      end: '2020-05-08T08:00:00+03:00'
    })
    expect(value.start.isEqual(start)).toBe(true)
    expect(value.end?.isEqual(end)).toBe(true)

    const valueWithoutEnd = DateTimeRange.parseJson({
      start: '2020-05-07T13:59:00+03:00',
      end: null
    })
    expect(valueWithoutEnd.start.isEqual(start)).toBe(true)
    expect(valueWithoutEnd.end).toBeNull()
  })

  it('becomes a simple object with ISO string endpoints when JSONified', () => {
    const json = JSON.stringify({
      withEnd: new DateTimeRange(start, end),
      withoutEnd: new DateTimeRange(start, null)
    })
    expect(JSON.parse(json)).toStrictEqual({
      withEnd: { start: start.formatIso(), end: end.formatIso() },
      withoutEnd: { start: start.formatIso(), end: null }
    })
  })

  it('includes its start but not its end', () => {
    const r = range(1, 10)
    expect(r.includes(at(1).subMinutes(1))).toBe(false)
    expect(r.includes(at(1))).toBe(true)
    expect(r.includes(at(10).subMinutes(1))).toBe(true)
    expect(r.includes(at(10))).toBe(false)

    const withoutEnd = range(5, null)
    expect(withoutEnd.includes(at(5).subMinutes(1))).toBe(false)
    expect(withoutEnd.includes(at(5))).toBe(true)
    expect(withoutEnd.includes(HelsinkiDateTime.of(9999, 12, 31, 23, 59))).toBe(
      true
    )
  })

  it('operations on ranges with an end match FiniteDateTimeRange', () => {
    const pairs: [FiniteDateTimeRange, FiniteDateTimeRange][] = [
      // adjacent
      [finiteRange(1, 3), finiteRange(3, 5)],
      // gap between
      [finiteRange(1, 3), finiteRange(4, 6)],
      // partial overlap
      [finiteRange(1, 4), finiteRange(2, 6)],
      // one within the other
      [finiteRange(1, 6), finiteRange(2, 5)],
      // equal
      [finiteRange(1, 6), finiteRange(1, 6)]
    ]
    for (const [a, b] of [...pairs, ...pairs.map(([a, b]) => [b, a])]) {
      const x = a.asDateTimeRange()
      const description = `${a.start.format()}-${a.end.format()}, ${b.start.format()}-${b.end.format()}`
      expect(x.overlaps(b), description).toBe(a.overlaps(b))
      expect(x.overlaps(b.asDateTimeRange()), description).toBe(a.overlaps(b))
      expect(x.contains(b), description).toBe(a.contains(b))
      expect(x.contains(b.asDateTimeRange()), description).toBe(a.contains(b))
      expect(x.intersection(b), description).toStrictEqual(a.intersection(b))
      expect(x.intersection(b.asDateTimeRange()), description).toStrictEqual(
        a.intersection(b)?.asDateTimeRange()
      )
    }
  })

  it('overlaps the ranges that end after its start when it has no end', () => {
    //   1234567
    // A   ----->
    // B --_
    // C ---_
    // D     --_
    // E     --->
    const a = range(3, null)
    const b = range(1, 3)
    const c = range(1, 4)
    const d = range(5, 7)
    const e = range(5, null)
    expect(a.overlaps(b)).toBe(false)
    expect(b.overlaps(a)).toBe(false)
    expect(a.overlaps(c)).toBe(true)
    expect(c.overlaps(a)).toBe(true)
    expect(a.overlaps(d)).toBe(true)
    expect(d.overlaps(a)).toBe(true)
    expect(a.overlaps(e)).toBe(true)
    expect(e.overlaps(a)).toBe(true)
  })

  it('intersection with a range without an end', () => {
    //   1234567
    // A   ----->
    // B --_
    // C -----_
    // D     --->
    const a = range(3, null)
    const b = range(1, 3)
    const c = range(1, 6)
    const d = range(5, null)
    expect(a.intersection(b)).toBeUndefined()
    expect(b.intersection(a)).toBeUndefined()
    expect(a.intersection(c)).toStrictEqual(range(3, 6))
    expect(c.intersection(a)).toStrictEqual(range(3, 6))
    expect(a.intersection(finiteRange(1, 6))).toStrictEqual(finiteRange(3, 6))
    expect(a.intersection(d)).toStrictEqual(d)
    expect(d.intersection(a)).toStrictEqual(d)
  })

  it('contains the ranges that start at or after its start when it has no end', () => {
    //   1234567
    // A   ----->
    // B ---_
    // C    --_
    // D     --->
    const a = range(3, null)
    const b = range(1, 4)
    const c = range(4, 6)
    const d = range(5, null)
    expect(a.contains(a)).toBe(true)
    expect(a.contains(b)).toBe(false)
    expect(a.contains(c)).toBe(true)
    expect(c.contains(a)).toBe(false)
    expect(a.contains(d)).toBe(true)
    expect(d.contains(a)).toBe(false)
  })
})
