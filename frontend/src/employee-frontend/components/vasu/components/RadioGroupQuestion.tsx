// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { QuestionOption, RadioGroupQuestion } from 'lib-common/api-types/vasu'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import QuestionInfo from '../QuestionInfo'
import { VasuTranslations } from 'lib-customizations/employee'

interface Props {
  questionNumber: string
  question: RadioGroupQuestion
  selectedValue: string | null
  onChange?: (selected: QuestionOption) => void
  translations: VasuTranslations
}

export function RadioGroupQuestion({
  onChange,
  question: { name, options, info },
  questionNumber,
  selectedValue,
  translations
}: Props) {
  return (
    <div>
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>

      {onChange ? (
        <>
          <Gap size={'m'} />
          <FixedSpaceColumn>
            {options.map((option) => (
              <Radio
                key={option.key}
                checked={selectedValue === option.key}
                label={option.name}
                onChange={() => onChange(option)}
              />
            ))}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options.find((option) => option.key === selectedValue)?.name}
          translations={translations}
        />
      )}
    </div>
  )
}
