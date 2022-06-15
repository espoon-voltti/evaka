// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { errorToInputInfo } from 'employee-frontend/utils/validation/input-info-helper'
import { QuestionOption } from 'lib-common/api-types/vasu'
import { ErrorKey } from 'lib-common/form-validation'
import LocalDate from 'lib-common/local-date'
import Radio from 'lib-components/atoms/form/Radio'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { fasExclamationTriangle } from 'lib-icons'

import type { RadioGroupSelectedValue } from './RadioGroupQuestion'

export const OptionContainer = styled.div`
  display: flex;
  gap: 8px;
`

const Warning = styled.span`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`

const WarningIcon = styled(FontAwesomeIcon)`
  margin-left: 8px;
  margin-right: 8px;
`

interface Props {
  option: QuestionOption
  selectedValue: RadioGroupSelectedValue | null
  onChange: (selected: RadioGroupSelectedValue) => void
  isSelected: boolean
  onSelected: () => void
}

function usePendingUserInput<T>(
  transform: (input: string) => T | null,
  initialValue?: string
) {
  const [input, setInput] = useState(initialValue ?? '')
  const [validated, setValidated] = useState<T>()

  useEffect(() => {
    const transformed = transform(input)

    if (transformed) {
      setValidated(transformed)
    } else {
      setValidated(undefined)
    }
  }, [input, transform])

  return [input, setInput, validated] as [
    string,
    React.Dispatch<React.SetStateAction<string>>,
    T | undefined
  ]
}

const getRangeError = (
  input: string,
  valid: boolean,
  otherValid: boolean
): ErrorKey | undefined => {
  if (!input) {
    return 'required'
  }

  if (!valid) {
    if (input) {
      return 'validDate'
    }

    if (otherValid) {
      return 'required'
    }
  }

  return undefined
}

const transformDate = (d: string) => LocalDate.parseFiOrNull(d)

export default React.memo(function RadioGroupQuestionOption({
  option,
  selectedValue,
  onChange,
  isSelected,
  onSelected
}: Props) {
  const { i18n } = useTranslation()

  const { colors } = useTheme()

  const selectedStartDate =
    (isSelected && selectedValue?.range?.start) || undefined
  const selectedEndDate = (isSelected && selectedValue?.range?.end) || undefined

  const [startInput, setStartInput, startDate] = usePendingUserInput(
    transformDate,
    selectedStartDate?.format()
  )
  const [endInput, setEndInput, endDate] = usePendingUserInput(
    transformDate,
    selectedEndDate?.format()
  )

  const rangeIsLinear =
    startDate && endDate && startDate.isEqualOrBefore(endDate)

  const startDateError = isSelected
    ? getRangeError(startInput, !!startDate, !!endDate)
    : undefined
  const endDateError = isSelected
    ? getRangeError(endInput, !!endDate, !!startDate)
    : undefined

  useEffect(() => {
    if (
      isSelected &&
      startDate &&
      endDate &&
      !startDateError &&
      !endDateError &&
      rangeIsLinear
    ) {
      const rangeHasChanged =
        !selectedValue?.range?.start.isEqual(startDate) ||
        !selectedValue?.range?.end.isEqual(endDate)
      if (rangeHasChanged) {
        onChange({
          range: {
            start: startDate,
            end: endDate
          },
          key: option.key
        })
      }
    }
  }, [
    startDate,
    endDate,
    onChange,
    option,
    selectedValue,
    isSelected,
    startDateError,
    endDateError,
    rangeIsLinear
  ])

  return (
    <OptionContainer>
      <Radio
        checked={isSelected}
        label={option.name}
        onChange={() =>
          option.dateRange
            ? onSelected()
            : onChange({ key: option.key, range: null })
        }
        data-qa={`radio-group-date-question-option-${option.key}`}
      />

      {option.dateRange && (
        <div>
          <DatePicker
            locale="fi"
            date={isSelected ? startInput : ''}
            onChange={setStartInput}
            info={
              startDateError &&
              errorToInputInfo(startDateError, i18n.validationErrors)
            }
            isValidDate={(d) => !endDate || d.isEqualOrBefore(endDate)}
            data-qa={`radio-group-date-question-option-${option.key}-range-start`}
          />
          <span>-</span>
          <DatePicker
            locale="fi"
            date={isSelected ? endInput : ''}
            onChange={setEndInput}
            info={
              endDateError &&
              errorToInputInfo(endDateError, i18n.validationErrors)
            }
            isValidDate={(d) => !startDate || d.isEqualOrAfter(startDate)}
            data-qa={`radio-group-date-question-option-${option.key}-range-end`}
          />
          {rangeIsLinear === false && (
            <Warning>
              <WarningIcon
                icon={fasExclamationTriangle}
                color={colors.status.warning}
              />
              {i18n.validationErrors.dateRangeNotLinear}
            </Warning>
          )}
        </div>
      )}
    </OptionContainer>
  )
})
