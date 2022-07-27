// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { MultiFieldQuestion } from 'lib-common/api-types/vasu'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import type { VasuTranslations } from 'lib-customizations/employee'

import QuestionInfo from '../QuestionInfo'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

interface Props extends QuestionProps<MultiFieldQuestion> {
  onChange?: (index: number, value: string) => void
  translations: VasuTranslations
}

export const FixedSpaceRowOrColumns = styled.div<{ columns?: boolean }>`
  display: flex;
  flex-direction: ${(p) => (p.columns ? 'column' : 'row')};
  gap: ${defaultMargins.s};
`

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
          <FixedSpaceRowOrColumns columns={question.separateRows}>
            {question.keys.map((key, index) => (
              <FixedSpaceColumn key={key.name} spacing="xxs">
                <QuestionInfo info={key.info ?? null}>
                  <Label>{key.name}</Label>
                </QuestionInfo>
                {question.separateRows ? (
                  <TextArea
                    value={question.value[index]}
                    onChange={(v) => onChange(index, v)}
                    data-qa="text-question-input"
                  />
                ) : (
                  <InputField
                    value={question.value[index]}
                    onChange={(v) => onChange(index, v)}
                    width="m"
                    data-qa="text-question-input"
                  />
                )}
              </FixedSpaceColumn>
            ))}
          </FixedSpaceRowOrColumns>
        </>
      ) : question.separateRows ? (
        <>
          <Gap size="s" />
          <FixedSpaceColumn spacing="s">
            {question.keys.map((key, index) => (
              <FixedSpaceColumn key={key.name} spacing="xxs">
                <QuestionInfo info={key.info ?? null}>
                  <Label>{key.name}</Label>
                </QuestionInfo>
                <ValueOrNoRecord
                  text={question.value[index]}
                  translations={translations}
                />
              </FixedSpaceColumn>
            ))}
          </FixedSpaceColumn>
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
