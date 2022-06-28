// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { useEffect, useMemo, useState } from 'react'

import LocalDate from 'lib-common/local-date'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import DatePicker, {
  DatePickerProps,
  DatePickerSpacer,
  nativeDatePickerEnabled
} from './DatePicker'

interface DateRangePickerProps {
  start: LocalDate | null
  end: LocalDate | null
  onChange: (start: LocalDate | null, end: LocalDate | null) => void
  errorTexts: DatePickerProps['errorTexts'] & {
    dateTooEarly: string
    dateTooLate: string
  }
  startInfo?: InputInfo
  endInfo?: InputInfo
}

export default React.memo(function DateRangePicker({
  start,
  end,
  onChange,
  errorTexts,
  ...datePickerProps
}: DateRangePickerProps & Omit<DatePickerProps, 'date' | 'onChange'>) {
  const [internalStart, setInternalStart] = useState(start)
  const [internalEnd, setInternalEnd] = useState(end)
  const [internalStartError, setInternalStartError] = useState<InputInfo>()
  const [internalEndError, setInternalEndError] = useState<InputInfo>()

  useEffect(() => {
    setInternalStart(start)
  }, [start])

  useEffect(() => {
    setInternalEnd(end)
  }, [end])

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

    if (start !== internalStart || end !== internalEnd) {
      onChange(internalStart, internalEnd)
    }
  }, [internalStart, internalEnd, onChange, errorTexts, start, end])

  const minDateForEnd = useMemo(
    () =>
      (nativeDatePickerEnabled &&
      (!datePickerProps.minDate ||
        internalStart?.isAfter(datePickerProps.minDate))
        ? internalStart
        : datePickerProps.minDate) ?? undefined,
    [datePickerProps.minDate, internalStart]
  )

  const maxDateForStart = useMemo(
    () =>
      (nativeDatePickerEnabled &&
      (!datePickerProps.maxDate ||
        internalEnd?.isBefore(datePickerProps.maxDate))
        ? internalEnd
        : datePickerProps.maxDate) ?? undefined,
    [datePickerProps.maxDate, internalEnd]
  )

  return (
    <FixedSpaceRow>
      <DatePicker
        date={internalStart}
        onChange={(date) => setInternalStart(date)}
        data-qa="start-date"
        initialMonth={internalStart ?? datePickerProps.minDate}
        {...datePickerProps}
        maxDate={maxDateForStart}
        info={
          internalStartError ??
          datePickerProps.startInfo ??
          datePickerProps.info
        }
        errorTexts={errorTexts}
      />
      <DatePickerSpacer />
      <DatePicker
        date={internalEnd}
        onChange={(date) => setInternalEnd(date)}
        data-qa="end-date"
        initialMonth={internalEnd ?? minDateForEnd}
        {...datePickerProps}
        minDate={minDateForEnd}
        info={
          internalEndError ?? datePickerProps.endInfo ?? datePickerProps.info
        }
        errorTexts={errorTexts}
      />
    </FixedSpaceRow>
  )
})
