// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import TextArea from '../../../../lib-components/atoms/form/TextArea'
import { Label } from '../../../../lib-components/typography'
import { TextQuestion } from '../vasu-content'
import { OrNoRecord } from './OrNoRecord'
import { QuestionProps } from './question-props'

interface TextQuestionQuestionProps extends QuestionProps<TextQuestion> {
  onChange?: (value: string) => void
}

export function TextQuestion({
  onChange,
  question: { name, value },
  questionNumber
}: TextQuestionQuestionProps) {
  return (
    <>
      <Label>
        {questionNumber} {name}
      </Label>
      {onChange ? (
        <TextArea value={value} onChange={(e) => onChange(e.target.value)} />
      ) : (
        <OrNoRecord>{value}</OrNoRecord>
      )}
    </>
  )
}
