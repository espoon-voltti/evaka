// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  isValidRange,
  parseDate,
  parseTzDate,
  stringToDate
} from '@evaka/lib-common/src/utils/date'

describe('dateUtils', () => {
  describe('parseDate', () => {
    it('parses YYYY-MM-DD formatted datestring correctly', () => {
      expect(parseDate('2008-02-13')).toEqual(new Date(2008, 2 - 1, 13)) // months are 0-based
    })

    it('parses full ISO-8601 from only its datestring parts', () => {
      expect(parseDate('2008-02-13T13:13:13.000Z')).toEqual(
        new Date(2008, 2 - 1, 13)
      ) // months are 0-based
    })

    it('returns invalid date for invalid datestring', () => {
      expect(Number(parseDate('this is invalid'))).toEqual(
        Number(new Date(NaN))
      )
    })
  })

  describe('stringToDate', () => {
    it('parses YYYY-MM-DD formatted datestring correctly', () => {
      expect(stringToDate('2019-02-13')).toEqual(new Date(2019, 2 - 1, 13))
    })

    it('is given incorrect date, returns null', () => {
      expect(stringToDate('2019-02-30')).toEqual(null)
    })

    it('is given null, returns null', () => {
      expect(stringToDate(null)).toEqual(null)
    })

    it('is given undefined, returns null', () => {
      expect(stringToDate(undefined)).toEqual(null)
    })
  })

  describe('parseTzDate', () => {
    it('parses ISO-8601 formatted formatted string correctly', () => {
      expect(parseTzDate(new Date('2019-02-13T22:00:00.000Z'))).toEqual(
        '2019-02-14'
      )
    })

    it('is given null, returns null', () => {
      expect(parseTzDate(null)).toEqual(null)
    })
  })

  describe('isValidRange', () => {
    it('returns true when a valid end date is after a valid start date', () => {
      expect(
        isValidRange(new Date('2019-02-13'), new Date('2019-02-17'))
      ).toEqual(true)
    })

    it('returns false when valid start & end dates are the same', () => {
      expect(
        isValidRange(new Date('2019-02-13'), new Date('2019-02-13'))
      ).toEqual(false)
    })

    it('returns false when a valid end date is before a valid start date', () => {
      expect(
        isValidRange(new Date('2019-02-13'), new Date('2019-02-12'))
      ).toEqual(false)
    })

    it('returns false when start date is invalid', () => {
      expect(
        isValidRange(new Date('2019-32-32'), new Date('2019-02-12'))
      ).toEqual(false)
    })

    it('returns false when end date is invalid', () => {
      expect(isValidRange(new Date('2019-02-12'), new Date('date'))).toEqual(
        false
      )
    })

    it('returns false when start date is null', () => {
      expect(isValidRange(null, new Date('2019-02-13'))).toEqual(false)
    })

    it('returns false when end date is null', () => {
      expect(isValidRange(new Date('2019-02-13'), null)).toEqual(false)
    })

    it('returns false when start & end dates are null', () => {
      expect(isValidRange(null, null)).toEqual(false)
    })
  })
})
