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
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { useTranslation } from '../../../state/i18n'

interface TextQuestionQuestionProps extends QuestionProps<TextQuestion> {
  onChange?: (value: string) => void
}

export function TextQuestion({
  onChange,
  question: { name, value, multiline, info },
  questionNumber
}: TextQuestionQuestionProps) {
  const { i18n } = useTranslation()

  const getEditorOrStaticText = () => {
    if (!onChange) {
      return <ValueOrNoRecord text={value} />
    }
    return multiline ? (
      <TextArea value={value} onChange={(e) => onChange(e.target.value)} />
    ) : (
      <div>
        <InputField value={value} onChange={onChange} width="L" />
      </div>
    )
  }

  return (
    <>
      <ExpandingInfo
        info={info.length ? <div>{info}</div> : null}
        ariaLabel={i18n.common.openExpandingInfo}
      >
        <Label>
          {questionNumber} {name}
        </Label>
      </ExpandingInfo>
      {getEditorOrStaticText()}
    </>
  )
}
