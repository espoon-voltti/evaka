// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Label } from 'lib-components/typography'
import { TextQuestion } from '../vasu-content'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'
import QuestionInfo from '../QuestionInfo'
import { VasuTranslations } from 'lib-customizations/employee'

interface TextQuestionQuestionProps extends QuestionProps<TextQuestion> {
  onChange?: (value: string) => void
  translations: VasuTranslations
}

export function TextQuestion({
  onChange,
  question: { name, value, multiline, info },
  questionNumber,
  translations
}: TextQuestionQuestionProps) {
  const getEditorOrStaticText = () => {
    if (!onChange) {
      return <ValueOrNoRecord text={value} translations={translations} />
    }
    return multiline ? (
      <TextArea value={value} onChange={onChange} />
    ) : (
      <div>
        <InputField value={value} onChange={onChange} width="L" />
      </div>
    )
  }

  return (
    <>
      <QuestionInfo info={info}>
        <Label>
          {questionNumber} {name}
        </Label>
      </QuestionInfo>
      {getEditorOrStaticText()}
    </>
  )
}
