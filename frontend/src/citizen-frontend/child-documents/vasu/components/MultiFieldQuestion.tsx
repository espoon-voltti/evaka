// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { MultiFieldQuestion } from 'lib-common/api-types/vasu'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { VasuTranslations } from 'lib-customizations/employee'

import { ValueOrNoRecord } from './ValueOrNoRecord'
import type { QuestionProps } from './question-props'

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

      {question.separateRows ? (
        <>
          <Gap size="s" />
          <FixedSpaceColumn spacing="s">
            {question.keys.map((key, index) => (
              <FixedSpaceColumn key={key.name} spacing="xxs">
                <Label>{key.name}</Label>
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
