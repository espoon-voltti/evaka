// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { QuestionOption } from 'lib-common/api-types/vasu'
import { OfficialLanguage } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import Radio from 'lib-components/atoms/form/Radio'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'

import type { RadioGroupSelectedValue } from './RadioGroupQuestion'

export const OptionContainer = styled.div`
  display: flex;
  gap: 8px;
`

interface Props {
  option: QuestionOption
  selectedValue: RadioGroupSelectedValue | null
  onChange: (selected: RadioGroupSelectedValue) => void
  isSelected: boolean
  onSelected: () => void
  lang: OfficialLanguage
}

export default React.memo(function RadioGroupQuestionOption({
  option,
  selectedValue,
  onChange,
  isSelected,
  onSelected,
  lang
}: Props) {
  const selectedStartDate =
    (isSelected && selectedValue?.range?.start) || undefined
  const selectedEndDate = (isSelected && selectedValue?.range?.end) || undefined

  const [startDate, setStartDate] = useState(selectedStartDate ?? null)
  const [endDate, setEndDate] = useState(selectedEndDate ?? null)

  const handleChange = useCallback(
    (checked: boolean, start: LocalDate | null, end: LocalDate | null) => {
      if (option.dateRange) {
        if (checked && start && end) {
          const rangeHasChanged =
            !selectedValue?.range?.start.isEqual(start) ||
            !selectedValue?.range?.end.isEqual(end)
          if (rangeHasChanged) {
            onChange({ key: option.key, range: { start, end } })
          }
        } else if (checked) {
          onSelected()
        }
      } else {
        onChange({ key: option.key, range: null })
      }
    },
    [
      onChange,
      onSelected,
      option.dateRange,
      option.key,
      selectedValue?.range?.end,
      selectedValue?.range?.start
    ]
  )

  const handleRadioChange = useCallback(() => {
    handleChange(true, startDate, endDate)
  }, [handleChange, startDate, endDate])

  const handleStartDateChange = useCallback(
    (start: LocalDate | null) => {
      setStartDate(start)
      handleChange(isSelected, start, endDate)
    },
    [handleChange, isSelected, endDate]
  )

  const handleEndDateChange = useCallback(
    (end: LocalDate | null) => {
      setEndDate(end)
      handleChange(isSelected, startDate, end)
    },
    [handleChange, isSelected, startDate]
  )

  return (
    <OptionContainer>
      <Radio
        checked={isSelected}
        label={option.name}
        onChange={handleRadioChange}
        data-qa={`radio-group-date-question-option-${option.key}`}
      />

      {option.dateRange && (
        <div>
          <DatePicker
            locale={lang === 'SV' ? 'sv' : 'fi'}
            date={startDate}
            onChange={handleStartDateChange}
            maxDate={endDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-start`}
            hideErrorsBeforeTouched
          />
          <span>-</span>
          <DatePicker
            locale={lang === 'SV' ? 'sv' : 'fi'}
            date={endDate}
            onChange={handleEndDateChange}
            minDate={startDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-end`}
            hideErrorsBeforeTouched
          />
        </div>
      )}
    </OptionContainer>
  )
})
