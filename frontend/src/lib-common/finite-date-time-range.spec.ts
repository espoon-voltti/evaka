// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import DateTimeRange from './date-time-range'
import FiniteDateTimeRange from './finite-date-time-range'
import HelsinkiDateTime from './helsinki-date-time'
import LocalDate from './local-date'

const start = HelsinkiDateTime.of(2020, 5, 7, 13, 59)
const end = HelsinkiDateTime.of(2020, 5, 8, 8, 0)

const at = (hour: number) => HelsinkiDateTime.of(2019, 1, 1, hour, 0)
const range = (from: number, to: number) =>
  new FiniteDateTimeRange(at(from), at(to))

describe('FiniteDateTimeRange', () => {
  it('cannot end before it starts', () => {
    expect(() => new FiniteDateTimeRange(end, start)).toThrow()
  })

  it('can be parsed from JSON', () => {
    const value = FiniteDateTimeRange.parseJson({
      start: '2020-05-07T13:59:00+03:00',
      end: '2020-05-08T08:00:00+03:00'
    })
    expect(value.start.isEqual(start)).toBe(true)
    expect(value.end.isEqual(end)).toBe(true)
  })

  it('becomes a simple object with ISO string endpoints when JSONified', () => {
    const json = JSON.stringify({ range: new FiniteDateTimeRange(start, end) })
    expect(JSON.parse(json)).toStrictEqual({
      range: { start: start.formatIso(), end: end.formatIso() }
    })
  })

  it('ofDate covers the whole date, also on the days when DST starts or ends', () => {
    const date = LocalDate.of(2024, 5, 7)
    expect(FiniteDateTimeRange.ofDate(date)).toStrictEqual(
      new FiniteDateTimeRange(
        HelsinkiDateTime.of(2024, 5, 7, 0, 0),
        HelsinkiDateTime.of(2024, 5, 8, 0, 0)
      )
    )
    expect(FiniteDateTimeRange.ofDate(date).durationInMinutes()).toBe(24 * 60)
    expect(
      FiniteDateTimeRange.ofDate(LocalDate.of(2024, 3, 31)).durationInMinutes()
    ).toBe(23 * 60)
    expect(
      FiniteDateTimeRange.ofDate(LocalDate.of(2024, 10, 27)).durationInMinutes()
    ).toBe(25 * 60)
  })

  it('includes its start but not its end', () => {
    const r = range(1, 10)
    expect(r.includes(at(1).subMinutes(1))).toBe(false)
    expect(r.includes(at(1))).toBe(true)
    expect(r.includes(at(10).subMinutes(1))).toBe(true)
    expect(r.includes(at(10))).toBe(false)
  })

  it('overlaps, contains and intersection', () => {
    const cases = [
      {
        description: 'adjacent',
        a: range(1, 3),
        b: range(3, 5),
        overlaps: false,
        aContainsB: false,
        bContainsA: false,
        intersection: undefined
      },
      {
        description: 'gap between',
        a: range(1, 3),
        b: range(4, 6),
        overlaps: false,
        aContainsB: false,
        bContainsA: false,
        intersection: undefined
      },
      {
        description: 'partial overlap',
        a: range(1, 4),
        b: range(2, 6),
        overlaps: true,
        aContainsB: false,
        bContainsA: false,
        intersection: range(2, 4)
      },
      {
        description: 'one within the other',
        a: range(1, 6),
        b: range(2, 5),
        overlaps: true,
        aContainsB: true,
        bContainsA: false,
        intersection: range(2, 5)
      },
      {
        description: 'equal',
        a: range(1, 6),
        b: range(1, 6),
        overlaps: true,
        aContainsB: true,
        bContainsA: true,
        intersection: range(1, 6)
      }
    ]
    for (const c of cases) {
      expect(c.a.overlaps(c.b), c.description).toBe(c.overlaps)
      expect(c.b.overlaps(c.a), c.description).toBe(c.overlaps)
      expect(c.a.contains(c.b), c.description).toBe(c.aContainsB)
      expect(c.b.contains(c.a), c.description).toBe(c.bContainsA)
      expect(c.a.intersection(c.b), c.description).toStrictEqual(c.intersection)
      expect(c.b.intersection(c.a), c.description).toStrictEqual(c.intersection)
    }
  })

  it('overlaps, contains and intersection with a DateTimeRange without an end', () => {
    //   1234567
    // A   ---_
    // B   ----->
    // C      -->
    const a = range(3, 6)
    const b = new DateTimeRange(at(3), null)
    const c = new DateTimeRange(at(6), null)
    expect(a.overlaps(b)).toBe(true)
    expect(a.overlaps(c)).toBe(false)
    expect(a.contains(b)).toBe(false)
    expect(a.intersection(b)).toStrictEqual(a)
    expect(a.intersection(c)).toBeUndefined()
  })
})
