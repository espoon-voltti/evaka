// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import { isChangeRetroactive } from './RetroactiveConfirmation'

describe('isChangeRetroactive', () => {
  const today = LocalDate.of(2022, 6, 10)

  it('is not retroactive if new range is not yet valid', () => {
    expect(
      isChangeRetroactive(
        null,
        new DateRange(LocalDate.of(2000, 1, 1), null),
        false,
        today
      )
    ).toBeFalsy()
  })

  describe('creation', () => {
    it('is retroactive if new range starts before cutoff', () => {
      expect(
        isChangeRetroactive(
          new DateRange(LocalDate.of(2022, 5, 31), null),
          null,
          false,
          today
        )
      ).toBeTruthy()
    })
    it('is not retroactive if new range starts after cutoff', () => {
      expect(
        isChangeRetroactive(
          new DateRange(LocalDate.of(2022, 6, 1), null),
          null,
          false,
          today
        )
      ).toBeFalsy()
    })
  })

  describe('update', () => {
    describe('prev range before cutoff', () => {
      const prevRange = new FiniteDateRange(
        LocalDate.of(2022, 5, 1),
        LocalDate.of(2022, 5, 20)
      )

      it('is not retroactive if nothing changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, false, today)
        ).toBeFalsy()
      })
      it('is retroactive if content changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, true, today)
        ).toBeTruthy()
      })
      it('is retroactive if start date changes', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start.addDays(1), prevRange.end),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if end date changes', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start, prevRange.end.addDays(1)),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if moved to future', () => {
        expect(
          isChangeRetroactive(
            new DateRange(today, today.addDays(1)),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
    })

    describe('prev range overlaps cutoff', () => {
      const prevRange = new FiniteDateRange(
        LocalDate.of(2022, 5, 1),
        LocalDate.of(2022, 6, 5)
      )

      it('is not retroactive if nothing changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, false, today)
        ).toBeFalsy()
      })
      it('is retroactive if content changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, true, today)
        ).toBeTruthy()
      })
      it('is retroactive if start date changes and remains before cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start.addDays(1), prevRange.end),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if start date changes to after cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(LocalDate.of(2022, 6, 1), prevRange.end),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if end date changes to be before cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start, LocalDate.of(2022, 5, 30)),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is not retroactive if end date changes but remains after cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start, LocalDate.of(2022, 6, 1)),
            prevRange,
            false,
            today
          )
        ).toBeFalsy()
      })
    })

    describe('prev range after cutoff', () => {
      const prevRange = new FiniteDateRange(
        LocalDate.of(2022, 6, 5),
        LocalDate.of(2022, 6, 30)
      )

      it('is not retroactive if nothing changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, false, today)
        ).toBeFalsy()
      })
      it('is not retroactive if content changes', () => {
        expect(
          isChangeRetroactive(prevRange, prevRange, true, today)
        ).toBeFalsy()
      })
      it('is not retroactive if start date changes and remains after cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(LocalDate.of(2022, 6, 1), prevRange.end),
            prevRange,
            false,
            today
          )
        ).toBeFalsy()
      })
      it('is retroactive if start date changes to before cutoff', () => {
        expect(
          isChangeRetroactive(
            new DateRange(LocalDate.of(2022, 5, 31), prevRange.end),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if start date changes to before cutoff and end date changes', () => {
        expect(
          isChangeRetroactive(
            new DateRange(LocalDate.of(2022, 5, 31), prevRange.end.addDays(1)),
            prevRange,
            false,
            today
          )
        ).toBeTruthy()
      })
      it('is retroactive if start date changes to before cutoff and content changes', () => {
        expect(
          isChangeRetroactive(
            new DateRange(LocalDate.of(2022, 5, 31), prevRange.end),
            prevRange,
            true,
            today
          )
        ).toBeTruthy()
      })
      it('is not retroactive if only end date changes', () => {
        expect(
          isChangeRetroactive(
            new DateRange(prevRange.start, prevRange.end.addDays(1)),
            prevRange,
            false,
            today
          )
        ).toBeFalsy()
      })
    })
  })
})
