// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
import { FixedSpaceRow } from '../../layout/flex-helpers'

import DatePickerLowLevel, {
  DatePickerLowLevelProps
} from './DatePickerLowLevel'
import { DatePickerSpacer } from './DateRangePicker'
import { nativeDatePickerEnabled } from './helpers'

export interface DateRangePickerLowLevelProps
  extends Omit<DatePickerLowLevelProps, 'value' | 'onChange' | 'info'> {
  start: string
  end: string
  onChangeStart: (start: string) => void
  onChangeEnd: (end: string) => void
  startInfo?: InputInfo
  endInfo?: InputInfo
}

export default React.memo(function DateRangePickerLowLevel({
  start,
  end,
  onChangeStart,
  onChangeEnd,
  startInfo,
  endInfo,
  'data-qa': dataQa,
  ...datePickerProps
}: DateRangePickerLowLevelProps) {
  const startDate = useMemo(() => LocalDate.parseFiOrNull(start), [start])
  const endDate = useMemo(() => LocalDate.parseFiOrNull(end), [end])

  const minDateForEnd = useMemo(
    () =>
      (nativeDatePickerEnabled &&
      (!datePickerProps.minDate || startDate?.isAfter(datePickerProps.minDate))
        ? startDate
        : datePickerProps.minDate) ?? undefined,
    [datePickerProps.minDate, startDate]
  )

  const maxDateForStart = useMemo(
    () =>
      (nativeDatePickerEnabled &&
      (!datePickerProps.maxDate || endDate?.isBefore(datePickerProps.maxDate))
        ? endDate
        : datePickerProps.maxDate) ?? undefined,
    [datePickerProps.maxDate, endDate]
  )

  return (
    <FixedSpaceRow data-qa={dataQa}>
      <DatePickerLowLevel
        value={start}
        onChange={onChangeStart}
        info={startInfo}
        initialMonth={startDate ?? datePickerProps.minDate}
        data-qa="start-date"
        {...datePickerProps}
        maxDate={maxDateForStart}
      />
      <DatePickerSpacer />
      <DatePickerLowLevel
        value={end}
        onChange={onChangeEnd}
        info={endInfo}
        initialMonth={endDate ?? startDate ?? minDateForEnd}
        data-qa="end-date"
        {...datePickerProps}
        minDate={minDateForEnd}
      />
    </FixedSpaceRow>
  )
})
