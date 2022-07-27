// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { MultiSelectQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  selectedValues: string[]
  translations: VasuTranslations
}

export function MultiSelectQuestion({
  question: { name, options, textValue, dateValue },
  questionNumber,
  selectedValues,
  translations
}: Props) {
  return (
    <div>
      <Label>
        {questionNumber} {name}
      </Label>

      <ValueOrNoRecord
        text={options
          .filter((option) => selectedValues.includes(option.key))
          .map((o) => {
            const date = dateValue?.[o.key]
            const name = `${o.name}${date ? ` ${date.format()}` : ''}${
              o.subText ? `\n${o.subText}` : ''
            }`

            return textValue && textValue[o.key]
              ? `${name}: ${textValue[o.key]}`
              : name
          })
          .join(', ')}
        translations={translations}
        dataQa={`value-or-no-record-${questionNumber}`}
      />
    </div>
  )
}
