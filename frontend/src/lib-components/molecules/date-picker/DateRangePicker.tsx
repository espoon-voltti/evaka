// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DateValue } from '@internationalized/date'
import { useDateRangePicker } from '@react-aria/datepicker'
import { useDateRangePickerState } from '@react-stately/datepicker'
import classNames from 'classnames'
import React, { useEffect, useRef, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import {
  scrollIntoViewSoftKeyboard,
  scrollRefIntoView
} from 'lib-common/utils/scrolling'
import Popover from 'lib-components/atoms/Popover'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faCalendarAlt } from 'lib-icons'

import { InputInfo } from '../../atoms/form/InputField'
import { defaultMargins, Gap } from '../../white-space'

import { CalendarAriaProps } from './Calendar'
import DateField from './DateField'
import RangeCalendar from './RangeCalendar'
interface BaseDatePickerProps {
  default: { start?: LocalDate; end?: LocalDate } | null
  onChange: (range: FiniteDateRange | null) => void
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
    inconsistentRange: string
    required: string
  }
  openAbove?: boolean

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

  'aria-labelledby': string

  labels: CalendarAriaProps
}

type WithoutMinMaxBaseProps = Omit<BaseDatePickerProps, 'minDate' | 'maxDate'>

interface MinDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: never
  errorTexts: BaseDatePickerProps['errorTexts'] & {
    dateTooEarly: string
    /** TODO: startDateTooEarly, endDateTooEarly, etc V */
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

export type DatePickerProps =
  | BaseDatePickerProps
  | MinDatePickerProps
  | MaxDatePickerProps
  | MinMaxDatePickerProps

const DatePickerContainer = styled.div`
  position: relative;
  width: fit-content;
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

export default React.memo(function DateRangePicker(props: DatePickerProps) {
  const [error, setError] = useState<string>()

  const state = useDateRangePickerState({
    onChange: (newRange) => {
      if (!newRange || !newRange.start || !newRange.end) {
        props.onChange(null)

        if (props.required) {
          setError(props.errorTexts.required)
        }

        return
      }

      const { start, end } = newRange

      if (start.year < 1800 || end.year < 1800) {
        // years such as "202" are not supported (usually happens
        // when typing, e.g., 2022), but they may be emitted here;
        // this simple check is to ensure those cases are not handled
        return
      }

      const startDate = LocalDate.tryCreate(start.year, start.month, start.day)
      if (!startDate) {
        props.onChange(null)
        return
      }

      const endDate = LocalDate.tryCreate(end.year, end.month, end.day)
      if (!endDate) {
        props.onChange(null)
        return
      }

      if (endDate.isBefore(startDate)) {
        setError(props.errorTexts.inconsistentRange)
        return
      }

      props.onChange(new FiniteDateRange(startDate, endDate))
    },
    defaultValue: props.default
      ? {
          // These are optional fields, but the typings are incorrect.
          start: props.default.start?.asCalendarDate() as DateValue,
          end: props.default.end?.asCalendarDate() as DateValue
        }
      : undefined,
    placeholderValue: props.initialMonth?.asCalendarDate()
  })
  const ref = React.useRef<HTMLDivElement>(null)
  const {
    groupProps,
    startFieldProps,
    endFieldProps,
    buttonProps,
    dialogProps,
    calendarProps
  } = useDateRangePicker(
    {
      minValue: props.minDate?.asCalendarDate(),
      maxValue: props.maxDate?.asCalendarDate(),
      isDateUnavailable:
        props.isInvalidDate &&
        ((date) => {
          const localDate = LocalDate.tryCreate(date.year, date.month, date.day)
          return localDate ? !!props.isInvalidDate?.(localDate) : false
        }),
      isDisabled: props.disabled,
      'aria-labelledby': props['aria-labelledby']
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
      (state.value.start?.compare(props.minDate.asCalendarDate()) < 0 ||
        state.value.end?.compare(props.minDate.asCalendarDate()) < 0)
    ) {
      setError(props.errorTexts.dateTooEarly)
      return
    }

    if (
      props.maxDate &&
      (state.value.start?.compare(props.maxDate.asCalendarDate()) > 0 ||
        state.value.end?.compare(props.maxDate.asCalendarDate()) > 0)
    ) {
      setError(props.errorTexts.dateTooLate)
      return
    }
  }, [state.value, props.minDate, props.maxDate, props.errorTexts])

  const popoverRef = useRef<HTMLDivElement>(null)

  const [popoverOpen, setPopoverOpen] = useState(state.isOpen)

  useEffect(() => {
    if (state.isOpen) {
      setPopoverOpen(true)
      return undefined
    } else {
      const timeout = setTimeout(() => {
        setPopoverOpen(false)
      }, 50)
      return () => {
        clearTimeout(timeout)
      }
    }
  }, [state.isOpen])

  return (
    <DatePickerContainer>
      <DateInput
        {...groupProps}
        ref={ref}
        className={classNames({ warning: error })}
        data-qa={props['data-qa']}
      >
        <DateField
          {...startFieldProps}
          onFocus={(ev) => {
            props.onFocus?.(ev)
            startFieldProps.onFocus?.(ev)

            if (ref.current) {
              scrollIntoViewSoftKeyboard(ref.current, 'center')
            }
          }}
          locale={props.locale}
          data-qa="start-date"
        />
        <span>–</span>
        <DateField
          {...endFieldProps}
          onFocus={(ev) => {
            props.onFocus?.(ev)
            endFieldProps.onFocus?.(ev)

            if (ref.current) {
              scrollIntoViewSoftKeyboard(ref.current, 'center')
            }
          }}
          locale={props.locale}
          data-qa="end-date"
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
                  // Scrolls the date picker into view when it is opened
                  // in a scrollable container that would put the picker
                  // below the bottom boundary (e.g., reservations modal)
                  scrollRefIntoView(popoverRef, 10, {
                    block: 'end'
                  })
                }
              }
            }, 1)
          }}
        />
      </DateInput>
      <Gap size="xxs" />
      {!!error && (
        <InputFieldUnderRow className="warning">{error}</InputFieldUnderRow>
      )}
      {popoverOpen && (
        <Popover
          {...dialogProps}
          openAbove={props.openAbove}
          isOpen={popoverOpen}
          onClose={() => state.setOpen(false)}
          popoverRef={popoverRef}
        >
          <RangeCalendar {...calendarProps} {...props.labels} />
        </Popover>
      )}
    </DatePickerContainer>
  )
})
