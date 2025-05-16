// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Locale, Month, Day } from 'date-fns'
import { fi, sv, enGB } from 'date-fns/locale'
import React, { useMemo, useState } from 'react'
import type { OnSelectHandler } from 'react-day-picker'
import { DayPicker } from 'react-day-picker'

import LocalDate from 'lib-common/local-date'
import 'react-day-picker/style.css'
import { capitalizeFirstLetter } from 'lib-common/string'

interface Props {
  onSelect: OnSelectHandler<Date | undefined>
  inputValue: string
  locale: 'fi' | 'sv' | 'en'
  minDate?: LocalDate
  maxDate?: LocalDate
  initialMonth?: LocalDate
}

export default React.memo(function DatePickerDay({
  onSelect,
  inputValue,
  locale,
  minDate,
  maxDate,
  initialMonth
}: Props) {
  const date = useMemo(
    () => LocalDate.parseFiOrNull(inputValue) ?? undefined,
    [inputValue]
  )

  const localeData = useLocaleWithCapitalizedNames(locale)
  const [month, setMonth] = useState<Date>(
    () =>
      initialMonth?.toSystemTzDate() ??
      date?.toSystemTzDate() ??
      LocalDate.todayInHelsinkiTz().toSystemTzDate()
  )

  return (
    <DayPicker
      mode="single"
      captionLayout="dropdown"
      startMonth={startMonth}
      endMonth={endMonth}
      autoFocus
      onSelect={onSelect}
      locale={localeData}
      selected={date?.toSystemTzDate()}
      month={month}
      onMonthChange={setMonth}
      disabled={(date: Date) => {
        const localDate = LocalDate.fromSystemTzDate(date)
        return (
          (minDate && minDate.isAfter(localDate)) ||
          (maxDate && maxDate.isBefore(localDate)) ||
          false
        )
      }}
    />
  )
})

const startMonth = LocalDate.of(1900, 1, 1).toSystemTzDate()
const endMonth = LocalDate.todayInHelsinkiTz().addYears(50).toSystemTzDate()

function useLocaleWithCapitalizedNames(locale: 'fi' | 'sv' | 'en'): Locale {
  const localeData = locale === 'sv' ? sv : locale === 'en' ? enGB : fi
  return useMemo(
    () => ({
      ...localeData,
      localize: {
        ...localeData.localize,
        month: (m: Month) =>
          capitalizeFirstLetter(localeData.localize?.month(m) ?? ''),
        day: (d: Day) =>
          capitalizeFirstLetter(
            localeData.localize?.day(d, { width: 'short' }) ?? ''
          )
      }
    }),
    [localeData]
  )
}
