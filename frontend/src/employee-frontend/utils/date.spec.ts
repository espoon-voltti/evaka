// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import {
  rangesOverlap,
  formatDate,
  autoComplete,
  getGaps,
  DateRange
} from '../utils/date'

describe('utils/date', () => {
  describe('rangesOverlap', () => {
    //          xxxxxxxxxx
    // xxxxxxxxx
    // false
    it('should return correct result, #1', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 1),
          endDate: LocalDate.of(2000, 1, 9)
        }
      )
      expect(overlap).toBe(false)
    })

    //          xxxxxxxxxx
    // xxxxxxxxxx
    // true
    it('should return correct result, #2', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 1),
          endDate: LocalDate.of(2000, 1, 10)
        }
      )
      expect(overlap).toBe(true)
    })
    //          xxxxxxxxxx
    // xxxxxxxxxxxxxxx
    // true
    it('should return correct result, #3', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 1),
          endDate: LocalDate.of(2000, 1, 15)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    // xxxxxxxxxxxxxxxxxxxxxx
    // true
    it('should return correct result, #4', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 1),
          endDate: LocalDate.of(2000, 1, 23)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //          xxx
    // true
    it('should return correct result, #5', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 12)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //          xxxxxxxxxxxxx
    // true
    it('should return correct result, #6', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 23)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //             xx
    // true
    it('should return correct result, #7', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 13),
          endDate: LocalDate.of(2000, 1, 15)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //             xxxxxxxxx
    // true
    it('should return correct result, #8', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 13),
          endDate: LocalDate.of(2000, 1, 22)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //                   xxx
    // true
    it('should return correct result, #9', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 20),
          endDate: LocalDate.of(2000, 1, 22)
        }
      )
      expect(overlap).toBe(true)
    })

    //          xxxxxxxxxx
    //                    xxx
    // false
    it('should return correct result, #10', () => {
      const overlap = rangesOverlap(
        {
          startDate: LocalDate.of(2000, 1, 10),
          endDate: LocalDate.of(2000, 1, 20)
        },
        {
          startDate: LocalDate.of(2000, 1, 21),
          endDate: LocalDate.of(2000, 1, 23)
        }
      )
      expect(overlap).toBe(false)
    })
  })

  describe('formatDate', () => {
    it('should format valid date correctly', () => {
      expect(formatDate(new Date('2019-01-01'))).toBe('01.01.2019')
    })

    it('should format undefined to empty string', () => {
      expect(formatDate(undefined)).toBe('')
    })
  })

  describe('autoComplete', () => {
    it('should add period when given two digit day part', () => {
      expect(autoComplete('01')).toBe('01.')
      expect(autoComplete('10')).toBe('10.')
      expect(autoComplete('11')).toBe('11.')
    })

    it('should not add period when given one digit day part', () => {
      expect(autoComplete('0')).toBe('0')
      expect(autoComplete('1')).toBe('1')
      expect(autoComplete('9')).toBe('9')
    })

    it('should add period and century when given two digit day and month parts', () => {
      expect(autoComplete('01.01')).toBe('01.01.20')
      expect(autoComplete('01.10')).toBe('01.10.20')
      expect(autoComplete('01.11')).toBe('01.11.20')
    })

    it('should not add anything when given two digit day and one digit month part', () => {
      expect(autoComplete('01.0')).toBe('01.0')
      expect(autoComplete('01.1')).toBe('01.1')
      expect(autoComplete('01.9')).toBe('01.9')
    })

    it('should add century when given two digit day and month parts with period at the end', () => {
      expect(autoComplete('01.01.')).toBe('01.01.20')
      expect(autoComplete('01.10.')).toBe('01.10.20')
      expect(autoComplete('01.11.')).toBe('01.11.20')
    })

    it('should add century when given two digit day and month parts with period and millenium at the end', () => {
      expect(autoComplete('01.01.2')).toBe('01.01.20')
      expect(autoComplete('01.10.2')).toBe('01.10.20')
      expect(autoComplete('01.11.2')).toBe('01.11.20')
    })
  })

  describe('getGaps', () => {
    const range = (startDay: number, endDay: number): DateRange => ({
      startDate: LocalDate.of(2000, 1, startDay),
      endDate: LocalDate.of(2000, 1, endDay)
    })
    const parent = range(1, 30)

    it('should return parent range when no child ranges exists', () => {
      expect(getGaps([], parent)).toEqual([parent])
    })

    it('should return empty array when child range covers the entire parent', () => {
      expect(getGaps([range(1, 30)], parent)).toEqual([])
    })

    it('should return empty array when child ranges cover the entire parent', () => {
      expect(getGaps([range(1, 15), range(16, 30)], parent)).toEqual([])
    })

    it('should return gap when it is at the start', () => {
      expect(getGaps([range(16, 30)], parent)).toEqual([range(1, 15)])
    })

    it('should return gap when it is at the end', () => {
      expect(getGaps([range(1, 15)], parent)).toEqual([range(16, 30)])
    })

    it('should return gap when it is in between', () => {
      expect(getGaps([range(1, 10), range(20, 30)], parent)).toEqual([
        range(11, 19)
      ])
    })

    it('should return gaps in complex case', () => {
      expect(
        getGaps(
          [range(3, 5), range(8, 12), range(13, 15), range(20, 25)],
          parent
        )
      ).toEqual([range(1, 2), range(6, 7), range(16, 19), range(26, 30)])
    })
  })
})
