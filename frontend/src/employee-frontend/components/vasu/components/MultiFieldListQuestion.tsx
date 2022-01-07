// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect } from 'react'
import { MultiFieldListQuestion } from 'lib-common/api-types/vasu'
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

interface Props extends QuestionProps<MultiFieldListQuestion> {
  onChange?: (questionValue: MultiFieldListQuestion['value']) => void
  translations: VasuTranslations
}

export default React.memo(function MultiFieldListQuestion({
  onChange,
  question,
  questionNumber,
  translations
}: Props) {
  useAutoExpandList(question.value, onChange, question.keys.length)

  const onFieldChange = (
    [questionIndex, keyIndex]: [number, number],
    value: string
  ) =>
    onChange
      ? onChange([
          ...question.value.slice(0, questionIndex),
          [
            ...question.value[questionIndex].slice(0, keyIndex),
            value,
            ...question.value[questionIndex].slice(keyIndex + 1)
          ],
          ...question.value.slice(questionIndex + 1)
        ])
      : undefined

  return (
    <div data-qa="multi-field-list-question">
      <QuestionInfo info={question.info}>
        <Label>
          {questionNumber} {question.name}
        </Label>
      </QuestionInfo>
      {onChange ? (
        question.value.map((element, questionIndex) => (
          <Fragment key={questionIndex}>
            <Gap size="m" />
            <FixedSpaceRow data-qa="field-row">
              {question.keys.map((key, keyIndex) => (
                <FixedSpaceColumn key={key.name} spacing="xxs">
                  <Label>{key.name}</Label>
                  <InputField
                    value={element[keyIndex]}
                    onChange={(v) =>
                      onFieldChange([questionIndex, keyIndex], v)
                    }
                    width="m"
                  />
                </FixedSpaceColumn>
              ))}
            </FixedSpaceRow>
          </Fragment>
        ))
      ) : (
        <ValueOrNoRecord
          text={joinNonEmptyValues(question.value)}
          translations={translations}
        />
      )}
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

export function useAutoExpandList(
  records: string[][],
  onChange: Props['onChange'],
  keysLength: number
) {
  useEffect(() => {
    if (!records || !onChange) {
      return
    }

    const allButLast = records.slice(0, records.length - 1)
    const last = records[records.length - 1]

    const emptyRecord = new Array(keysLength).fill('')
    if (!last) {
      onChange([emptyRecord])
      return
    }

    if (allButLast.some(contentsAreEmpty) || !contentsAreEmpty(last)) {
      const filtered = allButLast.filter((r) => !contentsAreEmpty(r))
      const withEmptyLastValue = contentsAreEmpty(last)
        ? [...filtered, last]
        : [...filtered, last, emptyRecord]

      onChange(withEmptyLastValue)
    }
  }, [records, onChange, keysLength])
}

const contentsAreEmpty = (contents: string[]) => !contents.join('').trim()
