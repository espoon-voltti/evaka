// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { MultiSelectQuestion, QuestionOption } from 'lib-common/api-types/vasu'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'
import QuestionInfo from '../QuestionInfo'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  selectedValues: string[]
  onChange?: (option: QuestionOption, value: boolean | string) => void
  translations: VasuTranslations
}

export function MultiSelectQuestion({
  onChange,
  question: { name, options, info, textValue },
  questionNumber,
  selectedValues,
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
          <Gap size="m" />
          <FixedSpaceColumn>
            {options.map((option) => {
              return (
                <React.Fragment key={option.key}>
                  <Checkbox
                    key={option.key}
                    checked={selectedValues.includes(option.key)}
                    label={option.name}
                    onChange={(checked) => onChange(option, checked)}
                    data-qa={`multi-select-question-option-${option.key}`}
                  />
                  {option.textAnswer && textValue && (
                    <InputField
                      value={textValue[option.key] || ''}
                      onChange={(text) => onChange(option, text)}
                      placeholder={option.name}
                      data-qa={`multi-select-question-option-text-input-${option.key}`}
                    />
                  )}
                </React.Fragment>
              )
            })}
          </FixedSpaceColumn>
        </>
      ) : (
        <ValueOrNoRecord
          text={options
            .filter((option) => selectedValues.includes(option.key))
            .map((o) =>
              textValue && textValue[o.key]
                ? `${o.name} (${textValue[o.key]})`
                : o.name
            )
            .join(', ')}
          translations={translations}
          dataQa={`value-or-no-record-${questionNumber}`}
        />
      )}
    </div>
  )
}
