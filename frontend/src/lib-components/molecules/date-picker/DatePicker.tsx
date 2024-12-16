// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useRef, useState } from 'react'

import { LocalDateField } from 'lib-common/form/fields'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'

import DatePickerLowLevel, {
  DatePickerLowLevelProps
} from './DatePickerLowLevel'

export interface DatePickerProps
  extends Omit<DatePickerLowLevelProps, 'value' | 'onChange'> {
  date: LocalDate | null
  onChange: (date: LocalDate | null) => void
  isInvalidDate?: (date: LocalDate) => string | null
}

const DatePicker = React.memo(function DatePicker({
  date,
  onChange,
  info,
  minDate,
  maxDate,
  isInvalidDate,
  ...props
}: DatePickerProps) {
  const i18n = useTranslations()
  const [textValue, setTextValue] = useState(date?.format() ?? '')
  const [internalError, setInternalError] = useState<InputInfo>()

  const prevDate = useRef<LocalDate | null>(date)
  if (prevDate.current?.formatIso() !== date?.formatIso()) {
    prevDate.current = date
    if (date !== null) {
      setTextValue(date.format())
    }
  }

  const handleChange = useCallback(
    (value: string) => {
      setTextValue(value)

      const newDate = LocalDate.parseFiOrNull(value)
      if (newDate === null) {
        if (date !== null) onChange(null)
      } else {
        if (minDate && newDate.isBefore(minDate)) {
          setInternalError({
            text: i18n.datePicker.validationErrors.dateTooEarly,
            status: 'warning'
          })
        } else if (maxDate && newDate.isAfter(maxDate)) {
          setInternalError({
            text: i18n.datePicker.validationErrors.dateTooLate,
            status: 'warning'
          })
        } else {
          const validationError = isInvalidDate?.(newDate)
          if (validationError) {
            setInternalError({ text: validationError, status: 'warning' })
          } else {
            setInternalError(undefined)
            if (date === null || !newDate.isEqual(date)) onChange(newDate)
          }
        }
      }
    },
    [date, i18n, maxDate, minDate, isInvalidDate, onChange]
  )

  return (
    <DatePickerLowLevel
      value={textValue}
      onChange={handleChange}
      info={internalError ?? info}
      minDate={minDate}
      maxDate={maxDate}
      {...props}
    />
  )
})

export default DatePicker

export interface DatePickerFProps
  extends Omit<
    DatePickerLowLevelProps,
    'value' | 'onChange' | 'minDate' | 'maxDate'
  > {
  bind: BoundForm<LocalDateField>
}

export const DatePickerF = React.memo(function DatePickerF({
  bind,
  ...props
}: DatePickerFProps) {
  const { value, config } = useFormFields(bind)
  return (
    <DatePickerLowLevel
      {...props}
      value={value.state}
      onChange={value.set}
      minDate={config.state?.minDate}
      maxDate={config.state?.maxDate}
      info={'info' in props ? props.info : bind.inputInfo()}
    />
  )
})
