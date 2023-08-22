// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import classNames from 'classnames'
import React, { useEffect, useMemo, useState } from 'react'

import { BoundFormShape, useFormField } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import {
  InputFieldUnderRow,
  InputInfo
} from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import UnderRowStatusIcon from '../../atoms/StatusIcon'
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
  externalRangeValidation?: boolean
  onValidationResult?: (hasErrors: boolean) => void
}

const DateRangePicker = React.memo(function DateRangePicker({
  start,
  end,
  onChange,
  'data-qa': dataQa,
  externalRangeValidation,
  onValidationResult,
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

    if (
      !externalRangeValidation &&
      internalStart &&
      internalEnd &&
      internalStart.isAfter(internalEnd)
    ) {
      setInternalStartError({
        text: i18n.datePicker.validationErrors.dateTooLate,
        status: 'warning'
      })
      setInternalEndError({
        text: i18n.datePicker.validationErrors.dateTooEarly,
        status: 'warning'
      })
      if (onValidationResult) onValidationResult(true)
      return
    }

    if (
      start?.formatIso() !== internalStart?.formatIso() ||
      end?.formatIso() !== internalEnd?.formatIso()
    ) {
      onChange(internalStart, internalEnd)
    }
  }, [
    i18n,
    internalStart,
    internalEnd,
    onChange,
    externalRangeValidation,
    start,
    end,
    onValidationResult
  ])

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
  externalRangeValidation?: boolean
  info?: InputInfo
}

export const DateRangePickerF = React.memo(function DateRangePickerF({
  bind,
  externalRangeValidation,
  info: infoOverride,
  ...props
}: DateRangePickerFProps) {
  const info = infoOverride ?? bind.inputInfo()
  const startDate = useFormField(bind, 'startDate')
  const endDate = useFormField(bind, 'endDate')
  return (
    <div>
      <DateRangePicker
        {...props}
        start={startDate.state}
        end={endDate.state}
        onChange={(start, end) => {
          startDate.set(start)
          endDate.set(end)
        }}
        externalRangeValidation={externalRangeValidation}
        startInfo={
          'startInfo' in props ? props.startInfo : startDate.inputInfo()
        }
        endInfo={'endInfo' in props ? props.endInfo : endDate.inputInfo()}
      />
      {info !== undefined ? (
        <InputFieldUnderRow className={classNames(info.status)}>
          <span>{info.text}</span> <UnderRowStatusIcon status={info.status} />
        </InputFieldUnderRow>
      ) : null}
    </div>
  )
})
