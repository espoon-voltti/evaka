// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from './finite-date-range'
import LocalDate from './local-date'

describe('FiniteDateRange', () => {
  it('can be parsed from JSON', () => {
    const value = FiniteDateRange.parseJson({
      start: '2020-01-20',
      end: '2020-01-21'
    })
    expect(value.start.isEqual(LocalDate.of(2020, 1, 20))).toBe(true)
    expect(value.end.isEqual(LocalDate.of(2020, 1, 21))).toBe(true)
  })
  it('becomes a simple object with ISO string endpoints when JSONified', () => {
    const json = JSON.stringify({
      dateRange: new FiniteDateRange(
        LocalDate.of(2020, 2, 1),
        LocalDate.of(2020, 3, 1)
      )
    })
    expect(JSON.parse(json)).toStrictEqual({
      dateRange: {
        start: '2020-02-01',
        end: '2020-03-01'
      }
    })
  })
  describe('intersection/overlaps', () => {
    it('returns undefined/false if there is no overlap', () => {
      function expectNoOverlap(a: FiniteDateRange, b: FiniteDateRange) {
        expect(a.intersection(b)).toBeUndefined()
        expect(b.intersection(a)).toBeUndefined()
        expect(a.overlaps(b)).toBe(false)
        expect(b.overlaps(a)).toBe(false)
      }
      //   1234
      // A --
      // B   --
      const a = new FiniteDateRange(
        LocalDate.of(2019, 1, 1),
        LocalDate.of(2019, 1, 2)
      )
      const b = new FiniteDateRange(
        LocalDate.of(2019, 1, 3),
        LocalDate.of(2019, 1, 4)
      )
      expectNoOverlap(a, b)

      //   12345
      // C --
      // D    --
      const c = new FiniteDateRange(
        LocalDate.of(2019, 1, 1),
        LocalDate.of(2019, 1, 2)
      )
      const d = new FiniteDateRange(
        LocalDate.of(2019, 1, 4),
        LocalDate.of(2019, 1, 5)
      )
      expectNoOverlap(c, d)
    })
    it('returns the intersecting date range if there is at least partial overlap', () => {
      function expectOverlap(
        a: FiniteDateRange,
        b: FiniteDateRange,
        overlap: FiniteDateRange
      ) {
        expect(a.intersection(b)?.isEqual(overlap)).toBe(true)
        expect(b.intersection(a)?.isEqual(overlap)).toBe(true)
        expect(a.overlaps(b)).toBe(true)
        expect(b.overlaps(a)).toBe(true)
      }

      //   1234
      // A ---
      // B  ---
      // X  --
      const a = new FiniteDateRange(
        LocalDate.of(2019, 1, 1),
        LocalDate.of(2019, 1, 3)
      )
      const b = new FiniteDateRange(
        LocalDate.of(2019, 1, 2),
        LocalDate.of(2019, 1, 4)
      )
      const x = new FiniteDateRange(
        LocalDate.of(2019, 1, 2),
        LocalDate.of(2019, 1, 3)
      )
      expectOverlap(a, b, x)

      //   12345
      // C ---
      // D   ---
      // Y   -
      const c = new FiniteDateRange(
        LocalDate.of(2019, 1, 1),
        LocalDate.of(2019, 1, 3)
      )
      const d = new FiniteDateRange(
        LocalDate.of(2019, 1, 3),
        LocalDate.of(2019, 1, 5)
      )
      const y = new FiniteDateRange(
        LocalDate.of(2019, 1, 3),
        LocalDate.of(2019, 1, 3)
      )
      expectOverlap(c, d, y)
    })
  })

  describe('getGaps', () => {
    const range = (startDay: number, endDay: number) =>
      new FiniteDateRange(
        LocalDate.of(2000, 1, startDay),
        LocalDate.of(2000, 1, endDay)
      )
    const parent = range(1, 30)

    it('should return parent range when no child ranges exists', () => {
      expect(parent.getGaps([])).toEqual([parent])
    })

    it('should return parent range when no child ranges exists - one day', () => {
      expect(range(1, 1).getGaps([])).toEqual([range(1, 1)])
    })

    it('should return empty array when child range covers the entire parent', () => {
      expect(parent.getGaps([range(1, 30)])).toEqual([])
    })

    it('should return empty array when child ranges cover the entire parent', () => {
      expect(parent.getGaps([range(1, 15), range(16, 30)])).toEqual([])
    })

    it('should return gap when it is at the start', () => {
      expect(parent.getGaps([range(16, 30)])).toEqual([range(1, 15)])
    })

    it('should return gap when it is at the end', () => {
      expect(parent.getGaps([range(1, 15)])).toEqual([range(16, 30)])
    })

    it('should return gap when it is in between', () => {
      expect(parent.getGaps([range(1, 10), range(20, 30)])).toEqual([
        range(11, 19)
      ])
    })

    it('should work when gap is one day long', () => {
      expect(parent.getGaps([range(1, 15), range(17, 30)])).toEqual([
        range(16, 16)
      ])
    })

    it('should return gaps in complex case', () => {
      expect(
        parent.getGaps([
          range(3, 5),
          range(8, 12),
          range(13, 15),
          range(20, 25)
        ])
      ).toEqual([range(1, 2), range(6, 7), range(16, 19), range(26, 30)])
    })
  })
})
