// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import React, { useEffect, useMemo, useState } from 'react'

import { BoundFormShape, useFormField } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import { InputInfo } from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import { useTranslations } from '../../i18n'

import DatePicker, {
  DatePickerProps,
  DatePickerSpacer,
  nativeDatePickerEnabled
} from './DatePicker'

interface DateRangePickerProps
  extends Omit<DatePickerProps, 'date' | 'onChange'> {
  start: LocalDate | null
  end: LocalDate | null
  onChange: (start: LocalDate | null, end: LocalDate | null) => void
  startInfo?: InputInfo
  endInfo?: InputInfo
  'data-qa'?: string
}

const DateRangePicker = React.memo(function DateRangePicker({
  start,
  end,
  onChange,
  'data-qa': dataQa,
  ...datePickerProps
}: DateRangePickerProps) {
  const i18n = useTranslations()
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
        text: i18n.datePicker.validationErrors.dateTooLate,
        status: 'warning'
      })
      setInternalEndError({
        text: i18n.datePicker.validationErrors.dateTooEarly,
        status: 'warning'
      })
      return
    }

    if (
      start?.formatIso() !== internalStart?.formatIso() ||
      end?.formatIso() !== internalEnd?.formatIso()
    ) {
      onChange(internalStart, internalEnd)
    }
  }, [i18n, internalStart, internalEnd, onChange, start, end])

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
    <FixedSpaceRow data-qa={dataQa}>
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
      />
      <DatePickerSpacer />
      <DatePicker
        date={internalEnd}
        onChange={(date) => setInternalEnd(date)}
        data-qa="end-date"
        initialMonth={internalEnd ?? internalStart ?? minDateForEnd}
        {...datePickerProps}
        minDate={minDateForEnd}
        info={
          internalEndError ?? datePickerProps.endInfo ?? datePickerProps.info
        }
      />
    </FixedSpaceRow>
  )
})

export default DateRangePicker

export interface DateRangePickerFProps
  extends Omit<DateRangePickerProps, 'start' | 'end' | 'onChange'> {
  bind: BoundFormShape<
    {
      startDate: LocalDate | null
      endDate: LocalDate | null
    },
    {
      startDate: Form<unknown, string, LocalDate | null, unknown>
      endDate: Form<unknown, string, LocalDate | null, unknown>
    }
  >
}

export const DateRangePickerF = React.memo(function DateRangePickerF({
  bind,
  ...props
}: DateRangePickerFProps) {
  const startDate = useFormField(bind, 'startDate')
  const endDate = useFormField(bind, 'endDate')
  return (
    <DateRangePicker
      {...props}
      start={startDate.state}
      end={endDate.state}
      onChange={(start, end) => {
        startDate.set(start)
        endDate.set(end)
      }}
      startInfo={'startInfo' in props ? props.startInfo : startDate.inputInfo()}
      endInfo={'endInfo' in props ? props.endInfo : endDate.inputInfo()}
    />
  )
})
