// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fi, sv, enGB } from 'date-fns/locale'
import React, { useEffect, useMemo, useState } from 'react'
import type { DayModifiers } from 'react-day-picker'
import { DayPicker } from 'react-day-picker'

import LocalDate from 'lib-common/local-date'
import 'react-day-picker/dist/style.css'
import { capitalizeFirstLetter } from 'lib-common/string'

interface Props {
  handleDayClick: (day: Date, modifiers?: DayModifiers) => void
  inputValue: LocalDate | null
  locale: 'fi' | 'sv' | 'en'
  isInvalidDate?: (date: LocalDate) => boolean
  initialMonth?: LocalDate
  minDate?: LocalDate
  maxDate?: LocalDate
}

export default React.memo(function DatePickerDay({
  handleDayClick,
  inputValue,
  locale,
  isInvalidDate,
  initialMonth,
  minDate,
  maxDate
}: Props) {
  const localeData = useLocaleWithCapitalizedNames(locale)
  const [month, setMonth] = useState<Date>(
    initialMonth?.toSystemTzDate() ??
      LocalDate.todayInHelsinkiTz().toSystemTzDate()
  )

  useEffect(() => {
    if (inputValue) {
      setMonth(inputValue.toSystemTzDate())
    }
  }, [inputValue])

  return (
    <DayPicker
      onDayClick={handleDayClick}
      locale={localeData}
      selected={inputValue?.toSystemTzDate() ?? undefined}
      month={month}
      onMonthChange={(m) => setMonth(m)}
      disabled={(date: Date) => {
        const localDate = LocalDate.fromSystemTzDate(date)

        if (isInvalidDate?.(localDate)) {
          return true
        }

        if (minDate && minDate.isAfter(localDate)) {
          return true
        }

        if (maxDate && maxDate.isBefore(localDate)) {
          return true
        }

        return false
      }}
      defaultMonth={
        inputValue?.toSystemTzDate() ?? initialMonth?.toSystemTzDate()
      }
    />
  )
})

function useLocaleWithCapitalizedNames(locale: 'fi' | 'sv' | 'en'): Locale {
  const localeData = locale === 'sv' ? sv : locale === 'en' ? enGB : fi
  return useMemo(
    () => ({
      ...localeData,
      localize: localeData.localize
        ? {
            ...localeData.localize,
            month: (m: unknown) =>
              capitalizeFirstLetter(localeData.localize?.month(m) ?? ''), // eslint-disable-line @typescript-eslint/no-unsafe-argument
            day: (d: unknown) =>
              capitalizeFirstLetter(
                localeData.localize?.day(d, { width: 'short' }) ?? '' // eslint-disable-line @typescript-eslint/no-unsafe-argument
              )
          }
        : undefined
    }),
    [localeData]
  )
}
