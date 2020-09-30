// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/src/local-date'
import { translations } from '~assets/i18n'

export function getRange(num: number) {
  const nums = []
  let i = 0
  while (i < num) {
    nums.push(i + 1)
    i++
  }
  return nums
}

export function getWeekDay(date: LocalDate) {
  return translations.fi.datePicker.weekdaysShort[
    date.toSystemTzDate().getDay()
  ]
}

export function getMonthDays(date: LocalDate): LocalDate[] {
  const firstDayOfMonth = date.withDate(1)
  const firstDayOfNextMonth = firstDayOfMonth.addMonths(1)

  const dates = []
  let dateToAdd = firstDayOfMonth
  while (dateToAdd.isBefore(firstDayOfNextMonth)) {
    dates.push(dateToAdd)
    dateToAdd = dateToAdd.addDays(1)
  }

  return dates
}
