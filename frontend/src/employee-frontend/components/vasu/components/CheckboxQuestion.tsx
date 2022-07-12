// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { CheckboxQuestion } from 'lib-common/api-types/vasu'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label } from 'lib-components/typography'
import { VasuTranslations } from 'lib-customizations/employee'

import { useTranslation } from '../../../state/i18n'
import QuestionInfo from '../QuestionInfo'

import { ReadOnlyValue } from './ReadOnlyValue'
import { QuestionProps } from './question-props'

type Props = QuestionProps<CheckboxQuestion> & {
  onChange?: (checked: boolean) => void
  translations: VasuTranslations
}

export function CheckboxQuestion(props: Props) {
  const { i18n } = useTranslation()
  const checkboxLabel =
    props.question.label || props.question.notNumbered
      ? props.question.name
      : `${props.questionNumber} ${props.question.name}`
  const numberedLabel =
    props.question.label && `${props.questionNumber} ${props.question.label}`

  if (props.onChange && numberedLabel) {
    return (
      <FixedSpaceColumn spacing="xs">
        <QuestionInfo info={props.question.info}>
          <Label>{numberedLabel}</Label>
        </QuestionInfo>
        <Checkbox
          checked={props.question.value}
          label={checkboxLabel}
          onChange={props.onChange}
          data-qa="checkbox-question"
        />
      </FixedSpaceColumn>
    )
  }

  return (
    <QuestionInfo info={props.question.info}>
      {props.onChange ? (
        <Checkbox
          checked={props.question.value}
          label={checkboxLabel}
          onChange={props.onChange}
          data-qa="checkbox-question"
        />
      ) : (
        <ReadOnlyValue
          label={numberedLabel ?? checkboxLabel}
          value={
            props.question.value
              ? props.question.label
                ? props.question.name
                : i18n.common.yes
              : i18n.common.no
          }
          translations={props.translations}
        />
      )}
    </QuestionInfo>
  )
}
