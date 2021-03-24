{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import LocalDate from 'lib-common/local-date'
import { Income } from '../../../types/income'

interface TimePeriod {
  startDate: LocalDate
  endDate: LocalDate
}

function notEmpty<TValue>(value: TValue | null | undefined): value is TValue {
  return value !== null && value !== undefined
}

const timePeriodToString = (interval: TimePeriod): string => {
  const [startDate, endDate] = [interval.startDate, interval.endDate]
  const [equalDate, equalMonth, equalYear] = [
    startDate.getDate() === endDate.getDate(),
    startDate.getMonth() === endDate.getMonth(),
    startDate.getYear() === endDate.getYear()
  ]
  if (equalDate && equalMonth && equalYear) {
    return `${endDate.getDate()}.${endDate.getMonth()}.${endDate.getYear()}`
  } else if (equalMonth && equalYear) {
    return `${startDate.getDate()}.-${endDate.getDate()}.${endDate.getMonth()}.${endDate.getYear()}`
  } else if (equalYear) {
    return `${startDate.getDate()}.${startDate.getMonth()}-${endDate.getDate()}.${endDate.getMonth()}.${endDate.getYear()}`
  } else {
    return `${startDate.getDate()}.${startDate.getMonth()}.${startDate.getYear()}-${endDate.getDate()}.${endDate.getMonth()}.${endDate.getYear()}`
  }
}

const getTimePeriodBetweenIncomesOrUndefined = (
  incomeA: Income,
  incomeB: Income
): TimePeriod | undefined => {
  const gapStart = [incomeA.validTo, incomeB.validTo]
    .filter(notEmpty)
    .sort((dateA, dateB) => (dateA.isBefore(dateB) ? -1 : 1))[0]

  const gapEnd = incomeA.validFrom.isBefore(incomeB.validFrom)
    ? incomeB.validFrom
    : incomeA.validFrom

  return gapStart && gapStart.addDays(1).isBefore(gapEnd)
    ? {
        startDate: gapStart.addDays(1),
        endDate: gapEnd.subDays(1)
      }
    : undefined
}

export const getMissingIncomePeriodsString = (
  incomeList: Income[],
  andString: string
): string => {
  const sortedIncomePeriods = incomeList.sort((incomeA, incomeB) =>
    Math.sign(
      incomeA.validFrom.toSystemTzDate().getTime() -
        incomeB.validFrom.toSystemTzDate().getTime()
    )
  )

  const missingIncomePeriodStrings = sortedIncomePeriods
    .slice(0, -1)
    .map((_, i) =>
      getTimePeriodBetweenIncomesOrUndefined(
        sortedIncomePeriods[i],
        sortedIncomePeriods[i + 1]
      )
    )
    .filter(notEmpty)
    .map(timePeriodToString)

  return (
    missingIncomePeriodStrings.slice(0, -2).concat(['']).join(', ') +
    missingIncomePeriodStrings.slice(-2).join(` ${andString} `)
  )
}
