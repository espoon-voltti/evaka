// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { MultiSelectQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiSelectQuestion> {
  translations: VasuTranslations
}

export function MultiSelectQuestion({
  question: { name, value, options, textValue, dateValue, dateRangeValue },
  questionNumber,
  translations
}: Props) {
  return (
    <div>
      <Label>
        {questionNumber} {name}
      </Label>

      <ValueOrNoRecord
        text={options
          .filter((option) => value.includes(option.key))
          .map((o) => {
            const date = dateValue?.[o.key]
            const dateStr = date ? ` ${date.format()}` : ''
            const dateRange = dateRangeValue?.[o.key]
            const dateRangeStr = dateRange
              ? ` ${dateRange.start.format()}–${dateRange.end.format()}`
              : ''
            const subTextStr = o.subText ? `\n${o.subText}` : ''
            const name = `${o.name}${dateStr}${dateRangeStr}${subTextStr}`
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
