// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { useEffect, useState } from 'react'

import DateRange from 'lib-common/date-range'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { CalendarAriaProps } from './Calendar'
import DatePicker, { DatePickerProps } from './DatePicker'

interface DateRangePickerProps {
  dateRange: DateRange | null
  onChange: (range: DateRange | null) => void
  errorTexts: DatePickerProps['errorTexts'] & {
    dateTooEarly: string
    dateTooLate: string
  }
  startInfo?: InputInfo
  endInfo?: InputInfo
  'data-qa'?: string
  labels: CalendarAriaProps
  'aria-labelledby': string
}

/**
 * If possible, use DateRangePicker, as it is more reliable
 * for closed date ranges and is more accessible overall.
 */
export default React.memo(function OpenDateRangePicker({
  dateRange,
  onChange,
  errorTexts,
  'data-qa': dataQa,
  ...datePickerProps
}: DateRangePickerProps & Omit<DatePickerProps, 'date' | 'onChange'>) {
  const [internalStart, setInternalStart] = useState(dateRange?.start ?? null)
  const [internalEnd, setInternalEnd] = useState(dateRange?.end ?? null)
  const [internalStartError, setInternalStartError] = useState<InputInfo>()
  const [internalEndError, setInternalEndError] = useState<InputInfo>()

  useEffect(() => {
    setInternalStart(dateRange?.start ?? null)
  }, [dateRange])

  useEffect(() => {
    setInternalEnd(dateRange?.end ?? null)
  }, [dateRange])

  useEffect(() => {
    setInternalStartError(undefined)
    setInternalEndError(undefined)

    if (internalStart && internalEnd && internalStart.isAfter(internalEnd)) {
      setInternalStartError({
        text: errorTexts.dateTooLate,
        status: 'warning'
      })
      setInternalEndError({
        text: errorTexts.dateTooEarly,
        status: 'warning'
      })
      return
    }

    if (
      dateRange?.start.formatIso() !== internalStart?.formatIso() ||
      dateRange?.end?.formatIso() !== internalEnd?.formatIso()
    ) {
      onChange(internalStart && new DateRange(internalStart, internalEnd))
    }
  }, [internalStart, internalEnd, onChange, errorTexts, dateRange])

  return (
    <FixedSpaceRow data-qa={dataQa} alignItems="center" spacing="xs">
      <DatePicker
        date={internalStart}
        onChange={(date) => setInternalStart(date)}
        data-qa="start-date"
        initialMonth={internalStart ?? datePickerProps.minDate}
        {...datePickerProps}
        maxDate={datePickerProps.maxDate}
        info={
          internalStartError ??
          datePickerProps.startInfo ??
          datePickerProps.info
        }
        errorTexts={errorTexts}
      />
      <span>–</span>
      <DatePicker
        date={internalEnd}
        onChange={(date) => setInternalEnd(date)}
        data-qa="end-date"
        initialMonth={internalEnd ?? internalStart ?? datePickerProps.minDate}
        {...datePickerProps}
        minDate={datePickerProps.minDate}
        info={
          internalEndError ?? datePickerProps.endInfo ?? datePickerProps.info
        }
        errorTexts={errorTexts}
      />
    </FixedSpaceRow>
  )
})
