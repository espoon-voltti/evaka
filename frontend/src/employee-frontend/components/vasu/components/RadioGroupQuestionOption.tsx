// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { QuestionOption } from 'lib-common/api-types/vasu'
import { VasuLanguage } from 'lib-common/generated/api-types/vasu'
import Radio from 'lib-components/atoms/form/Radio'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { VasuTranslations } from 'lib-customizations/employee'

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
  lang: VasuLanguage
  translations: VasuTranslations
}

export default React.memo(function RadioGroupQuestionOption({
  option,
  selectedValue,
  onChange,
  isSelected,
  onSelected,
  lang,
  translations
}: Props) {
  const selectedStartDate =
    (isSelected && selectedValue?.range?.start) || undefined
  const selectedEndDate = (isSelected && selectedValue?.range?.end) || undefined

  const [startDate, setStartDate] = useState(selectedStartDate ?? null)
  const [endDate, setEndDate] = useState(selectedEndDate ?? null)

  useEffect(() => {
    if (isSelected && option.dateRange && startDate && endDate) {
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
    option.key,
    option.dateRange,
    selectedValue,
    isSelected
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
            locale={lang === 'SV' ? 'sv' : 'fi'}
            date={isSelected ? startDate : null}
            onChange={setStartDate}
            maxDate={endDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-start`}
            errorTexts={translations.validationErrors}
            hideErrorsBeforeTouched
          />
          <span>-</span>
          <DatePicker
            locale={lang === 'SV' ? 'sv' : 'fi'}
            date={isSelected ? endDate : null}
            onChange={setEndDate}
            minDate={startDate ?? undefined}
            data-qa={`radio-group-date-question-option-${option.key}-range-end`}
            errorTexts={translations.validationErrors}
            hideErrorsBeforeTouched
          />
        </div>
      )}
    </OptionContainer>
  )
})
