// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { RadioGroupQuestion } from 'lib-common/api-types/vasu'
import { OfficialLanguage } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Bold, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import RadioGroupQuestionOption from './RadioGroupQuestionOption'
import { ValueOrNoRecord } from './ValueOrNoRecord'

export interface RadioGroupSelectedValue {
  key: string
  range: {
    start: LocalDate
    end: LocalDate
  } | null
}

interface Props {
  questionNumber: string
  question: RadioGroupQuestion
  onChange?: (selected: RadioGroupSelectedValue | null) => void
  lang: OfficialLanguage
  translations: VasuTranslations
}

export function RadioGroupQuestion({
  onChange,
  question: { name, options, info, value, dateRange },
  questionNumber,
  lang,
  translations
}: Props) {
  const selectedValue = value !== null ? { key: value, range: dateRange } : null
  const selectedOption = options.find(
    (option) => option.key === selectedValue?.key
  )
  const selectedDateRange = selectedValue?.range
    ? ` ${selectedValue.range.start.format()}–${selectedValue.range.end.format()}`
    : ''

  const [pendingSelected, setPendingSelected] = useState(
    selectedValue?.key ?? ''
  )

  return (
    <div>
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>

      {onChange ? (
        <>
          <Gap size="m" />
          <FixedSpaceColumn>
            <RadioGroupQuestionOption
              option={{ key: '', name: translations.noRecord }}
              onChange={() => {
                onChange(null)
                setPendingSelected('')
              }}
              selectedValue={selectedValue}
              isSelected={pendingSelected === ''}
              onSelected={() => setPendingSelected('')}
              lang={lang}
            />
            {options.map((option) =>
              option.isIntervention ? (
                <QuestionInfo key={option.key} info={option.info ?? null}>
                  <Bold>{option.name}</Bold>
                </QuestionInfo>
              ) : (
                <RadioGroupQuestionOption
                  key={option.key}
                  option={option}
                  onChange={(value) => {
                    onChange(value)
                    setPendingSelected(option.key)
                  }}
                  selectedValue={selectedValue}
                  isSelected={option.key === pendingSelected}
                  onSelected={() => setPendingSelected(option.key)}
                  lang={lang}
                />
              )
            )}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={selectedOption && `${selectedOption.name}${selectedDateRange}`}
          translations={translations}
        />
      )}
    </div>
  )
}
