// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from './date-range'
import LocalDate from './local-date'

describe('DateRange', () => {
  describe('with finite end', () => {
    it('can be parsed from JSON', () => {
      const value = DateRange.parseJson({
        start: '2020-01-20',
        end: '2020-01-21'
      })
      expect(value.start.isEqual(LocalDate.of(2020, 1, 20))).toBe(true)
      expect(value.end?.isEqual(LocalDate.of(2020, 1, 21))).toBe(true)
    })
    it('becomes a simple object with ISO string endpoints when JSONified', () => {
      const json = JSON.stringify({
        dateRange: new DateRange(
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
  })
  describe('with infinite end', () => {
    it('can be parsed from JSON', () => {
      const value = DateRange.parseJson({
        start: '2021-01-01',
        end: null
      })
      expect(value.start.isEqual(LocalDate.of(2021, 1, 1))).toBe(true)
      expect(value.end).toBeNull()
    })
    it('becomes a simple object with ISO string endpoints when JSONified', () => {
      const json = JSON.stringify({
        dateRange: new DateRange(LocalDate.of(2021, 1, 1), null)
      })
      expect(JSON.parse(json)).toStrictEqual({
        dateRange: {
          start: '2021-01-01',
          end: null
        }
      })
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
      expect(r.complement(range(5, null))).toEqual([])
      expect(r.complement(range(15, null))).toEqual([range(10, 14)])
      expect(r.complement(range(20, null))).toEqual([range(10, 19)])
      expect(r.complement(range(21, null))).toEqual([range(10, 20)])

      const r2 = range(10, null)
      expect(r2.complement(range(5, null))).toEqual([])
      expect(r2.complement(range(10, null))).toEqual([])
      expect(r2.complement(range(15, null))).toEqual([range(10, 14)])
    })
  })

  describe('contains', () => {
    it('returns if range fully contains another range', () => {
      const r = range(10, 20)
      expect(r.contains(range(9, 19))).toEqual(false)
      expect(r.contains(range(9, 20))).toEqual(false)
      expect(r.contains(range(9, 21))).toEqual(false)
      expect(r.contains(range(9, null))).toEqual(false)

      expect(r.contains(range(10, 19))).toEqual(true)
      expect(r.contains(range(10, 20))).toEqual(true)
      expect(r.contains(range(10, 21))).toEqual(false)
      expect(r.contains(range(10, null))).toEqual(false)

      expect(r.contains(range(11, 19))).toEqual(true)
      expect(r.contains(range(11, 20))).toEqual(true)
      expect(r.contains(range(11, 21))).toEqual(false)
      expect(r.contains(range(11, null))).toEqual(false)

      const r2 = range(10, null)
      expect(r2.contains(range(9, 20))).toEqual(false)
      expect(r2.contains(range(9, null))).toEqual(false)
      expect(r2.contains(range(10, 20))).toEqual(true)
      expect(r2.contains(range(10, null))).toEqual(true)
      expect(r2.contains(range(11, 20))).toEqual(true)
      expect(r2.contains(range(11, null))).toEqual(true)
    })
  })
})

const localDate = (startDay: number) => LocalDate.of(2000, 1, startDay)

const range = (startDay: number, endDay: number | null) =>
  new DateRange(localDate(startDay), endDay !== null ? localDate(endDay) : null)
