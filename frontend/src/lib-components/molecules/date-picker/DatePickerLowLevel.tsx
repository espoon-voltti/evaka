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
import type { OnSelectHandler } from 'react-day-picker'
import { createPortal } from 'react-dom'
import FocusLock from 'react-focus-lock'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { useTranslations } from 'lib-components/i18n'
import { faCalendarAlt } from 'lib-icons'

import type { InputInfo } from '../../atoms/form/InputField'
import { fontWeights } from '../../typography'
import { defaultMargins } from '../../white-space'

import DatePickerDay from './DatePickerDay'
import DatePickerInput from './DatePickerInput'
import { nativeDatePickerEnabled } from './helpers'

const inputWidth = 120
const iconWidth = 36

const DatePickerWrapper = styled.div`
  position: relative;
  display: inline-flex;
  width: ${inputWidth + iconWidth}px;
`

const DayPickerPositioner = styled.div`
  position: fixed;
  z-index: 99999;
  justify-content: center;
  align-items: center;
  display: inline-block;
`

const pickerHeight = 350

const DayPickerDiv = styled.div`
  height: ${pickerHeight}px;
  padding: ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;

  .rdp-root {
    --rdp-day-width: 40px;
    --rdp-day-height: 40px;
    --rdp-weekday-padding: 0;
    --rdp-selected-font: inherit;
    --rdp-selected-border: none;
  }

  .rdp-month_caption {
    font-family: inherit;
    font-size: 18px;
    font-weight: ${fontWeights.medium};
  }

  .rdp-chevron {
    fill: #0f0f0f;
  }

  .rdp-caption_label {
    padding: 0 0.5em;
  }

  .rdp-day {
    padding: 0;
  }

  .rdp-weekday {
    text-transform: none;
    font-size: 1em;
    font-weight: ${fontWeights.normal};
  }

  .rdp-today {
    color: ${(p) => p.theme.colors.accents.a2orangeDark};
    font-weight: ${fontWeights.bold};
  }

  .rdp-day_button:not([disabled]) {
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

  .rdp-selected:not([disabled]) .rdp-day_button {
    color: ${(p) => p.theme.colors.grayscale.g0};
    background-color: ${(p) => p.theme.colors.main.m2Active};
  }
`

export interface DatePickerLowLevelProps {
  value: string
  onChange: (value: string) => void
  onFocus?: (e: React.FocusEvent<HTMLInputElement>) => void
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void
  locale: 'fi' | 'sv' | 'en'
  info?: InputInfo
  hideErrorsBeforeTouched?: boolean
  disabled?: boolean
  'data-qa'?: string
  id?: string
  required?: boolean
  initialMonth?: LocalDate
  minDate?: LocalDate
  maxDate?: LocalDate
  useBrowserPicker?: boolean
}

export default React.memo(function DatePickerLowLevel({
  value,
  onChange,
  onBlur,
  locale,
  info,
  hideErrorsBeforeTouched,
  disabled,
  id,
  required,
  initialMonth,
  minDate,
  maxDate,
  useBrowserPicker = nativeDatePickerEnabled,
  'data-qa': dataQa
}: DatePickerLowLevelProps) {
  const [datePickerShown, useDatePickerShown] = useBoolean(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const pickerRef = useRef<HTMLDivElement>(null)
  const [isDatePicked, setIsDatePicked] = useState(false)

  const hideDatePicker = useDatePickerShown.off
  const toggleDatePicker = useDatePickerShown.toggle

  const i18n = useTranslations()

  const handleDateSelect = useCallback<OnSelectHandler<Date | undefined>>(
    (day, _triggerDate, modifiers) => {
      if (modifiers?.disabled) {
        return
      }
      setIsDatePicked(true)
      hideDatePicker()
      if (day) {
        onChange(LocalDate.fromSystemTzDate(day).format())
      }
    },
    [hideDatePicker, onChange]
  )

  const handleEsc = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') {
        // If the datepicker is inside a parent that also closes with Escape
        // (e.g. a modal), this prevents the parent from closing
        e.stopPropagation()
        hideDatePicker()
      }
    },
    [hideDatePicker]
  )

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      const date = LocalDate.parseFiOrNull(e.target.value)
      if (date !== null) {
        onChange(date.format())
      }
      onBlur?.(e)
    },
    [onBlur, onChange]
  )

  useFloatingPositioning({
    anchorRef: wrapperRef,
    floatingRef: pickerRef,
    floatingHeight: pickerHeight,
    active: datePickerShown
  })

  useEffect(() => {
    const handleClickOutside = (event: {
      target: EventTarget | null
      type: string
    }) => {
      if (
        event.target instanceof Element &&
        !wrapperRef.current?.contains(event.target) &&
        !pickerRef.current?.contains(event.target)
      ) {
        hideDatePicker()
      }
    }

    if (datePickerShown) {
      addEventListener('pointerup', handleClickOutside)

      return () => {
        removeEventListener('pointerup', handleClickOutside)
      }
    }

    return () => undefined
  }, [hideDatePicker, datePickerShown])

  return (
    <DatePickerWrapper ref={wrapperRef}>
      <DatePickerInput
        value={value}
        onChange={onChange}
        onBlur={!datePickerShown ? handleBlur : onBlur}
        disabled={disabled}
        info={datePickerShown ? undefined : info}
        data-qa={dataQa}
        id={id}
        required={required}
        locale={locale}
        useBrowserPicker={useBrowserPicker}
        hideErrorsBeforeTouched={!isDatePicked && hideErrorsBeforeTouched}
        minDate={minDate}
        maxDate={maxDate}
      />
      <StyledIconButton
        icon={faCalendarAlt}
        onClick={toggleDatePicker}
        disabled={disabled}
        aria-controls="dialog"
        aria-haspopup="dialog"
        aria-expanded={datePickerShown}
        aria-label={
          datePickerShown ? i18n.datePicker.close : i18n.datePicker.open
        }
      />
      {!useBrowserPicker && datePickerShown
        ? createPortal(
            <FocusLock returnFocus>
              <DayPickerPositioner ref={pickerRef} onKeyDown={handleEsc}>
                <DayPickerDiv>
                  <DatePickerDay
                    locale={locale}
                    inputValue={value}
                    initialMonth={initialMonth}
                    onSelect={handleDateSelect}
                    minDate={minDate}
                    maxDate={maxDate}
                  />
                </DayPickerDiv>
              </DayPickerPositioner>
            </FocusLock>,
            document.getElementById('datepicker-container') ?? document.body
          )
        : null}
    </DatePickerWrapper>
  )
})

const StyledIconButton = styled(IconOnlyButton)`
  width: 100%;
  margin: 0 0 0 4px;
`

export function useFloatingPositioning({
  anchorRef,
  floatingRef,
  floatingHeight,
  active
}: {
  anchorRef: React.RefObject<HTMLElement | null>
  floatingRef: React.RefObject<HTMLElement | null>
  floatingHeight: number
  active: boolean
}) {
  const position = useCallback(() => {
    // Minimum distance of the floating element from viewport edges
    const margin = 4 // px

    const viewportWidth = window.innerWidth
    const viewportHeight = window.innerHeight
    const scrollBarWidth = viewportWidth - document.documentElement.clientWidth

    const verticalMargin = margin
    const horizontalMargin = scrollBarWidth + margin

    const anchor = anchorRef.current
    const floating = floatingRef.current
    if (!anchor || !floating) return

    const anchorRect = anchor.getBoundingClientRect()

    const spaceBelow = viewportHeight - anchorRect.bottom
    const spaceAbove = anchorRect.top
    const openAbove = spaceBelow < floatingHeight && spaceAbove > spaceBelow
    const top = openAbove
      ? anchorRect.top - floating.offsetHeight - verticalMargin
      : anchorRect.bottom + verticalMargin

    const maxLeft = viewportWidth - floating.offsetWidth - horizontalMargin
    const minLeft = horizontalMargin
    const left = Math.min(Math.max(anchorRect.left, minLeft), maxLeft)

    Object.assign(floating.style, {
      top: `${top}px`,
      left: `${left}px`
    })
  }, [anchorRef, floatingHeight, floatingRef])

  useLayoutEffect(() => {
    if (!active) return
    position()
  }, [active, position])

  useEffect(() => {
    if (!active) return

    const anchor = anchorRef.current
    const floating = floatingRef.current
    if (!anchor || !floating) return

    const scrollables = getScrollableAncestors(anchor)
    scrollables.forEach((el) =>
      el.addEventListener('scroll', position, { passive: true })
    )
    window.addEventListener('resize', position)

    return () => {
      scrollables.forEach((el) => el.removeEventListener('scroll', position))
      window.removeEventListener('resize', position)
    }
  }, [active, anchorRef, floatingRef, position])
}

function getScrollableAncestors(el: HTMLElement): (HTMLElement | Window)[] {
  const scrollables: (HTMLElement | Window)[] = []

  let parent = el.parentElement
  while (parent) {
    if (isScrollable(parent)) scrollables.push(parent)
    parent = parent.parentElement
  }
  scrollables.push(window)

  return scrollables
}

function isScrollable(el: HTMLElement) {
  const { overflowX, overflowY } = window.getComputedStyle(el)
  return (
    /(auto|scroll|overlay)/.test(overflowX) ||
    /(auto|scroll|overlay)/.test(overflowY)
  )
}
