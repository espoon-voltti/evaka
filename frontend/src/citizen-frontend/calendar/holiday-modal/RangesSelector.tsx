// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange } from 'lib-common/form/fields'
import { array, object, required, transformed } from 'lib-common/form/form'
import { useForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import {
  StateOf,
  ValidationError,
  ValidationSuccess
} from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'
import { faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../localization'
import { LocalizationContext } from '../../localization/state'

interface Props {
  period: FiniteDateRange
  value: FiniteDateRange[]
  onSelectRanges: (selection: FiniteDateRange[]) => void
}

function checkForOverlappingRanges(ranges: FiniteDateRange[]): boolean {
  const rangesProcessed: FiniteDateRange[] = []

  for (const value of ranges) {
    if (rangesProcessed.some((range) => value.overlaps(range))) {
      return true
    }
    rangesProcessed.push(value)
  }
  return false
}

const rangesForm = transformed(
  object({
    period: required(localDateRange()),
    ranges: array(required(localDateRange()))
  }),
  ({ period, ranges }) => {
    if (ranges.length > 0) {
      if (ranges.some((range) => range.start.isBefore(period.start))) {
        return ValidationError.field('ranges', 'dateTooEarly')
      }
      if (ranges.some((range) => range.end.isAfter(period.end))) {
        return ValidationError.field('ranges', 'dateTooLate')
      }
      if (checkForOverlappingRanges(ranges)) {
        return ValidationError.field('ranges', 'rangesOverlap')
      }
    }
    return ValidationSuccess.of(ranges)
  }
)

function initialFormState(
  period: FiniteDateRange,
  ranges: FiniteDateRange[]
): StateOf<typeof rangesForm> {
  return {
    period: localDateRange.fromRange(period),
    ranges:
      ranges.length > 0
        ? ranges.map((range) => localDateRange.fromRange(range))
        : [localDateRange.empty()]
  }
}

export const RangeSelector = React.memo(function RangeSelector({
  period,
  value,
  onSelectRanges
}: Props) {
  const i18n = useTranslation()
  const { lang } = useContext(LocalizationContext)

  const form = useForm(
    rangesForm,
    () => initialFormState(period, value),
    {
      ...i18n.validationErrors,
      rangesOverlap: i18n.calendar.holidayModal.rangesOverlap
    },
    {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onUpdate: (prevState, nextState, form) => {
        const shape = form.shape()
        const next = shape.ranges.validate(nextState.ranges)

        if (!next.isValid) return nextState
        const ranges = next.value
        onSelectRanges(ranges)

        return nextState
      }
    }
  )

  const { ranges } = useFormFields(form)
  const rangeElems = useFormElems(ranges)

  const addRangeEntry = useCallback(() => {
    ranges.update((prev) => [...prev, localDateRange.empty()])
  }, [ranges])

  const removeRangeEntry = useCallback(
    (index: number) => {
      ranges.update((prev) => [
        ...prev.slice(0, index),
        ...prev.slice(index + 1)
      ])
    },
    [ranges]
  )

  return (
    <div>
      {rangeElems.map((range, i) => (
        <FixedSpaceRow
          key={`tb-${i}`}
          data-qa="range"
          alignItems="center"
          spacing="m"
        >
          <div className="bold">{`${i + 1}.`}</div>
          <DateRangePickerF
            hideErrorsBeforeTouched
            bind={range}
            locale={lang}
            data-qa={`range-input-${i + 1}`}
            initialMonth={
              LocalDate.parseFiOrNull(range.state.start) ?? period.start
            }
          />

          <Button
            appearance="inline"
            text={i18n.common.delete}
            icon={faTrash}
            data-qa="range-break-button"
            onClick={() => removeRangeEntry(i)}
          />
        </FixedSpaceRow>
      ))}

      {!ranges.isValid() && ranges.inputInfo() ? (
        <InputFieldUnderRow className="warning">
          <span data-qa="remove-range-info">{ranges.inputInfo()?.text}</span>
          <UnderRowStatusIcon status="warning" />
        </InputFieldUnderRow>
      ) : undefined}

      <Gap size="L" />

      <Button
        appearance="inline"
        text={i18n.calendar.holidayModal.addTimePeriod}
        icon={faPlus}
        data-qa="add-range-button"
        onClick={addRangeEntry}
      />
    </div>
  )
})
