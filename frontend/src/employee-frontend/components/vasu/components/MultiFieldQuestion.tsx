// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { MultiFieldQuestion } from 'lib-common/api-types/vasu'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { VasuTranslations } from 'lib-customizations/employee'
import QuestionInfo from '../QuestionInfo'
import { ValueOrNoRecord } from './ValueOrNoRecord'
import { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiFieldQuestion> {
  onChange?: (index: number, value: string) => void
  translations: VasuTranslations
}

export default React.memo(function MultiFieldQuestion({
  onChange,
  question,
  questionNumber,
  translations
}: Props) {
  return (
    <div data-qa="multi-field-question">
      <QuestionInfo info={question.info}>
        <Label>
          {questionNumber} {question.name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        <>
          <Gap size="m" />
          <FixedSpaceRow>
            {question.keys.map((key, index) => (
              <FixedSpaceColumn key={key.name} spacing="xxs">
                <Label>{key.name}</Label>
                <InputField
                  value={question.value[index]}
                  onChange={(v) => onChange(index, v)}
                  width="m"
                />
              </FixedSpaceColumn>
            ))}
          </FixedSpaceRow>
        </>
      ) : (
        <ValueOrNoRecord
          text={question.value
            .map((v) => v.trim())
            .join(' ')
            .trim()}
          translations={translations}
        />
      )}
    </div>
  )
})
