// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
import { FixedSpaceRow } from '../../layout/flex-helpers'

import DatePickerLowLevel, {
  DatePickerLowLevelProps
} from './DatePickerLowLevel'
import { DatePickerSpacer } from './DateRangePicker'

export interface DateRangePickerLowLevelProps
  extends Omit<DatePickerLowLevelProps, 'value' | 'onChange' | 'info'> {
  value: [string, string]
  onChange: (value: [string, string]) => void
  startInfo?: InputInfo
  endInfo?: InputInfo
  ariaId?: string
}

export default React.memo(function DateRangePickerLowLevel({
  value: [start, end],
  onChange,
  startInfo,
  endInfo,
  'data-qa': dataQa,
  minDate,
  maxDate,
  ariaId,
  ...datePickerProps
}: DateRangePickerLowLevelProps) {
  const startDate = useMemo(() => LocalDate.parseFiOrNull(start), [start])
  const endDate = useMemo(() => LocalDate.parseFiOrNull(end), [end])

  const handleChangeStart = useCallback(
    (newStart: string) => {
      const newStartDate = LocalDate.parseFiOrNull(newStart)
      if (
        newStartDate !== null &&
        endDate !== null &&
        newStartDate.isAfter(endDate)
      ) {
        onChange([newStart, newStart])
      } else {
        onChange([newStart, end])
      }
    },
    [end, endDate, onChange]
  )

  const handleChangeEnd = useCallback(
    (newEnd: string) => {
      const newEndDate = LocalDate.parseFiOrNull(newEnd)
      if (
        startDate !== null &&
        newEndDate !== null &&
        newEndDate.isBefore(startDate)
      ) {
        onChange([newEnd, newEnd])
      } else {
        onChange([start, newEnd])
      }
    },
    [onChange, start, startDate]
  )

  return (
    <FixedSpaceRow data-qa={dataQa} aria-labelledby={ariaId} role="group">
      <DatePickerLowLevel
        value={start}
        onChange={handleChangeStart}
        info={startInfo}
        initialMonth={startDate ?? minDate}
        data-qa="start-date"
        minDate={minDate}
        maxDate={maxDate}
        {...datePickerProps}
      />
      <DatePickerSpacer />
      <DatePickerLowLevel
        value={end}
        onChange={handleChangeEnd}
        info={endInfo}
        initialMonth={endDate ?? startDate ?? minDate}
        data-qa="end-date"
        minDate={minDate}
        maxDate={maxDate}
        {...datePickerProps}
      />
    </FixedSpaceRow>
  )
})
