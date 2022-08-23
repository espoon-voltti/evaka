// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useDatePicker } from '@react-aria/datepicker'
import { useDatePickerState } from '@react-stately/datepicker'
import classNames from 'classnames'
import React, { useEffect, useRef, useState } from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import {
  scrollIntoViewSoftKeyboard,
  scrollRefIntoView
} from 'lib-common/utils/scrolling'
import Popover from 'lib-components/atoms/Popover'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faCalendarAlt } from 'lib-icons'

import { InputInfo } from '../../atoms/form/InputField'
import { defaultMargins } from '../../white-space'

import Calendar, { CalendarAriaProps } from './Calendar'
import DateField from './DateField'

interface BaseDatePickerProps {
  date: LocalDate | null
  onChange: (date: LocalDate | null) => void
  onFocus?: (e: React.FocusEvent<Element>) => void
  onBlur?: () => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  initialMonth?: LocalDate
  errorTexts: {
    validDate: string
  }

  /**
   * It is preferable to use `minDate` and `maxDate` instead, if possible.
   * The native date pickers only support minimum and maximum dates, so
   * it is more user-friendly to use min/max whenever possible.
   *
   * Should return a localized error string if the date is invalid, null
   * if the date is valid.
   */
  isInvalidDate?: (date: LocalDate) => string | null

  minDate?: never
  maxDate?: never

  labels: CalendarAriaProps

  fullWidth?: boolean
}

type WithoutMinMaxBaseProps = Omit<BaseDatePickerProps, 'minDate' | 'maxDate'>

interface MinDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: never
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooEarly: string
  }
}

interface MaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: never
  maxDate?: LocalDate
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooLate: string
  }
}

interface MinMaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: LocalDate
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooEarly: string
    dateTooLate: string
  }
}

type DatePickerAriaLabelProps =
  | {
      'aria-labelledby': string
    }
  | {
      /** aria-labelledby is preferred */
      'aria-label': string
    }

export type DatePickerProps = (
  | BaseDatePickerProps
  | MinDatePickerProps
  | MaxDatePickerProps
  | MinMaxDatePickerProps
) &
  DatePickerAriaLabelProps

const DatePickerContainer = styled.div<{ fullWidth?: boolean }>`
  position: relative;
  width: ${(p) => (p.fullWidth ? '100%' : 'fit-content')};
`

const DateInput = styled.div`
  margin: 0;
  border: none;
  border-top: 2px solid transparent;
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  border-radius: 0;
  outline: none;
  text-align: left;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  font-size: 1rem;
  color: ${(p) => p.theme.colors.grayscale.g100};
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  justify-content: space-between;

  &.focus,
  &.success,
  &.warning {
    border-bottom-width: 2px;
    margin-bottom: -1px;
  }

  &.focus {
    border: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    border-radius: 2px;
    padding-left: 8px;
    padding-right: 8px;
  }

  &.success {
    border-bottom-color: ${(p) => p.theme.colors.status.success};

    &.focus {
      border-color: ${(p) => p.theme.colors.status.success};
    }
  }

  &.warning {
    border-bottom-color: ${(p) => p.theme.colors.status.warning};

    &.focus {
      border-color: ${(p) => p.theme.colors.status.warning};
    }
  }
`

const InputFieldUnderRow = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  font-size: 1rem;
  line-height: 1rem;
  margin-top: ${defaultMargins.xxs};

  color: ${(p) => p.theme.colors.grayscale.g70};

  &.success {
    color: ${(p) => p.theme.colors.accents.a1greenDark};
  }

  &.warning {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
  }
`

export default React.memo(function DatePicker(props: DatePickerProps) {
  const [error, setError] = useState<string>()

  const state = useDatePickerState({
    onChange: (v) =>
      props.onChange(LocalDate.tryCreate(v.year, v.month, v.day) ?? null),
    // missing nullable type
    value: props.date?.asCalendarDate(),
    placeholderValue: props.initialMonth?.asCalendarDate()
  })
  const ref = React.useRef<HTMLDivElement>(null)
  const { groupProps, fieldProps, buttonProps, dialogProps, calendarProps } =
    useDatePicker(
      {
        minValue: props.minDate?.asCalendarDate(),
        maxValue: props.maxDate?.asCalendarDate(),
        isDateUnavailable:
          props.isInvalidDate &&
          ((date) => {
            const localDate = LocalDate.tryCreate(
              date.year,
              date.month,
              date.day
            )
            return localDate ? !!props.isInvalidDate?.(localDate) : false
          }),
        isDisabled: props.disabled,
        'aria-labelledby':
          'aria-labelledby' in props ? props['aria-labelledby'] : undefined,
        'aria-label': 'aria-label' in props ? props['aria-label'] : undefined
      },
      state,
      ref
    )

  useEffect(() => {
    setError(undefined)
    if (!state.value) {
      return
    }

    if (
      props.minDate &&
      state.value.compare(props.minDate.asCalendarDate()) < 0
    ) {
      setError(props.errorTexts.dateTooEarly)
      return
    }

    if (
      props.maxDate &&
      state.value.compare(props.maxDate.asCalendarDate()) > 0
    ) {
      setError(props.errorTexts.dateTooLate)
      return
    }
  }, [state.value, props.minDate, props.maxDate, props.errorTexts])

  const popoverRef = useRef<HTMLDivElement>(null)

  return (
    <DatePickerContainer fullWidth={props.fullWidth}>
      <DateInput
        {...groupProps}
        ref={ref}
        className={classNames({ warning: error })}
      >
        <DateField
          {...fieldProps}
          data-qa={props['data-qa']}
          locale={props.locale}
          onFocus={(ev) => {
            props.onFocus?.(ev)
            fieldProps.onFocus?.(ev)

            if (ref.current) {
              scrollIntoViewSoftKeyboard(ref.current, 'center')
            }
          }}
        />
        <IconButton
          icon={faCalendarAlt}
          aria-label={props.labels.calendarLabel}
          {...buttonProps}
          onPress={(ev) => {
            buttonProps.onPress?.(ev)
            setTimeout(() => {
              if (popoverRef.current) {
                const { top, bottom } =
                  popoverRef.current.getBoundingClientRect()

                if (top < 0 || bottom > window.innerHeight) {
                  scrollRefIntoView(popoverRef, 1, {
                    block: 'end'
                  })
                }
              }
            }, 1)
          }}
        />
      </DateInput>
      {(!!error || !!props.info) && (
        <InputFieldUnderRow
          className="warning"
          data-qa={props['data-qa'] ? `${props['data-qa']}-info` : undefined}
        >
          {error || props.info?.text}
        </InputFieldUnderRow>
      )}
      {state.isOpen && (
        <Popover
          {...dialogProps}
          isOpen={state.isOpen}
          popoverRef={popoverRef}
          onClose={() => state.setOpen(false)}
        >
          <Calendar {...calendarProps} {...props.labels} />
        </Popover>
      )}
    </DatePickerContainer>
  )
})

export const DatePickerSpacer = React.memo(function DatePickerSpacer() {
  return <DateInputSpacer>–</DateInputSpacer>
})

const DateInputSpacer = styled.div`
  padding: 6px;
`
