// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange, { mergeDateRanges } from './finite-date-range'
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

  describe('complement', () => {
    it('returns complement', () => {
      const r = range(10, 20)
      expect(r.complement(range(1, 5))).toEqual([r])
      expect(r.complement(range(21, 30))).toEqual([r])
      expect(r.complement(range(10, 20))).toEqual([])
      expect(r.complement(range(5, 25))).toEqual([])
      expect(r.complement(range(10, 15))).toEqual([range(16, 20)])
      expect(r.complement(range(15, 20))).toEqual([range(10, 14)])
      expect(r.complement(range(11, 19))).toEqual([
        range(10, 10),
        range(20, 20)
      ])
    })
  })

  const localDate = (startDay: number) => LocalDate.of(2000, 1, startDay)
  const range = (startDay: number, endDay: number) =>
    new FiniteDateRange(localDate(startDay), localDate(endDay))

  describe('getGaps', () => {
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

  describe('overlaps', () => {
    const expectOverlap = (
      start: FiniteDateRange,
      end: FiniteDateRange,
      expected: boolean
    ) => {
      expect(start.overlaps(end)).toEqual(expected)
      expect(end.overlaps(start)).toEqual(expected)
    }

    it('should overlap when there is actual overlap', () => {
      expectOverlap(range(1, 15), range(14, 27), true)
    })

    it('should overlap when adjacent', () => {
      expectOverlap(range(1, 14), range(14, 27), true)
    })

    it('should not overlap when there is a gap', () => {
      expectOverlap(range(1, 13), range(14, 27), false)
    })
  })

  describe('includes', () => {
    it('should include start day', () => {
      expect(range(7, 15).includes(localDate(7))).toEqual(true)
    })
    it('should include end day', () => {
      expect(range(7, 15).includes(localDate(15))).toEqual(true)
    })
    it('should include day within range', () => {
      expect(range(7, 15).includes(localDate(10))).toEqual(true)
    })
    it('should not include when date is before', () => {
      expect(range(7, 15).includes(localDate(1))).toEqual(false)
    })
    it('should not include when date is after', () => {
      expect(range(7, 15).includes(localDate(24))).toEqual(false)
    })
  })
})

describe('mergeDateRanges', () => {
  it('should merge ordered adjacent ranges', () => {
    const ranges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
      new FiniteDateRange(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 4))
    ]
    expect(mergeDateRanges(ranges)).toEqual([
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4))
    ])
  })

  it('should merge unordered adjacent ranges', () => {
    const ranges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 4)),
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2))
    ]
    expect(mergeDateRanges(ranges)).toEqual([
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4))
    ])
  })

  it('should not merge ranges with a gap between them', () => {
    const ranges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
      new FiniteDateRange(LocalDate.of(2020, 1, 4), LocalDate.of(2020, 1, 5))
    ]
    expect(mergeDateRanges(ranges)).toEqual([
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)),
      new FiniteDateRange(LocalDate.of(2020, 1, 4), LocalDate.of(2020, 1, 5))
    ])
  })

  it('should merge ranges with overlap', () => {
    const ranges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 3)),
      new FiniteDateRange(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 4))
    ]
    expect(mergeDateRanges(ranges)).toEqual([
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4))
    ])
  })
})
