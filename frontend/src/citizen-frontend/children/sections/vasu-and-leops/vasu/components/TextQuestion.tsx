// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { TextQuestion } from 'lib-common/api-types/vasu'
import { Label } from 'lib-components/typography'
import type { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

interface TextQuestionQuestionProps extends QuestionProps<TextQuestion> {
  translations: VasuTranslations
}

export function TextQuestion({
  question: { name, value },
  questionNumber,
  translations
}: TextQuestionQuestionProps) {
  return (
    <div>
      <Label>
        {questionNumber} {name}
      </Label>
      <ValueOrNoRecord text={value} translations={translations} />
    </div>
  )
}
