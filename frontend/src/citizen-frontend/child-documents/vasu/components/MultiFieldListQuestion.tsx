// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { MultiFieldListQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiFieldListQuestion> {
  translations: VasuTranslations
}

export default React.memo(function MultiFieldListQuestion({
  question,
  questionNumber,
  translations
}: Props) {
  return (
    <div data-qa="multi-field-list-question">
      <Label>
        {questionNumber} {question.name}
      </Label>

      <ValueOrNoRecord
        text={joinNonEmptyValues(question.value)}
        translations={translations}
      />
    </div>
  )
})

function joinNonEmptyValues(values: string[][]) {
  return values
    .filter((fields) =>
      fields.map((value) => value.trim()).some((value) => value)
    )
    .map((fields) => fields.map((value) => value.trim()).join(' '))
    .join(',\n')
}
