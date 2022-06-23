// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useState } from 'react'
import styled, { useTheme } from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { QuestionOption } from 'lib-common/api-types/vasu'
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

  const [startDate, setStartDate] = useState(selectedStartDate ?? null)
  const [endDate, setEndDate] = useState(selectedEndDate ?? null)

  const rangeIsLinear =
    startDate && endDate && startDate.isEqualOrBefore(endDate)

  useEffect(() => {
    if (isSelected && startDate && endDate && rangeIsLinear) {
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
            date={isSelected ? startDate : null}
            onChange={setStartDate}
            maxDate={endDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-start`}
            errorTexts={i18n.validationErrors}
            hideErrorsBeforeTouched
          />
          <span>-</span>
          <DatePicker
            locale="fi"
            date={isSelected ? endDate : null}
            onChange={setEndDate}
            minDate={startDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-end`}
            errorTexts={i18n.validationErrors}
            hideErrorsBeforeTouched
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
