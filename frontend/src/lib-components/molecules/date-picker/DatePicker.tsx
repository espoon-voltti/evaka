// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState
} from 'react'
import { DayModifiers } from 'react-day-picker'
import styled from 'styled-components'

import { BoundFormState } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'

import { InputInfo } from '../../atoms/form/InputField'
import { useTranslations } from '../../i18n'
import { fontWeights } from '../../typography'
import { defaultMargins } from '../../white-space'

import DatePickerDay from './DatePickerDay'
import DatePickerInput from './DatePickerInput'

const inputWidth = 120

const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-block;
  width: ${inputWidth}px;
`
const minMargin = 16
const overflow = 100

const DayPickerPositioner = styled.div<{ openAbove?: boolean }>`
  position: absolute;
  ${({ openAbove }) =>
    openAbove ? 'bottom' : 'top'}: calc(2.5rem + ${minMargin}px);
  left: -${overflow}px;
  right: -${overflow}px;
  z-index: 99999;
  justify-content: center;
  align-items: center;
  display: inline-block;
`

const DayPickerDiv = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  padding: ${defaultMargins.s} 0;
  border-radius: 2px;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;

  .rdp-caption_label {
    font-weight: ${fontWeights.medium};
  }

  .rdp-head_cell {
    text-transform: none;
    font-size: 1em;
    font-weight: ${fontWeights.normal};
  }

  .rdp-day_today {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
  }

  .rdp-button:not([disabled]) {
    &:hover {
      color: ${(p) => p.theme.colors.grayscale.g100};
      background-color: ${(p) => p.theme.colors.accents.a8lightBlue};
    }

    &:active,
    &:focus {
      color: ${(p) => p.theme.colors.grayscale.g100};
      background-color: ${(p) => p.theme.colors.grayscale.g0};
      border: 2px solid ${(p) => p.theme.colors.main.m2Active};
    }
  }

  .rdp-day_selected:not([disabled]) {
    color: ${(p) => p.theme.colors.grayscale.g0};
    background-color: ${(p) => p.theme.colors.main.m2Active};
  }
`

interface BaseDatePickerProps {
  date: LocalDate | null
  onChange: (date: LocalDate | null) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: () => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  initialMonth?: LocalDate
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
}

type WithoutMinMaxBaseProps = Omit<BaseDatePickerProps, 'minDate' | 'maxDate'>

interface MinDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: never
}

interface MaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: never
  maxDate?: LocalDate
}

interface MinMaxDatePickerProps extends WithoutMinMaxBaseProps {
  minDate?: LocalDate
  maxDate?: LocalDate
}

export type DatePickerProps =
  | BaseDatePickerProps
  | MinDatePickerProps
  | MaxDatePickerProps
  | MinMaxDatePickerProps

const nativeDatePickerAgentMatchers = [/Android/i]

const checkDateInputSupport = () => {
  if (!nativeDatePickerAgentMatchers.some((m) => m.test(navigator.userAgent))) {
    return false
  }

  const input = document.createElement('input')
  input.setAttribute('type', 'date')

  const testDate = LocalDate.of(2020, 2, 11)
  input.setAttribute('value', testDate.formatIso())

  return (
    !!input.valueAsDate &&
    LocalDate.fromSystemTzDate(input.valueAsDate).isEqual(testDate)
  )
}

export const nativeDatePickerEnabled = checkDateInputSupport()

const DatePicker = React.memo(function DatePicker({
  date,
  onChange,
  onFocus = () => undefined,
  onBlur = () => undefined,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  isInvalidDate,
  id,
  required,
  initialMonth,
  openAbove,
  maxDate,
  minDate,
  ...props
}: DatePickerProps) {
  const i18n = useTranslations()
  const [showDatePicker, setShowDatePicker] = useState<boolean>(false)
  const [showErrors, setShowErrors] = useState(!hideErrorsBeforeTouched)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)

  const [internalError, setInternalError] = useState<InputInfo>()

  if (!hideErrorsBeforeTouched && !showErrors) {
    setShowErrors(true)
  }

  const handleChange = useCallback(
    (value: LocalDate | null) => {
      if (value?.formatIso() !== date?.formatIso()) {
        if (value === null) {
          onChange(null)
        } else if (minDate && value.isBefore(minDate)) {
          setInternalError({
            text: i18n.datePicker.validationErrors.dateTooEarly,
            status: 'warning'
          })
        } else if (maxDate && value.isAfter(maxDate)) {
          setInternalError({
            text: i18n.datePicker.validationErrors.dateTooLate,
            status: 'warning'
          })
        } else {
          const validationError = isInvalidDate?.(value)
          if (validationError) {
            setInternalError({ text: validationError, status: 'warning' })
          } else {
            setInternalError(undefined)
            onChange(value)
          }
        }
      } else {
        setInternalError(undefined)
      }
    },
    [date, i18n, isInvalidDate, maxDate, minDate, onChange]
  )

  const hideDatePicker = useCallback(() => {
    setShowDatePicker(false)
    setShowErrors(true)
  }, [])

  const handleUserKeyPress = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Esc' || e.key === 'Escape' || e.key === 'Enter') {
        hideDatePicker()
      }
    },
    [hideDatePicker]
  )

  const handleDayClick = useCallback(
    (day: Date, modifiers?: DayModifiers) => {
      if (modifiers?.disabled) {
        return
      }
      hideDatePicker()
      handleChange(LocalDate.fromSystemTzDate(day))
    },
    [handleChange, hideDatePicker]
  )

  useLayoutEffect(() => {
    if (showDatePicker) {
      const realignPicker = () => {
        if (wrapperRef.current) {
          const distanceFromLeftEdge = wrapperRef.current.offsetLeft
          const distanceFromRightEdge =
            window.innerWidth - wrapperRef.current.offsetLeft - inputWidth

          const leftOffset =
            overflow - Math.min(overflow, distanceFromLeftEdge - minMargin)
          const rightOffset =
            overflow - Math.min(overflow, distanceFromRightEdge - minMargin)

          if (pickerRef.current && (leftOffset !== 0 || rightOffset !== 0)) {
            const left = -overflow + leftOffset - rightOffset
            pickerRef.current.style['left'] = `${left}px`
            const right = -overflow - leftOffset + rightOffset
            pickerRef.current.style['right'] = `${right}px`
          }
        }
      }
      realignPicker()
      addEventListener('resize', realignPicker, { passive: true })
      return () => removeEventListener('resize', realignPicker)
    }

    return
  }, [showDatePicker])

  useEffect(() => {
    function handleEvent(event: { target: EventTarget | null; type: string }) {
      if (event.target instanceof Element) {
        if (wrapperRef.current?.contains(event.target)) {
          return
        }
        if (
          event.type === 'pointerup' &&
          event.target.classList.contains('modal-container')
        ) {
          return // do not close when clicking on modal scrollbar
        }
      }

      hideDatePicker()
    }

    if (showDatePicker) {
      addEventListener('focusin', handleEvent)
      addEventListener('pointerup', handleEvent)

      return () => {
        removeEventListener('focusin', handleEvent)
        removeEventListener('pointerup', handleEvent)
      }
    }

    return () => undefined
  }, [hideDatePicker, showDatePicker])

  return (
    <DatePickerWrapper ref={wrapperRef} onKeyDown={handleUserKeyPress}>
      <DatePickerInput
        date={date}
        setDate={handleChange}
        disabled={disabled}
        onFocus={(ev) => {
          setShowDatePicker(true)
          onFocus(ev)
        }}
        onBlur={() => {
          onBlur()
        }}
        info={showErrors ? internalError ?? info : undefined}
        data-qa={props['data-qa']}
        id={id}
        required={required}
        locale={locale}
        useBrowserPicker={nativeDatePickerEnabled}
        minDate={minDate}
        maxDate={maxDate}
        hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        datePickerVisible={showDatePicker}
      />
      {!nativeDatePickerEnabled && showDatePicker ? (
        <DayPickerPositioner ref={pickerRef} openAbove={openAbove}>
          <DayPickerDiv>
            <DatePickerDay
              locale={locale}
              inputValue={date}
              handleDayClick={handleDayClick}
              minDate={minDate}
              maxDate={maxDate}
              isInvalidDate={isInvalidDate && ((date) => !!isInvalidDate(date))}
              initialMonth={initialMonth}
            />
          </DayPickerDiv>
        </DayPickerPositioner>
      ) : null}
    </DatePickerWrapper>
  )
})

export default DatePicker

export const DatePickerSpacer = React.memo(function DatePickerSpacer() {
  return <DateInputSpacer>–</DateInputSpacer>
})

const DateInputSpacer = styled.div`
  padding: 6px;
`

export interface DatePickerFProps
  extends Omit<DatePickerProps, 'date' | 'onChange'> {
  bind: BoundFormState<LocalDate | null>
}

export const DatePickerF = React.memo(function DatePickerF({
  bind: { state, set, inputInfo },
  ...props
}: DatePickerFProps) {
  return (
    <DatePicker
      {...props}
      date={state}
      onChange={set}
      info={'info' in props ? props.info : inputInfo()}
    />
  )
})
