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
  minDate,
  maxDate,
  ...datePickerProps
}: DateRangePickerLowLevelProps) {
  const startDate = useMemo(() => LocalDate.parseFiOrNull(start), [start])
  const endDate = useMemo(() => LocalDate.parseFiOrNull(end), [end])

  const minDateForEnd = useMemo(
    () =>
      (!minDate || startDate?.isAfter(minDate) ? startDate : minDate) ??
      undefined,
    [minDate, startDate]
  )

  const maxDateForStart = useMemo(
    () =>
      (!maxDate || endDate?.isBefore(maxDate) ? endDate : maxDate) ?? undefined,
    [maxDate, endDate]
  )

  return (
    <FixedSpaceRow data-qa={dataQa}>
      <DatePickerLowLevel
        value={start}
        onChange={onChangeStart}
        info={startInfo}
        initialMonth={startDate ?? minDate}
        data-qa="start-date"
        minDate={minDate}
        maxDate={maxDateForStart}
        {...datePickerProps}
      />
      <DatePickerSpacer />
      <DatePickerLowLevel
        value={end}
        onChange={onChangeEnd}
        info={endInfo}
        initialMonth={endDate ?? startDate ?? minDateForEnd}
        data-qa="end-date"
        minDate={minDateForEnd}
        maxDate={maxDate}
        {...datePickerProps}
      />
    </FixedSpaceRow>
  )
})
