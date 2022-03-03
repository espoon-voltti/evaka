// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import { getMissingIncomePeriodsString } from './missingIncomePeriodUtils'

describe('getMissingIncomePeriodsString', () => {
  it('produces dates between two incomes', () => {
    const incomeDates = [
      {
        validFrom: LocalDate.of(2020, 1, 1),
        validTo: LocalDate.of(2020, 1, 31)
      },
      {
        validFrom: LocalDate.of(2020, 3, 1),
        validTo: LocalDate.of(2020, 3, 31)
      }
    ]
    const placementRanges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31))
    ]
    expect(
      getMissingIncomePeriodsString(incomeDates, placementRanges, '&')
    ).toEqual('01.-29.02.2020')
  })

  it('produces dates only between incomes', () => {
    const incomeDates = [
      {
        validFrom: LocalDate.of(2020, 1, 10),
        validTo: LocalDate.of(2020, 1, 20)
      },
      {
        validFrom: LocalDate.of(2020, 3, 2),
        validTo: LocalDate.of(2020, 3, 30)
      }
    ]
    const placementRanges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31))
    ]
    expect(
      getMissingIncomePeriodsString(incomeDates, placementRanges, '&')
    ).toEqual('21.01.-01.03.2020')
  })

  it('produces nothing with only one income', () => {
    const incomeDates = [
      {
        validFrom: LocalDate.of(2020, 2, 1),
        validTo: LocalDate.of(2020, 2, 28)
      }
    ]
    const placementRanges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31))
    ]
    expect(
      getMissingIncomePeriodsString(incomeDates, placementRanges, '&')
    ).toEqual('')
  })

  it('produces nothing with a gap between two incomes and placement periods', () => {
    const incomeDates = [
      {
        validFrom: LocalDate.of(2020, 1, 1),
        validTo: LocalDate.of(2020, 1, 31)
      },
      {
        validFrom: LocalDate.of(2020, 3, 1),
        validTo: LocalDate.of(2020, 3, 31)
      }
    ]
    const placementRanges = [
      new FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 20)),
      new FiniteDateRange(LocalDate.of(2020, 3, 10), LocalDate.of(2020, 3, 31))
    ]
    expect(
      getMissingIncomePeriodsString(incomeDates, placementRanges, '&')
    ).toEqual('')
  })
})
