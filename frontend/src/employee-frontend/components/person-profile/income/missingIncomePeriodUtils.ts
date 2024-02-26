// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'

import FiniteDateRange, { mergeDateRanges } from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

type IncomeDates = { validFrom: LocalDate; validTo: LocalDate | null }

const getDateRangeBetweenIncomes = (
  firstIncome: IncomeDates,
  laterIncome: IncomeDates
): FiniteDateRange | undefined => {
  const start = firstIncome.validTo?.addDays(1)
  const end = laterIncome.validFrom.subDays(1)
  if (start?.isBefore(laterIncome.validFrom)) {
    return new FiniteDateRange(start, end)
  }
  return undefined
}

export const getMissingIncomePeriodsString = (
  incomes: IncomeDates[],
  placementRanges: FiniteDateRange[],
  andString: string
): string => {
  const sortedIncomeRanges = sortBy(incomes, (i) => i.validFrom.formatIso())

  const missingIncomeRanges = sortedIncomeRanges
    .slice(0, -1)
    .map((_, i) =>
      getDateRangeBetweenIncomes(
        sortedIncomeRanges[i],
        sortedIncomeRanges[i + 1]
      )
    )
    .filter((r): r is FiniteDateRange => !!r)
    // only keep date ranges that overlap with some placement range
    .reduce<FiniteDateRange[]>(
      (ranges, range) => [
        ...ranges,
        ...placementRanges
          .map((r) => r.intersection(range))
          .filter((r): r is FiniteDateRange => !!r)
      ],
      []
    )

  const formattedPeriods = mergeDateRanges(missingIncomeRanges).map((range) =>
    range.formatCompact()
  )

  return (
    formattedPeriods.slice(0, -2).concat(['']).join(', ') +
    formattedPeriods.slice(-2).join(` ${andString} `)
  )
}
