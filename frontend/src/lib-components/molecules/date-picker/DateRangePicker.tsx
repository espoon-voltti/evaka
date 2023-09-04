// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { LocalDateRangeField } from 'lib-common/form/fields'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import {
  InputFieldUnderRow,
  InputInfo
} from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'

import UnderRowStatusIcon from '../../atoms/StatusIcon'
import { useTranslations } from '../../i18n'

import DatePicker, { DatePickerProps } from './DatePicker'
import DateRangePickerLowLevel, {
  DateRangePickerLowLevelProps
} from './DateRangePickerLowLevel'
import { nativeDatePickerEnabled } from './helpers'

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

export const DatePickerSpacer = React.memo(function DatePickerSpacer() {
  return <DateInputSpacer>–</DateInputSpacer>
})

const DateInputSpacer = styled.div`
  padding: 6px;
`

export interface DateRangePickerFProps
  extends Omit<
    DateRangePickerLowLevelProps,
    | 'start'
    | 'end'
    | 'onChangeStart'
    | 'onChangeEnd'
    | 'startInfo'
    | 'endInfo'
    | 'minDate'
    | 'maxDate'
  > {
  bind: BoundForm<LocalDateRangeField>
  info?: InputInfo | undefined
}

export const DateRangePickerF = React.memo(function DateRangePickerF({
  bind,
  info: infoOverride,
  ...props
}: DateRangePickerFProps) {
  const { start, end, config } = useFormFields(bind)
  const info = infoOverride ?? bind.inputInfo()
  return (
    <div>
      <DateRangePickerLowLevel
        start={start.state}
        end={end.state}
        onChangeStart={start.set}
        onChangeEnd={end.set}
        startInfo={start.inputInfo()}
        endInfo={end.inputInfo()}
        minDate={config.state?.minDate}
        maxDate={config.state?.maxDate}
        {...props}
      />
      {info !== undefined ? (
        <InputFieldUnderRow className={classNames(info.status)}>
          <span>{info.text}</span> <UnderRowStatusIcon status={info.status} />
        </InputFieldUnderRow>
      ) : null}
    </div>
  )
})
