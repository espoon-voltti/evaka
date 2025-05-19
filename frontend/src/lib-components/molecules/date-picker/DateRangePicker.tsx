// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React, { useCallback, useEffect, useRef, useState } from 'react'
import styled from 'styled-components'

import type { LocalDateRangeField } from 'lib-common/form/fields'
import type { BoundForm } from 'lib-common/form/hooks'
import { useBoolean, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import { useStableCallback } from 'lib-common/utils/useStableCallback'
import type { InputInfo } from 'lib-components/atoms/form/InputField'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'

import UnderRowStatusIcon from '../../atoms/StatusIcon'
import { useTranslations } from '../../i18n'

import type { DatePickerProps } from './DatePicker'
import type { DateRangePickerLowLevelProps } from './DateRangePickerLowLevel'
import DateRangePickerLowLevel from './DateRangePickerLowLevel'

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
  const [isValid, setIsValid] = useState<boolean>()

  const afterValidation = useStableCallback(
    onValidationResult ?? (() => undefined)
  )

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
      setIsValid(isValid)
      return isValid
    },
    [
      i18n.datePicker.validationErrors.dateTooEarly,
      i18n.datePicker.validationErrors.dateTooLate,
      minDate,
      maxDate
    ]
  )

  useEffect(() => {
    if (isValid !== undefined) afterValidation(isValid)
  }, [afterValidation, isValid])

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
    ([startStr, endStr]: [string, string]) => {
      setInternalStart(startStr)
      setInternalEnd(endStr)
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

  return (
    <DateRangePickerLowLevel
      value={[internalStart, internalEnd]}
      onChange={handleChange}
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
  return <DateInputSpacer aria-hidden>–</DateInputSpacer>
})

const DateInputSpacer = styled.div`
  padding: 6px;
`

export interface DateRangePickerFProps
  extends Omit<
    DateRangePickerLowLevelProps,
    'value' | 'onChange' | 'startInfo' | 'endInfo' | 'minDate' | 'maxDate'
  > {
  bind: BoundForm<LocalDateRangeField>
  info?: InputInfo | undefined
  ariaId?: string
}

export const DateRangePickerF = React.memo(function DateRangePickerF({
  bind,
  info: infoOverride,
  hideErrorsBeforeTouched,
  ...props
}: DateRangePickerFProps) {
  const { start, end, config } = useFormFields(bind)
  const { update } = bind

  const [touched, useTouched] = useBoolean(false)

  const handleChange = useCallback(
    ([newStart, newEnd]: [string, string]) => {
      update((prev) => ({
        ...prev,
        start: newStart,
        end: newEnd
      }))
    },
    [update]
  )
  const info = infoOverride ?? bind.inputInfo()

  return (
    <div>
      <DateRangePickerLowLevel
        value={[start.state, end.state]}
        onChange={handleChange}
        onBlur={useTouched.on}
        startInfo={start.inputInfo()}
        endInfo={end.inputInfo()}
        minDate={config.state?.minDate}
        maxDate={config.state?.maxDate}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        {...props}
      />
      {info !== undefined && (!hideErrorsBeforeTouched || touched) ? (
        <InputFieldUnderRow className={classNames(info.status)}>
          <span role={info.status === 'warning' ? 'alert' : undefined}>
            {info.text}
          </span>
          <UnderRowStatusIcon status={info.status} />
        </InputFieldUnderRow>
      ) : null}
    </div>
  )
})
