// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { QuestionOption } from 'lib-common/api-types/vasu'
import FiniteDateRange from 'lib-common/finite-date-range'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import Radio from 'lib-components/atoms/form/Radio'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { Bold } from 'lib-components/typography'

import QuestionInfo from '../QuestionInfo'

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
}

export default React.memo(function RadioGroupQuestionOption({
  option,
  selectedValue,
  onChange,
  isSelected,
  onSelected
}: Props) {
  const { i18n } = useTranslation()

  const [range, setRange] = useState(selectedValue?.range ?? undefined)

  useEffect(() => {
    if (isSelected && range) {
      const rangeHasChanged =
        !selectedValue?.range?.start.isEqual(range.start) ||
        !selectedValue?.range?.end.isEqual(range.end)
      if (rangeHasChanged) {
        onChange({
          range,
          key: option.key
        })
      }
    }
  }, [range, onChange, option, selectedValue, isSelected])

  const ariaId = useUniqueId()

  if (option.isIntervention) {
    return (
      <QuestionInfo info={option.info ?? null}>
        <Bold>{option.name}</Bold>
      </QuestionInfo>
    )
  }

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
        id={ariaId}
      />

      {option.dateRange && (
        <DateRangePicker
          locale="fi"
          default={
            isSelected && range
              ? new FiniteDateRange(range.start, range.end)
              : null
          }
          onChange={(range) =>
            setRange(range ? { start: range.start, end: range.end } : undefined)
          }
          data-qa={`radio-group-date-question-option-${option.key}-range`}
          errorTexts={i18n.validationErrors}
          hideErrorsBeforeTouched
          labels={i18n.common.datePicker}
          aria-labelledby={ariaId}
        />
      )}
    </OptionContainer>
  )
})
