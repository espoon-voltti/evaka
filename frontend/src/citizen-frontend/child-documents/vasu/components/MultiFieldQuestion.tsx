// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { MultiFieldQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiFieldQuestion> {
  translations: VasuTranslations
}

export default React.memo(function MultiFieldQuestion({
  question,
  questionNumber,
  translations
}: Props) {
  return (
    <div data-qa="multi-field-question">
      <Label>
        {questionNumber} {question.name}
      </Label>

      <ValueOrNoRecord
        text={question.value
          .map((v) => v.trim())
          .join(' ')
          .trim()}
        translations={translations}
      />
    </div>
  )
})
