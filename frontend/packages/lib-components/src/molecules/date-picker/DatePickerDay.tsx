// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import DayPicker, { Modifiers } from 'react-day-picker'
import 'react-day-picker/lib/style.css'
import { fi, sv, enGB } from 'date-fns/locale'

import LocalDate from '@evaka/lib-common/src/local-date'

const monthNumbers = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]

const weekdayNumbers = [0, 1, 2, 3, 4, 5, 6]

interface Props {
  handleDayClick: (day: Date, modifiers?: Modifiers) => void
  inputValue: string
  locale: 'fi' | 'sv' | 'en'
  isValidDate?: (date: LocalDate) => boolean
}

function DatePickerDay({
  handleDayClick,
  inputValue,
  locale,
  isValidDate
}: Props) {
  const dateI18n = locale === 'sv' ? sv : locale === 'en' ? enGB : fi

  function convertToDate(date: string) {
    try {
      return LocalDate.parseFi(date).toSystemTzDate()
    } catch (e) {
      return undefined
    }
  }

  const capitalize = (s: string) => s.charAt(0).toUpperCase() + s.slice(1)

  const months = monthNumbers
    .map((m) => dateI18n.localize?.month(m) ?? '') // eslint-disable-line @typescript-eslint/no-unsafe-return
    .map(capitalize)
  const weekdaysLong = weekdayNumbers
    .map((d) => dateI18n.localize?.day(d) ?? '') // eslint-disable-line @typescript-eslint/no-unsafe-return
    .map(capitalize)
  const weekdaysShort = weekdayNumbers
    .map((d) => dateI18n.localize?.day(d, { width: 'short' }) ?? '') // eslint-disable-line @typescript-eslint/no-unsafe-return
    .map(capitalize)

  return (
    <DayPicker
      onDayClick={handleDayClick}
      locale={locale}
      months={months}
      weekdaysLong={weekdaysLong}
      weekdaysShort={weekdaysShort}
      firstDayOfWeek={locale === 'en' ? 0 : 1}
      selectedDays={convertToDate(inputValue)}
      disabledDays={(date: Date) => {
        const localDate = LocalDate.fromSystemTzDate(date)
        return isValidDate ? !isValidDate(localDate) : true
      }}
    />
  )
}

export default DatePickerDay
