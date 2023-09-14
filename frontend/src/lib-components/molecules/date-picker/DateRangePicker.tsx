// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useCallback, useRef, useState } from 'react'
import styled from 'styled-components'

import { LocalDateRangeField } from 'lib-common/form/fields'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import {
  InputFieldUnderRow,
  InputInfo
} from 'lib-components/atoms/form/InputField'

import UnderRowStatusIcon from '../../atoms/StatusIcon'
import { useTranslations } from '../../i18n'

import { DatePickerProps } from './DatePicker'
import DateRangePickerLowLevel, {
  DateRangePickerLowLevelProps
} from './DateRangePickerLowLevel'

interface DateRangePickerProps
  extends Omit<DatePickerProps, 'date' | 'onChange'> {
  start: LocalDate | null
  end: LocalDate | null
  onChange: (start: LocalDate | null, end: LocalDate | null) => void
  startInfo?: InputInfo
  endInfo?: InputInfo
  'data-qa'?: string
  onValidationResult?: (hasErrors: boolean) => void
}

const DateRangePicker = React.memo(function DateRangePicker({
  start,
  end,
  onChange,
  onValidationResult,
  minDate,
  maxDate,
  ...datePickerProps
}: DateRangePickerProps) {
  const i18n = useTranslations()
  const [internalStart, setInternalStart] = useState(start?.format() ?? '')
  const [internalEnd, setInternalEnd] = useState(end?.format() ?? '')
  const [internalStartError, setInternalStartError] = useState<InputInfo>()
  const [internalEndError, setInternalEndError] = useState<InputInfo>()

  const validate = useCallback(
    (start: LocalDate | null, end: LocalDate | null): boolean => {
      let isValid = true
      if (start && minDate && start.isBefore(minDate)) {
        setInternalStartError({
          text: i18n.datePicker.validationErrors.dateTooEarly,
          status: 'warning'
        })
        isValid = false
      }
      if (end && maxDate && end.isAfter(maxDate)) {
        setInternalEndError({
          text: i18n.datePicker.validationErrors.dateTooEarly,
          status: 'warning'
        })
        isValid = false
      }
      if (start && end && isValid && start.isAfter(end)) {
        setInternalStartError({
          text: i18n.datePicker.validationErrors.dateTooLate,
          status: 'warning'
        })
        setInternalEndError({
          text: i18n.datePicker.validationErrors.dateTooEarly,
          status: 'warning'
        })
        isValid = false
      }
      if (isValid) {
        setInternalStartError(undefined)
        setInternalEndError(undefined)
      }
      if (onValidationResult) onValidationResult(isValid)
      return isValid
    },
    [
      i18n.datePicker.validationErrors.dateTooEarly,
      i18n.datePicker.validationErrors.dateTooLate,
      minDate,
      maxDate,
      onValidationResult
    ]
  )

  const prevStart = useRef(start)
  const prevEnd = useRef(end)
  if (
    prevStart.current?.formatIso() !== start?.formatIso() ||
    prevEnd.current?.formatIso() !== end?.formatIso()
  ) {
    prevStart.current = start
    if (start !== null) {
      setInternalStart(start.format())
    }
    prevEnd.current = end
    if (end !== null) {
      setInternalEnd(end.format())
    }
    validate(start, end)
  }

  const handleChange = useCallback(
    (startStr: string, endStr: string) => {
      const newStart = LocalDate.parseFiOrNull(startStr)
      const newEnd = LocalDate.parseFiOrNull(endStr)
      const isValid = validate(newStart, newEnd)
      if (
        isValid &&
        (newStart?.formatIso() !== start?.formatIso() ||
          newEnd?.formatIso() !== end?.formatIso())
      ) {
        onChange(newStart, newEnd)
      }
    },
    [end, onChange, start, validate]
  )

  const handleChangeStart = useCallback(
    (newStart: string) => {
      setInternalStart(newStart)
      handleChange(newStart, internalEnd)
    },
    [handleChange, internalEnd]
  )

  const handleChangeEnd = useCallback(
    (newEnd: string) => {
      setInternalEnd(newEnd)
      handleChange(internalStart, newEnd)
    },
    [handleChange, internalStart]
  )

  return (
    <DateRangePickerLowLevel
      start={internalStart}
      end={internalEnd}
      onChangeStart={handleChangeStart}
      onChangeEnd={handleChangeEnd}
      startInfo={internalStartError ?? datePickerProps.startInfo}
      endInfo={internalEndError ?? datePickerProps.endInfo}
      minDate={minDate}
      maxDate={maxDate}
      {...datePickerProps}
    />
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
