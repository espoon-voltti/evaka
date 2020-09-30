// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useEffect } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import DateInput from './DateInput'
import { useTranslation } from '../../state/i18n'
import './DateRangeInput.scss'

type NullableEnd = {
  end?: LocalDate
  onChange: (start: LocalDate, end?: LocalDate) => void
  nullableEndDate: true
}

type NonNullableEnd = {
  end: LocalDate
  onChange: (start: LocalDate, end: LocalDate) => void
}

type CommonProps = {
  start: LocalDate
  onValidationResult: (hasErrors: boolean) => void
}

type Props = CommonProps & (NullableEnd | NonNullableEnd)

export default function DateRangeInput(props: Props) {
  const onChange = (start: LocalDate, end: LocalDate | undefined) => {
    if ('nullableEndDate' in props && props.nullableEndDate) {
      props.onChange(start, end)
      return true
    } else {
      if (end === undefined) {
        return false
      } else {
        props.onChange(start, end)
        return true
      }
    }
  }

  return (
    <BaseDateRangeInput
      start={props.start}
      end={props.end}
      onChange={onChange}
      onValidationResult={props.onValidationResult}
    />
  )
}

interface BaseDateRangeInputProps {
  start: LocalDate
  end?: LocalDate
  // true means possibly nullable end date was accepted, false means it was not
  onChange: (start: LocalDate, end?: LocalDate) => boolean
  onValidationResult: (hasErrors: boolean) => void
}

function BaseDateRangeInput({
  start,
  end,
  onChange,
  onValidationResult
}: BaseDateRangeInputProps) {
  const { i18n } = useTranslation()

  const [startValue, setStartValue] = useState<
    LocalDate | 'invalid' | undefined
  >(start)
  const [endValue, setEndValue] = useState<LocalDate | 'invalid' | undefined>(
    end
  )

  const [validationErrors, setValidationErrors] = useState<{
    start?: string
    end?: string
  }>({})

  const isInverted = (start: LocalDate, end?: LocalDate) =>
    end && start.isAfter(end)

  const handleOnChange = (start: LocalDate, end?: LocalDate) => {
    if (isInverted(start, end)) {
      setValidationErrors((state) => ({
        ...state,
        end: i18n.validationError.invertedDateRange
      }))
    }

    if (!onChange(start, end)) {
      setValidationErrors((state) => ({
        ...state,
        end: i18n.validationError.mandatoryField
      }))
    }
  }

  useEffect(() => {
    if (startValue === undefined) {
      setValidationErrors((state) => ({
        ...state,
        start: i18n.validationError.mandatoryField
      }))
    } else if (startValue === 'invalid') {
      setValidationErrors((state) => ({
        ...state,
        start: i18n.validationError.dateRange
      }))
    } else {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      setValidationErrors(({ start, ...state }) => ({
        ...state
      }))
      handleOnChange(startValue, end)
    }
  }, [startValue])

  useEffect(() => {
    if (endValue === 'invalid') {
      setValidationErrors((state) => ({
        ...state,
        end: i18n.validationError.dateRange
      }))
    } else {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      setValidationErrors(({ end, ...state }) => ({
        ...state
      }))
      handleOnChange(start, endValue)
    }
  }, [endValue])

  useEffect(() => {
    onValidationResult(!!validationErrors.start || !!validationErrors.end)
  }, [validationErrors])

  return (
    <div className="date-range-input-container">
      <DateInput
        initial={start}
        onChange={setStartValue}
        errorMessage={validationErrors.start}
        dataQa="date-range-input-start-date"
      />
      <DateInput
        initial={end}
        onChange={setEndValue}
        errorMessage={validationErrors.end}
        dataQa="date-range-input-end-date"
      />
    </div>
  )
}
